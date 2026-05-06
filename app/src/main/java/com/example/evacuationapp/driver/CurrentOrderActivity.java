package com.example.evacuationapp.driver;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.evacuationapp.R;
import com.example.evacuationapp.models.Order;
import com.example.evacuationapp.network.RetrofitClient;
import com.example.evacuationapp.utils.PreferenceManager;
import com.example.evacuationapp.utils.LocationTracker;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CurrentOrderActivity extends AppCompatActivity {

    private TextView tvOrderId, tvPickup, tvDropoff, tvPrice, tvStatus;
    private Button btnStart, btnComplete, btnBack;
    private MapView mapView;
    private Marker clientMarker;
    private Order currentOrder;
    private long driverId;
    private LocationTracker locationTracker;
    private Handler handler = new Handler();
    private Runnable sendLocationRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Инициализация OSMDroid
        android.content.SharedPreferences osmdroidPref = androidx.preference.PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Configuration.getInstance().load(getApplicationContext(), osmdroidPref);
        Configuration.getInstance().setOsmdroidBasePath(new File(getCacheDir().getAbsolutePath(), "osmdroid"));
        Configuration.getInstance().setOsmdroidTileCache(new File(getCacheDir().getAbsolutePath(), "osmdroid/tiles"));

        setContentView(R.layout.activity_current_order);

        tvOrderId = findViewById(R.id.tvOrderId);
        tvPickup = findViewById(R.id.tvPickup);
        tvDropoff = findViewById(R.id.tvDropoff);
        tvPrice = findViewById(R.id.tvPrice);
        tvStatus = findViewById(R.id.tvStatus);
        btnStart = findViewById(R.id.btnStart);
        btnComplete = findViewById(R.id.btnComplete);
        btnBack = findViewById(R.id.btnBack);
        mapView = findViewById(R.id.mapView);

        driverId = new PreferenceManager(this).getUserId();
        currentOrder = (Order) getIntent().getSerializableExtra("order");

        if (currentOrder == null) {
            Toast.makeText(this, "Нет данных о заказе", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Настройка карты
        if (mapView != null) {
            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setMultiTouchControls(true);
            mapView.setBuiltInZoomControls(true);
            mapView.getController().setZoom(15.0);
        }

        displayOrder();
        showClientOnMap();

        btnStart.setOnClickListener(v -> updateStatus("in_progress"));
        btnComplete.setOnClickListener(v -> updateStatus("completed"));
        btnBack.setOnClickListener(v -> finish());

        // Запускаем отправку геолокации
        startSendingLocation();
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
            stopSendingLocation();
        }
    }

    private void showClientOnMap() {
        if (mapView == null) {
            Toast.makeText(this, "Карта не инициализирована", Toast.LENGTH_SHORT).show();
            return;
        }

        mapView.getOverlays().clear();

        // Маркер места подачи (клиент)
        GeoPoint pickupPoint = getPointFromAddress(currentOrder.getPickupAddress());
        if (pickupPoint != null) {
            Marker pickupMarker = new Marker(mapView);
            pickupMarker.setIcon(getResources().getDrawable(android.R.drawable.ic_menu_mylocation));
            pickupMarker.setTitle("📍 Место подачи");
            pickupMarker.setSnippet(currentOrder.getPickupAddress());
            pickupMarker.setPosition(pickupPoint);
            mapView.getOverlays().add(pickupMarker);
        }

        // Маркер места назначения
        GeoPoint dropoffPoint = getPointFromAddress(currentOrder.getDropoffAddress());
        if (dropoffPoint != null) {
            Marker dropoffMarker = new Marker(mapView);
            dropoffMarker.setIcon(getResources().getDrawable(android.R.drawable.ic_menu_directions));
            dropoffMarker.setTitle("🏁 Место назначения");
            dropoffMarker.setSnippet(currentOrder.getDropoffAddress());
            dropoffMarker.setPosition(dropoffPoint);
            mapView.getOverlays().add(dropoffMarker);
        }

        // Центрируем карту на месте подачи (или на месте назначения, если подача не найдена)
        if (pickupPoint != null) {
            mapView.getController().setCenter(pickupPoint);
            mapView.getController().setZoom(14.0);
            Toast.makeText(this, "Точки отмечены на карте", Toast.LENGTH_SHORT).show();
        } else if (dropoffPoint != null) {
            mapView.getController().setCenter(dropoffPoint);
            mapView.getController().setZoom(14.0);
            Toast.makeText(this, "Точка назначения отмечена на карте", Toast.LENGTH_SHORT).show();
        } else {
            GeoPoint defaultPoint = new GeoPoint(55.7558, 37.6173);
            mapView.getController().setCenter(defaultPoint);
            Toast.makeText(this, "Не удалось определить координаты", Toast.LENGTH_SHORT).show();
        }
    }

    private GeoPoint getPointFromAddress(String address) {
        if (address == null || address.isEmpty()) {
            return null;
        }
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address location = addresses.get(0);
                double lat = location.getLatitude();
                double lon = location.getLongitude();
                if (lat != 0.0 && lon != 0.0) {
                    return new GeoPoint(lat, lon);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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
                        finish();
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

    private void startSendingLocation() {
        locationTracker = new LocationTracker(this);
        locationTracker.startLocationUpdates(this, location -> {
            if (location != null && currentOrder != null && !"completed".equals(currentOrder.getStatus())) {
                sendLocationToServer(location.getLatitude(), location.getLongitude());
            }
        });

        sendLocationRunnable = new Runnable() {
            @Override
            public void run() {
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
    }

    private void sendLocationToServer(double lat, double lon) {
        com.example.evacuationapp.models.DriverLocation loc = new com.example.evacuationapp.models.DriverLocation();
        loc.setDriverId(driverId);
        loc.setLatitude(lat);
        loc.setLongitude(lon);
        Call<Void> call = RetrofitClient.getApiService().updateLocation(loc);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {}
            @Override
            public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSendingLocation();
    }
}