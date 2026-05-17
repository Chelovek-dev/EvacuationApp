package com.example.evacuationapp.client;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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

import org.osmdroid.util.GeoPoint;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateOrderActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final int SELECT_PICKUP_LOCATION_REQUEST_CODE = 101;
    private static final int SELECT_DROPOFF_LOCATION_REQUEST_CODE = 102;

    private EditText etPickup, etDropoff, etContactPhone, etComment;
    private TextView tvPrice;
    private Button btnCreate, btnCancel, btnCurrentLocation, btnPickupOnMap, btnSelectOnMap;
    private long clientId;
    private FusedLocationProviderClient fusedLocationClient;
    private int currentPrice = 3500;
    private boolean isPickupValid = false;
    private boolean isDropoffValid = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_order);

        clientId = new PreferenceManager(this).getUserId();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        etPickup = findViewById(R.id.etPickupAddress);
        etDropoff = findViewById(R.id.etDropoffAddress);
        etContactPhone = findViewById(R.id.etContactPhone);
        etComment = findViewById(R.id.etComment);
        tvPrice = findViewById(R.id.tvPrice);
        btnCreate = findViewById(R.id.btnCreateOrder);
        btnCancel = findViewById(R.id.btnCancel);
        btnCurrentLocation = findViewById(R.id.btnCurrentLocation);
        btnPickupOnMap = findViewById(R.id.btnPickupOnMap);
        btnSelectOnMap = findViewById(R.id.btnSelectOnMap);

        btnCreate.setOnClickListener(v -> createOrder());
        btnCancel.setOnClickListener(v -> finish());
        btnCurrentLocation.setOnClickListener(v -> getCurrentLocation());

        btnPickupOnMap.setOnClickListener(v -> {
            Intent intent = new Intent(CreateOrderActivity.this, SelectLocationActivity.class);
            startActivityForResult(intent, SELECT_PICKUP_LOCATION_REQUEST_CODE);
        });

        btnSelectOnMap.setOnClickListener(v -> {
            Intent intent = new Intent(CreateOrderActivity.this, SelectLocationActivity.class);
            startActivityForResult(intent, SELECT_DROPOFF_LOCATION_REQUEST_CODE);
        });

        // Слушатели изменения текста
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateAddresses();
                calculateAndUpdatePrice();
            }
        };

        etPickup.addTextChangedListener(textWatcher);
        etDropoff.addTextChangedListener(textWatcher);
    }

    private void validateAddresses() {
        String pickup = etPickup.getText().toString().trim();
        String dropoff = etDropoff.getText().toString().trim();

        if (!TextUtils.isEmpty(pickup)) {
            checkAddressExists(pickup, true);
        } else {
            isPickupValid = false;
        }

        if (!TextUtils.isEmpty(dropoff)) {
            checkAddressExists(dropoff, false);
        } else {
            isDropoffValid = false;
        }
    }

    private void checkAddressExists(String address, boolean isPickup) {
        new Thread(() -> {
            // Проверяем, есть ли геокоординаты у введённого адреса
            GeoPoint point = getCoordinatesFromAddress(address);
            String houseNumber = extractHouseNumber(address);
            boolean hasHouseNumber = (houseNumber != null && !houseNumber.isEmpty());

            // Дополнительная проверка: есть ли вообще какая-то улица/место
            boolean anyMatch = isAnyAddressExists(address);

            runOnUiThread(() -> {
                String errorMsg = null;
                boolean isValid = false;

                if (point == null && !anyMatch) {
                    // Случай 1: адрес вообще не найден
                    errorMsg = "Адрес не найден. Проверьте правильность ввода";
                    isValid = false;
                } else if (hasHouseNumber && point == null) {
                    // Случай 2: улица есть, но номер дома не найден
                    errorMsg = "Дом " + houseNumber + " не найден. Проверьте номер";
                    isValid = false;
                } else {
                    // Случай 3: адрес корректен
                    isValid = true;
                }

                if (isPickup) {
                    isPickupValid = isValid;
                    if (errorMsg != null) {
                        etPickup.setError(errorMsg);
                    } else {
                        etPickup.setError(null);
                    }
                } else {
                    isDropoffValid = isValid;
                    if (errorMsg != null) {
                        etDropoff.setError(errorMsg);
                    } else {
                        etDropoff.setError(null);
                    }
                }
            });
        }).start();
    }

    private boolean isAnyAddressExists(String address) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            return addresses != null && !addresses.isEmpty();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

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
                if (addressLine != null && !addressLine.isEmpty()) {
                    etPickup.setText(addressLine);
                    Toast.makeText(this, "Адрес определён", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Адрес не найден", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Адрес не найден", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка геокодирования", Toast.LENGTH_SHORT).show();
        }
    }

    private void calculateAndUpdatePrice() {
        String pickup = etPickup.getText().toString().trim();
        String dropoff = etDropoff.getText().toString().trim();

        if (TextUtils.isEmpty(pickup) || TextUtils.isEmpty(dropoff)) {
            tvPrice.setText("Стоимость: рассчитывается...");
            return;
        }

        new Thread(() -> {
            GeoPoint pickupPoint = getCoordinatesFromAddress(pickup);
            GeoPoint dropoffPoint = getCoordinatesFromAddress(dropoff);

            if (pickupPoint != null && dropoffPoint != null) {
                double distance = calculateDistance(
                        pickupPoint.getLatitude(), pickupPoint.getLongitude(),
                        dropoffPoint.getLatitude(), dropoffPoint.getLongitude()
                );
                int price = calculatePrice(distance);
                currentPrice = price;
                runOnUiThread(() -> {
                    tvPrice.setText("Стоимость: " + price + " ₽");
                });
            } else {
                runOnUiThread(() -> {
                    tvPrice.setText("Стоимость: не удалось рассчитать");
                });
            }
        }).start();
    }

    private GeoPoint getCoordinatesFromAddress(String address) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            // Получаем несколько вариантов адреса для проверки
            List<Address> addresses = geocoder.getFromLocationName(address, 5);
            if (addresses != null && !addresses.isEmpty()) {

                // Извлекаем номер дома из введённой строки
                String houseNumber = extractHouseNumber(address);
                boolean hasHouseNumber = (houseNumber != null && !houseNumber.isEmpty());

                // Ищем адрес с совпадающим номером дома
                for (Address loc : addresses) {
                    String addressLine = loc.getAddressLine(0);
                    double lat = loc.getLatitude();
                    double lon = loc.getLongitude();

                    if (lat == 0.0 && lon == 0.0) continue;

                    // Если номер дома не требуется или он совпадает
                    if (!hasHouseNumber) {
                        return new GeoPoint(lat, lon);
                    }

                    // Проверяем, содержит ли найденный адрес нужный номер дома
                    if (addressLine != null && addressLine.contains(houseNumber)) {
                        return new GeoPoint(lat, lon);
                    }
                }

                // Если номер дома был в запросе, но не найден — возвращаем null
                if (hasHouseNumber) {
                    return null;
                }

                // Если номер дома не требовался, возвращаем первый результат
                Address first = addresses.get(0);
                return new GeoPoint(first.getLatitude(), first.getLongitude());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String extractHouseNumber(String address) {
        // Ищем номер дома в строке (цифры, возможно с буквой)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b(\\d+[а-я]?)\\b");
        java.util.regex.Matcher matcher = pattern.matcher(address);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private int calculatePrice(double distanceKm) {
        int price;
        if (distanceKm > 20) {
            price = 3000 + (int) Math.round(distanceKm * 70);
        } else if (distanceKm > 10) {
            price = 4500;
        } else if (distanceKm > 7) {
            price = 4000;
        } else {
            price = 3500;
        }
        return roundTo500(price);
    }

    private int roundTo500(int price) {
        return (int) (Math.round(price / 500.0) * 500);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            double lat = data.getDoubleExtra("latitude", 0);
            double lon = data.getDoubleExtra("longitude", 0);
            if (lat != 0 && lon != 0) {
                new Thread(() -> {
                    String address = getAddressFromCoordinates(lat, lon);
                    runOnUiThread(() -> {
                        if (requestCode == SELECT_PICKUP_LOCATION_REQUEST_CODE) {
                            etPickup.setText(address);
                        } else if (requestCode == SELECT_DROPOFF_LOCATION_REQUEST_CODE) {
                            etDropoff.setText(address);
                        }
                    });
                }).start();
            }
        }
    }

    private String getAddressFromCoordinates(double lat, double lon) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private void createOrder() {
        String pickup = etPickup.getText().toString().trim();
        String dropoff = etDropoff.getText().toString().trim();
        String contactPhone = etContactPhone.getText().toString().trim();
        String comment = etComment.getText().toString().trim();

        if (TextUtils.isEmpty(pickup) || TextUtils.isEmpty(dropoff)) {
            Toast.makeText(this, "Заполните все адреса", Toast.LENGTH_SHORT).show();
            return;
        }

        // Проверка валидности адресов
        if (!isPickupValid) {
            new AlertDialog.Builder(this)
                    .setTitle("Адрес не найден")
                    .setMessage("Адрес подачи не удалось распознать. Пожалуйста, уточните адрес или выберите на карте.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        if (!isDropoffValid) {
            new AlertDialog.Builder(this)
                    .setTitle("Адрес не найден")
                    .setMessage("Адрес назначения не удалось распознать. Пожалуйста, уточните адрес или выберите на карте.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        Order order = new Order();
        order.setClientId(clientId);
        order.setPickupAddress(pickup);
        order.setDropoffAddress(dropoff);
        order.setStatus("waiting");
        order.setPrice(currentPrice);
        order.setContactPhone(contactPhone);
        order.setComment(comment);

        Call<Order> call = RetrofitClient.getApiService().createOrder(order);
        call.enqueue(new Callback<Order>() {
            @Override
            public void onResponse(Call<Order> call, Response<Order> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Order createdOrder = response.body();
                    Toast.makeText(CreateOrderActivity.this, "Заказ создан! Стоимость: " + currentPrice + " ₽", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(CreateOrderActivity.this, TrackOrderActivity.class);
                    intent.putExtra("orderId", createdOrder.getOrderId());
                    startActivity(intent);
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