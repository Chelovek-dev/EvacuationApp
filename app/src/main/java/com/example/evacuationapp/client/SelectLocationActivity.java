package com.example.evacuationapp.client;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.evacuationapp.R;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import java.io.File;

public class SelectLocationActivity extends AppCompatActivity {

    private MapView mapView;
    private Marker selectedMarker;
    private Button btnConfirm;
    private double selectedLat = 0;
    private double selectedLon = 0;

    // Координаты Перми
    private static final double PERM_LAT = 58.0105;
    private static final double PERM_LON = 56.2502;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Инициализация OSMDroid
        android.content.SharedPreferences osmdroidPref = androidx.preference.PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Configuration.getInstance().load(getApplicationContext(), osmdroidPref);
        Configuration.getInstance().setOsmdroidBasePath(new File(getCacheDir().getAbsolutePath(), "osmdroid"));
        Configuration.getInstance().setOsmdroidTileCache(new File(getCacheDir().getAbsolutePath(), "osmdroid/tiles"));

        setContentView(R.layout.activity_select_location);

        mapView = findViewById(R.id.mapView);
        btnConfirm = findViewById(R.id.btnConfirm);

        if (mapView != null) {
            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setMultiTouchControls(true);
            mapView.setBuiltInZoomControls(true);
            mapView.getController().setZoom(12.0);
            // Центрируем карту на Перми
            mapView.getController().setCenter(new GeoPoint(PERM_LAT, PERM_LON));

            // Создаём обработчик кликов по карте
            MapEventsReceiver receiver = new MapEventsReceiver() {
                @Override
                public boolean singleTapConfirmedHelper(GeoPoint point) {
                    // Удаляем старый маркер
                    if (selectedMarker != null) {
                        mapView.getOverlays().remove(selectedMarker);
                    }
                    // Добавляем новый маркер
                    selectedMarker = new Marker(mapView);
                    selectedMarker.setPosition(point);
                    selectedMarker.setIcon(getResources().getDrawable(android.R.drawable.ic_menu_mylocation));
                    selectedMarker.setTitle("Выбранная точка");
                    mapView.getOverlays().add(selectedMarker);
                    selectedLat = point.getLatitude();
                    selectedLon = point.getLongitude();
                    mapView.invalidate();
                    return true;
                }

                @Override
                public boolean longPressHelper(GeoPoint point) {
                    return false;
                }
            };

            MapEventsOverlay eventsOverlay = new MapEventsOverlay(receiver);
            mapView.getOverlays().add(eventsOverlay);
        }

        btnConfirm.setOnClickListener(v -> {
            if (selectedLat == 0 && selectedLon == 0) {
                Toast.makeText(this, "Выберите точку на карте", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent();
            intent.putExtra("latitude", selectedLat);
            intent.putExtra("longitude", selectedLon);
            setResult(RESULT_OK, intent);
            finish();
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
}