package com.example.evacuationapp.driver;

import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.evacuationapp.R;
import com.example.evacuationapp.models.DriverLocation;
import com.example.evacuationapp.models.Order;
import com.example.evacuationapp.network.RetrofitClient;
import com.example.evacuationapp.utils.LocationTracker;
import com.example.evacuationapp.utils.PreferenceManager;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CurrentOrderActivity extends AppCompatActivity {

    private TextView tvOrderId, tvPickup, tvDropoff, tvPrice, tvStatus;
    private Button btnStart, btnComplete, btnBack;
    private Order currentOrder;
    private long driverId;

    // Для геолокации
    private LocationTracker locationTracker;
    private Handler handler = new Handler();
    private Runnable sendLocationRunnable;
    private boolean isSendingLocation = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_order);

        tvOrderId = findViewById(R.id.tvOrderId);
        tvPickup = findViewById(R.id.tvPickup);
        tvDropoff = findViewById(R.id.tvDropoff);
        tvPrice = findViewById(R.id.tvPrice);
        tvStatus = findViewById(R.id.tvStatus);
        btnStart = findViewById(R.id.btnStart);
        btnComplete = findViewById(R.id.btnComplete);
        btnBack = findViewById(R.id.btnBack);

        driverId = new PreferenceManager(this).getUserId();
        currentOrder = (Order) getIntent().getSerializableExtra("order");

        if (currentOrder == null) {
            Toast.makeText(this, "Нет данных о заказе", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        displayOrder();

        btnStart.setOnClickListener(v -> updateStatus("in_progress"));
        btnComplete.setOnClickListener(v -> updateStatus("completed"));
        btnBack.setOnClickListener(v -> finish());

        // Запускаем отправку геолокации, если заказ ещё не завершён
        if (!"completed".equals(currentOrder.getStatus())) {
            startSendingLocation();
        }
    }

    private void displayOrder() {
        tvOrderId.setText("Заказ №" + currentOrder.getOrderId());
        tvPickup.setText("Откуда: " + currentOrder.getPickupAddress());
        tvDropoff.setText("Куда: " + currentOrder.getDropoffAddress());
        tvPrice.setText("Стоимость: " + (int) currentOrder.getPrice() + " ₽");
        tvStatus.setText("Статус: " + currentOrder.getStatusText());

        if ("accepted".equals(currentOrder.getStatus())) {
            btnStart.setEnabled(true);
            btnComplete.setEnabled(false);
        } else if ("in_progress".equals(currentOrder.getStatus())) {
            btnStart.setEnabled(false);
            btnComplete.setEnabled(true);
        } else if ("completed".equals(currentOrder.getStatus())) {
            btnStart.setEnabled(false);
            btnComplete.setEnabled(false);
            stopSendingLocation(); // Останавливаем отправку, если заказ завершён
        }
    }

    private void updateStatus(String newStatus) {
        Map<String, String> body = new HashMap<>();
        body.put("status", newStatus);

        Call<Order> call = RetrofitClient.getApiService().updateOrderStatus(currentOrder.getOrderId(), body);
        call.enqueue(new Callback<Order>() {
            @Override
            public void onResponse(Call<Order> call, Response<Order> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentOrder = response.body();
                    displayOrder();
                    Toast.makeText(CurrentOrderActivity.this, "Статус обновлён", Toast.LENGTH_SHORT).show();
                    if ("completed".equals(currentOrder.getStatus())) {
                        stopSendingLocation();
                        finish(); // Закрываем экран после завершения заказа
                    }
                } else {
                    Toast.makeText(CurrentOrderActivity.this, "Ошибка обновления статуса", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Order> call, Throwable t) {
                Toast.makeText(CurrentOrderActivity.this, "Нет связи с сервером", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==================== ГЕОЛОКАЦИЯ ====================
    private void startSendingLocation() {
        if (isSendingLocation) return;
        isSendingLocation = true;

        locationTracker = new LocationTracker(this);
        locationTracker.startLocationUpdates(this, location -> {
            if (location != null && currentOrder != null && !"completed".equals(currentOrder.getStatus())) {
                sendLocationToServer(location.getLatitude(), location.getLongitude());
            }
        });

        // Дополнительный периодический запуск для надёжности (каждые 5 секунд)
        sendLocationRunnable = new Runnable() {
            @Override
            public void run() {
                // Координаты уже отправляются в колбэке, но можно оставить как резерв
                handler.postDelayed(this, 5000);
            }
        };
        handler.post(sendLocationRunnable);
    }

    private void stopSendingLocation() {
        if (locationTracker != null) {
            locationTracker.stopLocationUpdates();
        }
        if (handler != null && sendLocationRunnable != null) {
            handler.removeCallbacks(sendLocationRunnable);
        }
        isSendingLocation = false;
    }

    private void sendLocationToServer(double lat, double lon) {
        DriverLocation loc = new DriverLocation();
        loc.setDriverId(driverId);
        loc.setLatitude(lat);
        loc.setLongitude(lon);
        Call<Void> call = RetrofitClient.getApiService().updateLocation(loc);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                // Успешно, можно ничего не делать
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Ошибка сети – игнорируем, попробуем в следующий раз
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSendingLocation();
    }
}