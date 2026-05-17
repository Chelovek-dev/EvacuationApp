package com.example.evacuationapp.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class LocationTracker {
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private OnLocationUpdateListener listener;
    private Context context;

    public interface OnLocationUpdateListener {
        void onLocationUpdate(Location location);
    }

    public LocationTracker(Context context) {
        this.context = context;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    public void getLastLocation(OnLocationUpdateListener callback) {
        // Проверяем разрешение
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (callback != null) {
                callback.onLocationUpdate(null);
            }
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (callback != null) {
                        callback.onLocationUpdate(location);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onLocationUpdate(null);
                    }
                });
    }

    public void startLocationUpdates(OnLocationUpdateListener listener) {
        this.listener = listener;

        // Проверяем разрешение
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null && listener != null) {
                    Location lastLocation = locationResult.getLastLocation();
                    if (lastLocation != null) {
                        listener.onLocationUpdate(lastLocation);
                    }
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    public void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
    }
}