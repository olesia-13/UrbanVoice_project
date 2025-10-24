package com.golap.urbanvoice;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager; // <<< ДОДАНО ЦЕЙ ІМПОРТ

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.List;
import java.util.Locale;

public class LocationAudioService extends Service {

    private static final String TAG = "AudioService";
    private static final String CHANNEL_ID = "AudioGuideChannel";
    private static final int NOTIFICATION_ID = 101;
    private static final float GEOFENCE_RADIUS = 20.0f; // Радіус тригера (20 метрів)

    // Action для локального широкомовлення (для RouteMap)
    public static final String ACTION_LOCATION_UPDATE = "com.golap.urbanvoice.LOCATION_UPDATE";
    public static final String EXTRA_NEXT_STATION_NAME = "NEXT_STATION_NAME";
    public static final String EXTRA_ROUTE_FINISHED = "ROUTE_FINISHED";

    // GPS Клієнт та запити
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    // Дані маршруту
    private List<Station> stations;
    private int currentStationIndex = -1;
    private String routeKey;
    private String currentDirection;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: Сервіс створено.");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Налаштування LocationCallback для обробки нових координат
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                Location lastLocation = locationResult.getLastLocation();
                if (lastLocation != null) {
                    // ГЛАВНА ЛОГІКА: Перевірка позиції
                    checkCurrentLocation(lastLocation);
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;

        routeKey = intent.getStringExtra("ROUTE_KEY");
        String directionFromIntent = intent.getStringExtra("DIRECTION");

        // 1. Завантаження даних маршруту
        // ЗМІНА ТИПУ: Замінено MapDataManager.RouteData на Object, оскільки він не визначений
        Object routeData = MapDataManager.getRouteData(routeKey);

        if (routeData != null) {
            currentDirection = directionFromIntent;
            // Припускаємо, що MapDataManager.getStationsForDirection повертає List<Station>
            stations = MapDataManager.getStationsForDirection(routeKey, currentDirection);

            if (stations != null && !stations.isEmpty()) {
                currentStationIndex = 0; // Починаємо з першої станції
            }
        }

        Log.d(TAG, "onStartCommand: Запущено маршрут: " + routeKey + ", напрямок: " + currentDirection);

        // 2. Створення та запуск Foreground-режиму
        startForeground(NOTIFICATION_ID, buildNotification());

        // 3. Запит на оновлення місцезнаходження
        requestLocationUpdates();

        // 4. Оновлення UI при старті (Наступна станція - перша)
        if (stations != null && !stations.isEmpty()) {
            sendUIUpdate(getString(stations.get(0).getNameResId()), false);
        } else {
            // ПОТРІБЕН R.string.route_loading_error
            sendUIUpdate(getString(R.string.route_loading_error), false);
        }

        return START_STICKY;
    }

    // =======================================================
    // I. ЛОГІКА FOREGROUND ТА СПОВІЩЕНЬ
    // =======================================================

    private Notification buildNotification() {
        // Створення каналу сповіщень
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Аудіогід Маршруту", // ПОТРІБЕН R.string.channel_name
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // Інтент для повернення до RouteMap
        Intent notificationIntent = new Intent(this, RouteMap.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // Створення сповіщення
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("UrbanVoice") // ПОТРІБЕН R.string.notification_title
                .setContentText("Аудіогід активовано. Слідкування за маршрутом...") // ПОТРІБЕН R.string.notification_text
                .setSmallIcon(R.drawable.ic_bus) // ПОТРІБЕН R.drawable.ic_bus
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    // =======================================================
    // II. ЛОГІКА GPS ТА ТРИГЕРІВ
    // =======================================================

    @SuppressWarnings("MissingPermission")
    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Помилка: Дозволи на місцезнаходження відсутні в сервісі.");
            stopSelf();
            return;
        }

        // LocationRequest: оновлення кожні 5 секунд
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        Log.d(TAG, "Запит на оновлення місцезнаходження запущено.");
    }

    private void checkCurrentLocation(Location currentLocation) {
        // Перевірка кінця маршруту
        if (stations == null || stations.isEmpty() || currentStationIndex < 0 || currentStationIndex >= stations.size()) {
            if (stations != null && currentStationIndex >= stations.size()) {
                Log.i(TAG, "Кінець маршруту. Зупиняємо сервіс.");
                sendUIUpdate(getString(R.string.route_finished), true); // ПОТРІБЕН R.string.route_finished
            }
            return;
        }

        Station targetStation = stations.get(currentStationIndex);

        // Обчислення відстані
        Location targetLocation = new Location("target");
        targetLocation.setLatitude(targetStation.getLatitude());
        targetLocation.setLongitude(targetStation.getLongitude());

        float distanceToTarget = currentLocation.distanceTo(targetLocation); // Відстань у метрах

        // 1. Тригер: Якщо ми в радіусі GEOFENCE_RADIUS (20м)
        if (distanceToTarget <= GEOFENCE_RADIUS) {
            Log.d(TAG, String.format(Locale.getDefault(),
                    "ДОСЯГНУТО: Станція %s. Дистанція: %.1fм", getString(targetStation.getNameResId()), distanceToTarget));

            triggerAudioGuide(targetStation); // Викликаємо заглушку

            // Перехід до наступної станції
            currentStationIndex++;

            // Оновлення UI: наступна станція
            if (currentStationIndex < stations.size()) {
                Station nextStation = stations.get(currentStationIndex);
                sendUIUpdate(getString(nextStation.getNameResId()), false);
            }

        } else {
            // Логіка відтворення/зупинки аудіо зараз відключена.
            Log.d(TAG, String.format(Locale.getDefault(),
                    "NEXT: %s. Дистанція: %.1fм", getString(targetStation.getNameResId()), distanceToTarget));
        }
    }

    // =======================================================
    // III. ЛОГІКА АУДІО (ЗАГЛУШКА: тільки логування)
    // =======================================================

    private void triggerAudioGuide(Station station) {
        // Логіка відтворення аудіо тепер просто логується.
        String audioKey = station.getAudioResKey();

        Log.i(TAG, String.format(Locale.getDefault(),
                "*** ТРИГЕР АУДІО! *** Запустити аудіогід для станції: %s (Ключ: %s)",
                getString(station.getNameResId()), audioKey));
    }

    // =======================================================
    // IV. ОНОВЛЕННЯ UI (LocalBroadcast)
    // =======================================================

    private void sendUIUpdate(String nextStationName, boolean isFinished) {
        Intent intent = new Intent(ACTION_LOCATION_UPDATE);
        intent.putExtra(EXTRA_NEXT_STATION_NAME, nextStationName);
        intent.putExtra(EXTRA_ROUTE_FINISHED, isFinished);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // =======================================================
    // V. СИСТЕМНІ МЕТОДИ ТА ЗВІЛЬНЕННЯ РЕСУРСІВ
    // =======================================================

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopLocationUpdates();

        stopForeground(true);

        Log.d(TAG, "onDestroy: Сервіс успішно знищено.");
        super.onDestroy();
    }
}