package com.example.evacuationapp.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import androidx.core.app.ActivityCompat;
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

    public interface OnLocationUpdateListener {
        void onLocationUpdate(Location location);
    }

    public LocationTracker(Context context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    public void startLocationUpdates(Context context, OnLocationUpdateListener listener) {
        this.listener = listener;
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build();
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null && listener != null) {
                    listener.onLocationUpdate(locationResult.getLastLocation());
                }
            }
        };
        fusedLocationClient.requestLocationUpdates(request, locationCallback, null);
    }

    public void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}