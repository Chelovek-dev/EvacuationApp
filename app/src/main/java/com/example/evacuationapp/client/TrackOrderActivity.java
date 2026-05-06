package com.example.evacuationapp.client;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.evacuationapp.R;
import com.example.evacuationapp.models.DriverLocation;
import com.example.evacuationapp.models.Order;
import com.example.evacuationapp.network.RetrofitClient;
import com.example.evacuationapp.utils.PreferenceManager;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrackOrderActivity extends AppCompatActivity {

    private TextView tvOrderId, tvStatus, tvPickup, tvDropoff, tvPrice;
    private Button btnCancelOrder, btnBack;
    private MapView mapView;
    private Marker driverMarker;
    private long orderId;
    private long clientId;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable statusUpdater;
    private Runnable locationUpdater;
    private Order currentOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().setOsmdroidBasePath(new File(getCacheDir().getAbsolutePath(), "osmdroid"));
        Configuration.getInstance().setOsmdroidTileCache(new File(getCacheDir().getAbsolutePath(), "osmdroid/tiles"));
        Configuration.getInstance().setTileFileSystemCacheMaxBytes(500 * 1024 * 1024); // 500 MB кеш



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
        mapView = findViewById(R.id.mapView);

        // Настройка карты
        if (mapView != null) {
            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setMultiTouchControls(true);
            mapView.setBuiltInZoomControls(true);
            mapView.getController().setZoom(15.0);
        }

        btnCancelOrder.setOnClickListener(v -> cancelOrder());
        btnBack.setOnClickListener(v -> finish());

        loadOrder();
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

                    if (currentOrder.getDriverId() != 0) {
                        startLocationPolling();
                    }
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

        // Отменить можно только в статусе "waiting"
        if ("waiting".equals(currentOrder.getStatus())) {
            btnCancelOrder.setEnabled(true);
        } else {
            btnCancelOrder.setEnabled(false);
        }
    }

    private void startPolling() {
        statusUpdater = new Runnable() {
            @Override
            public void run() {
                loadOrder();
                handler.postDelayed(this, 5000);
            }
        };
        handler.post(statusUpdater);
    }

    private void startLocationPolling() {
        if (locationUpdater != null) {
            handler.removeCallbacks(locationUpdater);
        }
        locationUpdater = new Runnable() {
            @Override
            public void run() {
                if (currentOrder != null && currentOrder.getDriverId() != 0) {
                    fetchDriverLocation(currentOrder.getDriverId());
                }
                handler.postDelayed(this, 5000);
            }
        };
        handler.post(locationUpdater);
    }

    private void fetchDriverLocation(long driverId) {
        Call<DriverLocation> call = RetrofitClient.getApiService().getDriverLocation(driverId);
        call.enqueue(new Callback<DriverLocation>() {
            @Override
            public void onResponse(Call<DriverLocation> call, Response<DriverLocation> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DriverLocation loc = response.body();
                    updateDriverOnMap(loc.getLatitude(), loc.getLongitude());
                }
            }

            @Override
            public void onFailure(Call<DriverLocation> call, Throwable t) {
                // Игнорируем ошибки сети при опросе
            }
        });
    }

    private void updateDriverOnMap(double lat, double lon) {
        if (mapView == null) return;
        GeoPoint driverPoint = new GeoPoint(lat, lon);
        if (driverMarker == null) {
            driverMarker = new Marker(mapView);
            driverMarker.setIcon(getResources().getDrawable(android.R.drawable.ic_menu_directions));
            driverMarker.setTitle("Водитель");
            mapView.getOverlays().add(driverMarker);
        }
        driverMarker.setPosition(driverPoint);
        mapView.getController().setCenter(driverPoint);
    }

    private void cancelOrder() {
        if (currentOrder == null) return;

        // Проверяем, можно ли отменить (только статус waiting)
        if (!"waiting".equals(currentOrder.getStatus())) {
            Toast.makeText(this, "Заказ уже нельзя отменить", Toast.LENGTH_SHORT).show();
            return;
        }

        // Подтверждение отмены
        new AlertDialog.Builder(this)
                .setTitle("Отмена заказа")
                .setMessage("Вы уверены, что хотите отменить заказ?")
                .setPositiveButton("Да", (dialog, which) -> {
                    Call<Order> call = RetrofitClient.getApiService().cancelOrder(orderId);
                    call.enqueue(new Callback<Order>() {
                        @Override
                        public void onResponse(Call<Order> call, Response<Order> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                currentOrder = response.body();
                                updateUI();
                                Toast.makeText(TrackOrderActivity.this, "Заказ отменён", Toast.LENGTH_SHORT).show();
                                // Через 2 секунды закрываем экран
                                new Handler().postDelayed(() -> finish(), 2000);
                            } else {
                                String error = "Ошибка отмены";
                                try {
                                    if (response.errorBody() != null) {
                                        error = response.errorBody().string();
                                    }
                                } catch (Exception e) {}
                                Toast.makeText(TrackOrderActivity.this, error, Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Order> call, Throwable t) {
                            Toast.makeText(TrackOrderActivity.this, "Нет связи с сервером", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Нет", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            if (statusUpdater != null) handler.removeCallbacks(statusUpdater);
            if (locationUpdater != null) handler.removeCallbacks(locationUpdater);
        }
    }
}