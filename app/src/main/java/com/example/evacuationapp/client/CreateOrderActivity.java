package com.example.evacuationapp.client;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.evacuationapp.R;
import com.example.evacuationapp.models.Order;
import com.example.evacuationapp.network.RetrofitClient;
import com.example.evacuationapp.utils.PreferenceManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateOrderActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    private EditText etPickup, etDropoff;
    private Button btnCreate, btnCancel, btnCurrentLocation;
    private long clientId;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_order);

        clientId = new PreferenceManager(this).getUserId();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        etPickup = findViewById(R.id.etPickupAddress);
        etDropoff = findViewById(R.id.etDropoffAddress);
        btnCreate = findViewById(R.id.btnCreateOrder);
        btnCancel = findViewById(R.id.btnCancel);
        btnCurrentLocation = findViewById(R.id.btnCurrentLocation);

        btnCreate.setOnClickListener(v -> createOrder());
        btnCancel.setOnClickListener(v -> finish());
        btnCurrentLocation.setOnClickListener(v -> getCurrentLocation());
    }

    private void getCurrentLocation() {
        // Проверяем разрешения
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Получаем последнюю известную позицию
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            getAddressFromLocation(location.getLatitude(), location.getLongitude());
                        } else {
                            Toast.makeText(CreateOrderActivity.this,
                                    "Не удалось определить местоположение. Попробуйте позже.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String addressLine = address.getAddressLine(0);
                etPickup.setText(addressLine);
                Toast.makeText(this, "Адрес определён", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Адрес не найден", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка геокодирования", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Нужно разрешение на определение местоположения",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void createOrder() {
        String pickup = etPickup.getText().toString().trim();
        String dropoff = etDropoff.getText().toString().trim();

        if (TextUtils.isEmpty(pickup) || TextUtils.isEmpty(dropoff)) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        Order order = new Order();
        order.setClientId(clientId);
        order.setPickupAddress(pickup);
        order.setDropoffAddress(dropoff);
        order.setStatus("waiting");
        order.setPrice(1000.0);

        Call<Order> call = RetrofitClient.getApiService().createOrder(order);
        call.enqueue(new Callback<Order>() {
            @Override
            public void onResponse(Call<Order> call, Response<Order> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(CreateOrderActivity.this, "Заказ создан!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(CreateOrderActivity.this, "Ошибка создания заказа", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Order> call, Throwable t) {
                Toast.makeText(CreateOrderActivity.this, "Нет связи с сервером", Toast.LENGTH_SHORT).show();
            }
        });
    }
}