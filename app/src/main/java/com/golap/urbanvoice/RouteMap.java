package com.golap.urbanvoice;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class RouteMap extends AppCompatActivity implements OnMapReadyCallback {

    // --- Карта та GPS ---
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean isAudioGuideRunning = false;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    // --- UI Елементи ---
    private TextView routeTitle;
    private TextView nextStationText;
    private ImageButton startAudioButton;
    private ImageView routeIcon;
    private ImageButton textButton;
    private ImageButton photoButton;

    // --- Дані маршруту ---
    private String routeKey; // Базовий ключ (наприклад, R001)
    private String routeDisplayName;
    private RouteData currentRouteData;
    // Напрямок, що визначається динамічно при старті
    private String currentDirection = null;

    // --- Local Broadcast ---
    private BroadcastReceiver locationUpdateReceiver;
    private boolean receiverRegistered = false;

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

        // 1. Ініціалізація Launcher для дозволів
        setupPermissionLauncher();

        // 2. Отримання та завантаження даних маршруту
        Intent intent = getIntent();
        String fullRouteKey = intent.getStringExtra("ROUTE_KEY");

        // !!! ДИНАМІЧНЕ ВИЗНАЧЕННЯ НАПРЯМКУ: Використовуємо лише базовий ключ !!!
        routeKey = fullRouteKey;
        // Припускаємо, що якщо ключ має суфікс "_A" або "_B", ми його видаляємо
        if (routeKey != null && (routeKey.endsWith("_A") || routeKey.endsWith("_B"))) {
            routeKey = routeKey.substring(0, routeKey.length() - 2);
        }

        routeDisplayName = intent.getStringExtra("ROUTE_DISPLAY_NAME");
        int iconId = intent.getIntExtra("ROUTE_ICON_ID", R.drawable.ic_bus);

        // Завантажуємо дані маршруту (які містять обидва напрямки)
        currentRouteData = MapDataManager.getRouteData(routeKey);

        // 3. Ініціалізація UI
        routeTitle = findViewById(R.id.route_map_title);
        nextStationText = findViewById(R.id.next_station_text);
        startAudioButton = findViewById(R.id.start_audio_button);
        routeIcon = findViewById(R.id.route_map_icon);
        textButton = findViewById(R.id.text_button);
        photoButton = findViewById(R.id.photo_button);

        routeTitle.setText(routeDisplayName != null ? routeDisplayName : "Маршрут");
        routeIcon.setImageResource(iconId);
        // ПОТРІБЕН R.string.next_station_placeholder
        nextStationText.setText(getString(R.string.next_station_placeholder));

        // 4. Налаштування кнопок верхньої панелі та UI
        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        findViewById(R.id.home_button).setOnClickListener(v -> {
            Intent homeIntent = new Intent(this, MainActivity.class);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(homeIntent);
        });

        // 5. Ініціалізація Google Maps та Location Client
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 6. Обробка кнопки Start/Stop Audio
        // Тепер ця кнопка запускає логіку визначення напрямку
        startAudioButton.setOnClickListener(v -> toggleAudioGuide());
        updateButtonUI(false);

        // 7. Налаштування приймача оновлень від сервісу
        setupLocationUpdateReceiver();

        // 8. Обробка кнопок Photo та Text
        textButton.setOnClickListener(v -> showFullTextGuide());
        photoButton.setOnClickListener(v -> showPhotos());
    }

    // =======================================================
    // I. ЖИТТЄВИЙ ЦИКЛ
    // =======================================================

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver();
    }

    // =======================================================
    // II. ЛОГІКА LOCAL BROADCAST RECEIVER
    // =======================================================

    private void setupLocationUpdateReceiver() {
        locationUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (LocationAudioService.ACTION_LOCATION_UPDATE.equals(intent.getAction())) {
                    String nextStationName = intent.getStringExtra(LocationAudioService.EXTRA_NEXT_STATION_NAME);
                    boolean isFinished = intent.getBooleanExtra(LocationAudioService.EXTRA_ROUTE_FINISHED, false);

                    // Оновлення UI
                    nextStationText.setText(nextStationName);

                    if (isFinished) {
                        // Якщо маршрут завершено, зупиняємо сервіс і оновлюємо кнопку
                        stopAudioGuide();
                        // Виводимо повідомлення про завершення (текст вже має бути у nextStationName)
                        Toast.makeText(context, nextStationName, Toast.LENGTH_LONG).show();
                    }
                }
            }
        };
    }

    private void registerReceiver() {
        if (!receiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(
                    locationUpdateReceiver,
                    new IntentFilter(LocationAudioService.ACTION_LOCATION_UPDATE)
            );
            receiverRegistered = true;
        }
    }

    private void unregisterReceiver() {
        if (receiverRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(locationUpdateReceiver);
            receiverRegistered = false;
        }
    }

    // =======================================================
    // III. ЛОГІКА GOOGLE MAPS - СТАТИЧНІ МАРКЕРИ
    // =======================================================

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        if (currentRouteData != null) {
            // Відображення полілінії маршруту
            String encodedPolyline = currentRouteData.getPolylineEncoded();
            drawRouteOnMap(encodedPolyline);

            // ОНОВЛЕНО: Відображаємо УСІ статичні маркери для даного маршруту (використовуючи координати як ID)
            placeAllRouteMarkers();

            // Рухаємо камеру до першої точки маршруту (напрямок A)
            List<Station> stations = currentRouteData.getForwardStations();

            if (!stations.isEmpty()) {
                Station firstStation = stations.get(0);
                LatLng startPoint = new LatLng(firstStation.getLatitude(), firstStation.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPoint, 12f));
            }
        } else {
            // Маршрут не знайдено, переходимо на Київ
            LatLng kyivCenter = new LatLng(50.4501, 30.5234);
            mMap.addMarker(new MarkerOptions().position(kyivCenter).title("Центр Києва"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kyivCenter, 12f));
        }

        enableUserLocationLayer();
    }

    private void drawRouteOnMap(String encodedPolyline) {
        if (mMap == null || encodedPolyline == null || encodedPolyline.isEmpty()) return;

        try {
            List<LatLng> decodedPath = PolyUtil.decode(encodedPolyline);

            PolylineOptions polylineOptions = new PolylineOptions()
                    .addAll(decodedPath)
                    // ПОТРІБЕН R.color.route_line_color
                    .width(10)
                    .color(ContextCompat.getColor(this, R.color.route_line_color));

            mMap.addPolyline(polylineOptions);

        } catch (Exception e) {
            Toast.makeText(this, "Помилка декодування маршруту", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    /**
     * Розміщує всі унікальні станції маршруту на карті.
     * Використовує широту та довготу як унікальний ID, оскільки метод getId() відсутній у Station.
     */
    private void placeAllRouteMarkers() {
        if (mMap == null || currentRouteData == null) return;

        // Використовуємо Set для зберігання унікальних координат як ID, щоб уникнути дублікатів.
        Set<String> placedStationLocations = new HashSet<>();

        // Обробка станцій Forward
        placeMarkersFromList(currentRouteData.getForwardStations(), placedStationLocations);

        // Обробка станцій Backward (додавання лише тих, які ще не були додані)
        placeMarkersFromList(currentRouteData.getBackwardStations(), placedStationLocations);
    }

    /**
     * Допоміжний метод для розміщення маркерів зі списку станцій.
     * @param placedStationLocations Набір унікальних location-ідентифікаторів, щоб уникнути дублювання.
     */
    private void placeMarkersFromList(List<Station> stations, Set<String> placedStationLocations) {
        // ПОТРІБЕН R.drawable.ic_station_mark
        BitmapDescriptor stationIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_station_mark);

        for (Station station : stations) {
            // КОРИГОВАНА ЛОГІКА: Створення унікального ID на основі координат (String.format для точності)
            String locationId = String.format(Locale.US, "%.6f,%.6f", station.getLatitude(), station.getLongitude());

            // Перевіряємо, чи ми вже розмістили цю станцію за її координатами
            if (!placedStationLocations.contains(locationId)) {
                LatLng position = new LatLng(station.getLatitude(), station.getLongitude());

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(position)
                        .title(getString(station.getNameResId()))
                        .icon(stationIcon);

                mMap.addMarker(markerOptions);
                placedStationLocations.add(locationId); // Додаємо location ID до набору розміщених
            }
        }
    }

    @SuppressWarnings("MissingPermission")
    private void enableUserLocationLayer() {
        if (checkLocationPermission() && mMap != null) {
            mMap.setMyLocationEnabled(true);
        }
    }

    // =======================================================
    // IV. ЛОГІКА ДОЗВОЛІВ
    // =======================================================

    private void setupPermissionLauncher() {
        requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        enableUserLocationLayer();
                        // Якщо дозволи надано, продовжуємо процес запуску гіда
                        determineRouteDirectionAndStartGuide();
                    } else {
                        Toast.makeText(this, "Потрібен доступ до місцезнаходження для аудіогіда.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    // =======================================================
    // V. ЛОГІКА СЕРВІСУ (Audio/Stop)
    // =======================================================

    private void toggleAudioGuide() {
        if (isAudioGuideRunning) {
            stopAudioGuide();
        } else {
            if (currentRouteData == null) {
                Toast.makeText(this, "Помилка: Не знайдено даних маршруту.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (checkLocationPermission()) {
                // Запускаємо процес визначення напрямку
                determineRouteDirectionAndStartGuide();
            } else {
                requestLocationPermission();
            }
        }
    }

    /**
     * Визначає напрямок руху користувача та запускає сервіс.
     */
    @SuppressWarnings("MissingPermission")
    private void determineRouteDirectionAndStartGuide() {
        if (!checkLocationPermission()) {
            Toast.makeText(this, "Потрібен дозвіл на місцезнаходження.", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());

                // --- ВИПРАВЛЕНО: Викликаємо determineOptimalDirection з двома аргументами ---
                String determinedDirection = MapDataManager.determineOptimalDirection(
                        routeKey, // Передаємо ключ маршруту
                        userLocation // Передаємо місцезнаходження користувача
                );

                if (determinedDirection != null) {
                    currentDirection = determinedDirection;
                    // Карта не оновлюється, лише запускається сервіс
                    startAudioGuideService(determinedDirection);
                } else {
                    // Користувач занадто далеко від маршруту
                    Toast.makeText(this, "Визначення напрямку не вдалося. Наблизьтесь до маршруту.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Не вдалося отримати поточне місцезнаходження. Спробуйте пізніше.", Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Помилка отримання місцезнаходження.", Toast.LENGTH_LONG).show();
        });
    }


    private void startAudioGuideService(String direction) {
        Intent serviceIntent = new Intent(this, LocationAudioService.class);
        serviceIntent.putExtra("ROUTE_KEY", routeKey);
        serviceIntent.putExtra("DIRECTION", direction); // Використовуємо динамічно визначений напрямок

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        isAudioGuideRunning = true;
        updateButtonUI(true);
        Toast.makeText(this, "Аудіогід ЗАПУЩЕНО (" + direction + ")", Toast.LENGTH_SHORT).show();
    }

    private void stopAudioGuide() {
        Intent serviceIntent = new Intent(this, LocationAudioService.class);
        stopService(serviceIntent);

        isAudioGuideRunning = false;
        updateButtonUI(false);
        Toast.makeText(this, "Аудіогід ЗУПИНЕНО.", Toast.LENGTH_SHORT).show();

        // ПОТРІБЕН R.string.next_station_placeholder
        nextStationText.setText(getString(R.string.next_station_placeholder));
        currentDirection = null; // Скидаємо визначений напрямок
    }

    private void updateButtonUI(boolean isRunning) {
        // ПОТРІБЕН R.drawable.ic_stop та R.drawable.ic_start
        if (isRunning) {
            startAudioButton.setImageResource(R.drawable.ic_stop);
        } else {
            startAudioButton.setImageResource(R.drawable.ic_start);
        }
    }

    // =======================================================
    // VI. ЛОГІКА КНОПКИ ТЕКСТУ ТА ФОТО
    // =======================================================

    private void showFullTextGuide() {
        if (currentRouteData == null || currentDirection == null) {
            Toast.makeText(this, "Спочатку запустіть аудіогід для визначення напрямку.", Toast.LENGTH_LONG).show();
            return;
        }

        // Використовуємо визначений напрямок для вибору тексту
        int textResId = MapDataManager.getTextResIdForDirection(routeKey, currentDirection);

        if (textResId != 0) {
            // !!! УЗГОДЖЕНЕ ІМ'Я КЛАСУ TextGuideActivity.class !!!
            Intent textIntent = new Intent(this, TextActivity.class);
            textIntent.putExtra("ROUTE_DISPLAY_NAME", routeDisplayName);
            textIntent.putExtra("TEXT_RES_ID", textResId);
            startActivity(textIntent);
        } else {
            Toast.makeText(this, "Текстовий гід не знайдено.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPhotos() {
        Intent photoIntent = new Intent(this, PhotoActivity.class);
        // Передаємо назву маршруту, щоб знати, які фотографії завантажувати
        photoIntent.putExtra("ROUTE_DISPLAY_NAME", routeDisplayName);
        photoIntent.putExtra("ROUTE_KEY", routeKey);
        startActivity(photoIntent);
    }
}