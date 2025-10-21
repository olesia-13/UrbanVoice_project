package com.golap.urbanvoice;

import android.os.Bundle;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.activity.EdgeToEdge;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class RouteMap extends AppCompatActivity implements OnMapReadyCallback{

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean isAudioGuideRunning = false; // Стан аудіогіда

    // UI Елементи
    private TextView routeTitle;
    private TextView nextStationText;
    private ImageButton startAudioButton;
    private ImageView routeIcon; // Іконка маршруту

    // Дані маршруту
    private String routeKey;
    private String routeDisplayName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_route_map);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Отримання даних маршруту (з SelectRoute Activity)
        Intent intent = getIntent();
        routeKey = intent.getStringExtra("ROUTE_KEY");
        routeDisplayName = intent.getStringExtra("ROUTE_DISPLAY_NAME");
        // Припускаємо, що ви передаєте ID ресурсу іконки, або встановіть дефолтну
        int iconId = intent.getIntExtra("ROUTE_ICON_ID", R.drawable.ic_bus);

        // 2. Ініціалізація UI
        routeTitle = findViewById(R.id.route_map_title);
        nextStationText = findViewById(R.id.next_station_text);
        startAudioButton = findViewById(R.id.start_audio_button);
        routeIcon = findViewById(R.id.route_map_icon);

        routeTitle.setText(routeDisplayName != null ? routeDisplayName : "Маршрут");
        routeIcon.setImageResource(iconId);
        nextStationText.setText("Очікування даних GPS...");

        // 3. Налаштування кнопок верхньої панелі
        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        findViewById(R.id.home_button).setOnClickListener(v -> {
            // Перехід на MainActivity та очищення стеку
            Intent homeIntent = new Intent(this, MainActivity.class);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(homeIntent);
        });

        // 4. Ініціалізація Google Maps
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // 5. Ініціалізація Location Client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 6. Обробка кнопки Start/Stop Audio
        startAudioButton.setOnClickListener(v -> toggleAudioGuide());
    }

    // ------------------- Логіка КАРТИ (OnMapReadyCallback) -------------------

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // === ТЕСТ: Додавання мітки на Київ ===
        LatLng kyivCenter = new LatLng(50.4501, 30.5234);
        mMap.addMarker(new MarkerOptions().position(kyivCenter).title("Центр Києва"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kyivCenter, 12f));
        // =====================================

        // Увімкнення шару розташування користувача (синя/жовта точка)
        enableUserLocationLayer();

        // TODO: Тут буде логіка малювання Polyline та розміщення міток станцій
    }

    private void enableUserLocationLayer() {
        // Перевірка дозволу перед активацією MyLocationLayer
        if (checkLocationPermission() && mMap != null) {
            // Нам потрібна перевірка checkSelfPermission, оскільки setMyLocationEnabled може кинути SecurityException
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                mMap.setMyLocationEnabled(true);
            }
        }
    }

    // ------------------- Логіка КНОПКИ Audio/Stop -------------------

    private void toggleAudioGuide() {
        if (isAudioGuideRunning) {
            // Якщо зараз працює -> зупинити
            stopAudioGuide();
        } else {
            // Якщо не працює -> запустити (після перевірки дозволу)
            if (checkLocationPermission()) {
                startAudioGuide();
            } else {
                requestLocationPermission();
            }
        }
    }

    private void startAudioGuide() {
        // Логіка для запуску аудіогіда
        isAudioGuideRunning = true;
        startAudioButton.setImageResource(R.drawable.ic_stop); // Змінюємо іконку на "стоп"
        // startAudioButton.setBackgroundResource(R.drawable.rounded_red_background); // Змінюємо колір, якщо потрібно
        Toast.makeText(this, "Аудіогід ЗАПУЩЕНО: " + routeDisplayName, Toast.LENGTH_SHORT).show();

        // TODO: Запуск LocationAudioService (Foreground Service)
    }

    private void stopAudioGuide() {
        // Логіка для зупинки аудіогіда
        isAudioGuideRunning = false;
        startAudioButton.setImageResource(R.drawable.ic_start); // Змінюємо іконку на "старт"
        // startAudioButton.setBackgroundResource(R.drawable.rounded_line_background); // Повертаємо початковий колір
        Toast.makeText(this, "Аудіогід ЗУПИНЕНО.", Toast.LENGTH_SHORT).show();

        // TODO: Зупинка LocationAudioService
    }

    // ------------------- Логіка ДОЗВОЛІВ -------------------

    private boolean checkLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Дозвіл отримано, можна спробувати запустити аудіогід
                enableUserLocationLayer();
                startAudioGuide();
            } else {
                Toast.makeText(this, "Потрібен дозвіл на геолокацію для роботи аудіогіда", Toast.LENGTH_LONG).show();
            }
        }


    }
}