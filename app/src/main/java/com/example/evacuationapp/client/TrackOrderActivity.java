package com.example.evacuationapp.client;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.evacuationapp.R;
import com.example.evacuationapp.models.Order;
import com.example.evacuationapp.network.RetrofitClient;
import com.example.evacuationapp.utils.PreferenceManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrackOrderActivity extends AppCompatActivity {

    private TextView tvOrderId, tvStatus, tvPickup, tvDropoff, tvPrice;
    private Button btnCancelOrder, btnBack;
    private long orderId;
    private long clientId;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable statusUpdater;
    private Order currentOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_order);

        // Получаем ID заказа из Intent
        orderId = getIntent().getLongExtra("orderId", 0);
        if (orderId == 0) {
            Order tempOrder = (Order) getIntent().getSerializableExtra("order");
            if (tempOrder != null) {
                orderId = tempOrder.getOrderId();
            }
        }
        clientId = new PreferenceManager(this).getUserId();

        tvOrderId = findViewById(R.id.tvOrderId);
        tvStatus = findViewById(R.id.tvStatus);
        tvPickup = findViewById(R.id.tvPickupAddress);
        tvDropoff = findViewById(R.id.tvDropoffAddress);
        tvPrice = findViewById(R.id.tvPrice);
        btnCancelOrder = findViewById(R.id.btnCancelOrder);
        btnBack = findViewById(R.id.btnBack);

        btnCancelOrder.setOnClickListener(v -> cancelOrder());
        btnBack.setOnClickListener(v -> finish());

        // Первая загрузка
        loadOrder();

        // Запускаем периодическое обновление (каждые 5 секунд)
        startPolling();
    }

    private void loadOrder() {
        Call<Order> call = RetrofitClient.getApiService().getOrder(orderId);
        call.enqueue(new Callback<Order>() {
            @Override
            public void onResponse(Call<Order> call, Response<Order> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentOrder = response.body();
                    updateUI();
                } else {
                    Toast.makeText(TrackOrderActivity.this, "Не удалось загрузить заказ", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Order> call, Throwable t) {
                Toast.makeText(TrackOrderActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        if (currentOrder == null) return;
        tvOrderId.setText("Заказ №" + currentOrder.getOrderId());
        tvPickup.setText("Откуда: " + currentOrder.getPickupAddress());
        tvDropoff.setText("Куда: " + currentOrder.getDropoffAddress());
        tvPrice.setText("Стоимость: " + (int) currentOrder.getPrice() + " ₽");
        tvStatus.setText("Статус: " + currentOrder.getStatusText());

        // Если заказ завершён или отменён, блокируем кнопку отмены
        if ("completed".equals(currentOrder.getStatus()) || "cancelled".equals(currentOrder.getStatus())) {
            btnCancelOrder.setEnabled(false);
        } else {
            btnCancelOrder.setEnabled(true);
        }
    }

    private void startPolling() {
        statusUpdater = new Runnable() {
            @Override
            public void run() {
                loadOrder(); // повторно загружаем заказ
                handler.postDelayed(this, 5000); // каждые 5 секунд
            }
        };
        handler.post(statusUpdater);
    }

    private void cancelOrder() {
        if (currentOrder == null) return;
        // TODO: вызвать API отмены заказа (PUT /api/orders/{orderId}/status со статусом "cancelled")
        Toast.makeText(this, "Отмена заказа пока не реализована", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(statusUpdater);
    }
}