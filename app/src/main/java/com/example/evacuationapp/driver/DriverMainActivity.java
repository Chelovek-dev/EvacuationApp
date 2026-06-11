package com.example.evacuationapp.driver;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.evacuationapp.R;
import com.example.evacuationapp.auth.LoginActivity;
import com.example.evacuationapp.models.DriverLocation;
import com.example.evacuationapp.network.RetrofitClient;
import com.example.evacuationapp.utils.LocationTracker;
import com.example.evacuationapp.utils.PreferenceManager;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverMainActivity extends AppCompatActivity {

    private TextView tvWelcome, tvStatus;
    private Switch switchOnline;
    private Button btnAvailableOrders, btnHistory, btnLogout;
    private long userId;
    private boolean isUpdating = false;
    private LocationTracker locationTracker;
    private boolean isSendingLocation = false;
    private Handler handler = new Handler();
    private Runnable sendLocationRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_main);

        userId = getIntent().getLongExtra("userId", 0);
        if (userId == 0) {
            userId = new PreferenceManager(this).getUserId();
        }

        tvWelcome = findViewById(R.id.tvWelcome);
        tvStatus = findViewById(R.id.tvStatus);
        switchOnline = findViewById(R.id.switchOnline);
        btnAvailableOrders = findViewById(R.id.btnAvailableOrders);
        btnHistory = findViewById(R.id.btnHistory);
        btnLogout = findViewById(R.id.btnLogout);

        String phone = getIntent().getStringExtra("phone");
        if (phone != null) {
            tvWelcome.setText("Водитель: " + phone);
        } else {
            tvWelcome.setText("Водитель");
        }

        loadDriverStatus();

        switchOnline.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUpdating) {
                updateDriverOnlineStatus(isChecked);
            }
        });

        btnAvailableOrders.setOnClickListener(v -> {
            if (!switchOnline.isChecked()) {
                Toast.makeText(DriverMainActivity.this, "Включите статус 'На линии'", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(DriverMainActivity.this, AvailableOrdersActivity.class));
        });

        btnHistory.setOnClickListener(v -> {
            startActivity(new Intent(DriverMainActivity.this, DriverHistoryActivity.class));
        });

        btnLogout.setOnClickListener(v -> {
            if (switchOnline.isChecked()) {
                updateDriverOnlineStatus(false);
            }

            PreferenceManager prefManager = new PreferenceManager(DriverMainActivity.this);
            prefManager.clear();

            Intent intent = new Intent(DriverMainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadDriverStatus() {
        Call<Map<String, Object>> call = RetrofitClient.getApiService().getDriverStatus(userId);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    boolean isOnline = (boolean) response.body().get("isOnline");
                    isUpdating = true;
                    switchOnline.setChecked(isOnline);
                    isUpdating = false;
                    updateUI(isOnline);
                    if (isOnline) {
                        startSendingLocation();
                    } else {
                        stopSendingLocation();
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void updateDriverOnlineStatus(boolean isOnline) {
        Map<String, Object> body = new HashMap<>();
        body.put("driverId", userId);
        body.put("isOnline", isOnline);

        Call<Void> call = RetrofitClient.getApiService().updateDriverStatus(body);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    updateUI(isOnline);
                    if (isOnline) {
                        startSendingLocation();
                        Toast.makeText(DriverMainActivity.this, "Вы на линии", Toast.LENGTH_SHORT).show();
                    } else {
                        stopSendingLocation();
                        Toast.makeText(DriverMainActivity.this, "Вы не на линии", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(DriverMainActivity.this, "Ошибка обновления статуса", Toast.LENGTH_SHORT).show();
                    isUpdating = true;
                    switchOnline.setChecked(!isOnline);
                    isUpdating = false;
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(DriverMainActivity.this, "Нет связи с сервером", Toast.LENGTH_SHORT).show();
                isUpdating = true;
                switchOnline.setChecked(!isOnline);
                isUpdating = false;
            }
        });
    }

    private void updateUI(boolean isOnline) {
        if (isOnline) {
            tvStatus.setText("Статус: На линии");
            tvStatus.setTextColor(getColor(android.R.color.holo_green_dark));
        } else {
            tvStatus.setText("Статус: Не на линии");
            tvStatus.setTextColor(getColor(android.R.color.darker_gray));
        }
    }

    private void startSendingLocation() {
        if (locationTracker == null) {
            locationTracker = new LocationTracker(this);
        }
        locationTracker.startLocationUpdates(location -> {
            if (location != null && switchOnline.isChecked()) {
                sendLocationToServer(location.getLatitude(), location.getLongitude());
            }
        });
    }

    private void stopSendingLocation() {
        if (locationTracker != null) {
            locationTracker.stopLocationUpdates();
            isSendingLocation = false;
        }
        if (handler != null && sendLocationRunnable != null) {
            handler.removeCallbacks(sendLocationRunnable);
        }
    }

    private void sendLocationToServer(double lat, double lon) {
        DriverLocation loc = new DriverLocation();
        loc.setDriverId(userId);
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
    protected void onDestroy() {
        if (switchOnline.isChecked()) {
            Map<String, Object> body = new HashMap<>();
            body.put("driverId", userId);
            body.put("isOnline", false);
            RetrofitClient.getApiService().updateDriverStatus(body).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {}
                @Override
                public void onFailure(Call<Void> call, Throwable t) {}
            });
        }
        stopSendingLocation();
        super.onDestroy();
    }
}