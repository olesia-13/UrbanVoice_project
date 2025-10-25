package com.golap.urbanvoice;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
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
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
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

    private static final String TAG = "RouteMap"; // Тег для логування

    // --- Карта та GPS ---
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean isAudioGuideRunning = false;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    // --- Контроль визначення напрямку ---
    private static final long LOCATION_REQUEST_INTERVAL = 1000; // Оновлення кожну 1 секунду
    private static final int MAX_DIRECTION_UPDATES = 3; // Чекаємо до 3 оновлень для визначення руху
    private LocationCallback directionCheckLocationCallback; // Колбек для перевірки напрямку
    private Location lastValidLocation; // Зберігає останню локацію для порівняння bearing
    private boolean isCheckingDirection = false;
    private int updateCount = 0;


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
    // Напрямок, що визначається динамічно при старті (наприклад, "_A" або "_B")
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
                        stopAudioGuide(true); // Передаємо true, бо маршрут завершено
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
                        // !!! ТЕПЕР ВИКЛИКАЄМО ТИМЧАСОВИЙ ЗБІР ДАНИХ ДЛЯ ВИЗНАЧЕННЯ НАПРЯМКУ !!!
                        startDirectionCheck();
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
            stopAudioGuide(false); // Зупиняємо, але зберігаємо напрямок
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
     * Запускає процес активного прослуховування локації для визначення напрямку.
     * Замінює стару логіку getLastLocation().
     */
    @SuppressWarnings("MissingPermission")
    private void determineRouteDirectionAndStartGuide() {
        if (!checkLocationPermission()) {
            Toast.makeText(this, "Потрібен дозвіл на місцезнаходження.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Нова логіка: запускаємо активну перевірку напрямку
        startDirectionCheck();
    }

    /**
     * Активно збирає дані GPS, щоб обчислити напрямок руху (Bearing).
     */
    @SuppressWarnings("MissingPermission")
    private void startDirectionCheck() {
        if (isCheckingDirection) return;
        isCheckingDirection = true;
        updateCount = 0;
        lastValidLocation = null; // Скидаємо попередню локацію

        Toast.makeText(this, "Визначаємо напрямок руху (3 сек.)...", Toast.LENGTH_SHORT).show();

        // 1. Створення LocationRequest для активного збору даних
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, LOCATION_REQUEST_INTERVAL)
                // ВИПРАВЛЕНО: Зменшуємо ліміт відстані до 1 метра
                .setMinUpdateDistanceMeters(1)
                .setMaxUpdates(MAX_DIRECTION_UPDATES)
                .build();

        // 2. Створення LocationCallback
        directionCheckLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location currentLocation = locationResult.getLastLocation();

                // 1. Обробка, якщо локація недійсна або не надана
                if (currentLocation == null) {
                    updateCount++;
                    if (updateCount >= MAX_DIRECTION_UPDATES) {
                        handleDirectionFailure(null, "Не вдалося отримати GPS-координати.");
                    }
                    return;
                }

                // === КРИТИЧНЕ ВИПРАВЛЕННЯ ЛОГІКИ РУХУ ===

                boolean movementDetected = false;

                if (lastValidLocation != null) {
                    // 2. Перевірка руху: порівнюємо з попереднім валідним значенням
                    if (currentLocation.distanceTo(lastValidLocation) >= 2.0f) {
                        movementDetected = true;
                    }
                }

                // 3. Зберігаємо поточну локацію для наступного порівняння
                lastValidLocation = currentLocation;
                updateCount++;

                // 4. Якщо руху недостатньо, і ще не досягнуто ліміту - чекаємо
                if (!movementDetected && updateCount < MAX_DIRECTION_UPDATES) {
                    return;
                }

                // 4. Якщо ліміт досягнуто АБО рух визначено - ПРИЙМАЄМО РІШЕННЯ

                // Визначення Bearing (Bearing між двома точками: lastValidLocation та currentLocation)
                float bearing = 0.0f; // Значення за замовчуванням

                // Використовуємо Bearing від самого GPS-сенсора (якщо він є і швидкість достатня)
                if (currentLocation.hasBearing() && currentLocation.getSpeed() > 0.5f) {
                    bearing = currentLocation.getBearing();
                }
                // Якщо рух визначено, але Bearing від сенсора немає, обчислюємо його
                else if (movementDetected) {
                    // Зауваження: в цьому оновленому коді lastValidLocation - це завжди остання успішна локація
                    if (lastValidLocation != null) {
                        bearing = lastValidLocation.bearingTo(currentLocation);
                    }
                }

                // 5. Визначення напрямку
                LatLng userLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

                String determinedDirection = MapDataManager.determineOptimalDirectionWithBearing(
                        routeKey,
                        userLocation,
                        bearing
                );

                // 6. Фінальний запуск або виведення помилки
                if (determinedDirection != null) {
                    currentDirection = determinedDirection;
                    // !!! ВИКЛИК СЕРВІСУ !!!
                    startAudioGuideService(determinedDirection, currentLocation);
                } else {
                    // !!! ВИКЛИК ПОМИЛКИ !!!
                    handleDirectionFailure(currentLocation, "Напрямок не визначено або ви далеко від маршруту. Спробуйте почати рух.");
                }

                // В кінці завжди зупиняємо активну перевірку
                stopDirectionCheck();
            }
        };

        // 4. Запуск запиту на оновлення
        fusedLocationClient.requestLocationUpdates(locationRequest, directionCheckLocationCallback, Looper.getMainLooper());
    }

    private void handleDirectionFailure(Location location, String message) {
        Toast.makeText(RouteMap.this, message, Toast.LENGTH_LONG).show();
        stopDirectionCheck();
        // ВАЖЛИВО: Оновіть UI, якщо гід не запущено
        updateButtonUI(false);
    }

    // Функція для зупинки активного прослуховування локації
    private void stopDirectionCheck() {
        if (isCheckingDirection) {
            fusedLocationClient.removeLocationUpdates(directionCheckLocationCallback);
            lastValidLocation = null;
            isCheckingDirection = false;
            updateCount = 0;
        }
    }


    /**
     * Запускає LocationAudioService, передаючи визначений напрямок та початкові координати.
     */
    private void startAudioGuideService(String direction, Location location) {
        Log.d(TAG, "Starting service with Direction: " + direction + " for RouteKey: " + routeKey);

        Intent serviceIntent = new Intent(this, LocationAudioService.class);
        serviceIntent.putExtra("ROUTE_KEY", routeKey);
        serviceIntent.putExtra("DIRECTION", direction);
        // !!! ПЕРЕДАЄМО КООРДИНАТИ ДЛЯ ПОЧАТКОВОГО ВИЗНАЧЕННЯ СТАНЦІЇ !!!
        serviceIntent.putExtra("START_LAT", location.getLatitude());
        serviceIntent.putExtra("START_LON", location.getLongitude());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        isAudioGuideRunning = true;
        updateButtonUI(true);
        Toast.makeText(this, "Аудіогід ЗАПУЩЕНО (" + direction + ")", Toast.LENGTH_SHORT).show();
    }

    /**
     * Зупиняє сервіс.
     * @param isRouteFinished Якщо true, скидаємо напрямок (маршрут пройдено).
     */
    private void stopAudioGuide(boolean isRouteFinished) {
        Intent serviceIntent = new Intent(this, LocationAudioService.class);
        stopService(serviceIntent);

        isAudioGuideRunning = false;
        updateButtonUI(false);
        Toast.makeText(this, "Аудіогід ЗУПИНЕНО.", Toast.LENGTH_SHORT).show();

        // ПОТРІБЕН R.string.next_station_placeholder
        nextStationText.setText(getString(R.string.next_station_placeholder));

        // КЛЮЧОВА ЗМІНА: Скидаємо напрямок ТІЛЬКИ, якщо маршрут ЗАВЕРШЕНО
        if (isRouteFinished) {
            currentDirection = null;
            Log.d(TAG, "Route finished. Direction reset to null.");
        } else {
            // Напрямок залишається збереженим для кнопки ТЕКСТУ
            Log.d(TAG, "Audio guide stopped manually. Direction remains: " + currentDirection);
        }
    }

    // Перевантажений метод для ручної зупинки
    private void stopAudioGuide() {
        stopAudioGuide(false);
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
        // Текст можна дивитися лише після визначення напрямку
        if (currentRouteData == null || currentDirection == null) {
            // Це спрацює, якщо не було жодного запуску АБО маршрут повністю пройдено
            Toast.makeText(this, "Спочатку запустіть аудіогід для визначення напрямку.", Toast.LENGTH_LONG).show();
            return;
        }

        // Використовуємо визначений напрямок для вибору тексту
        int textResId = MapDataManager.getTextResIdForDirection(routeKey, currentDirection);

        Log.d(TAG, "Showing text for saved Direction: " + currentDirection + " with ResId: " + textResId);

        if (textResId != 0) {
            // !!! УЗГОДЖЕНЕ ІМ'Я КЛАСУ TextActivity.class !!!
            Intent textIntent = new Intent(this, TextActivity.class);
            textIntent.putExtra("ROUTE_DISPLAY_NAME", routeDisplayName);
            // ПЕРЕДАЄМО ТІЛЬКИ ID РЕСУРСУ, ЩО МІСТИТЬ ПОВНИЙ ТЕКСТ ДЛЯ ВИЗНАЧЕНОГО НАПРЯМКУ
            textIntent.putExtra("TEXT_RES_ID", textResId);

            // **********************************************
            // 🚨 КРИТИЧНЕ ВИПРАВЛЕННЯ: Додаємо передачу ключа маршруту (R001)
            textIntent.putExtra("ROUTE_KEY", routeKey);
            // **********************************************

            // Додатково передаємо напрямок, якщо TextActivity його використовує
            textIntent.putExtra("DIRECTION", currentDirection);
            startActivity(textIntent);
        } else {
            Toast.makeText(this, "Текстовий гід не знайдено для напрямку " + currentDirection, Toast.LENGTH_SHORT).show();
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