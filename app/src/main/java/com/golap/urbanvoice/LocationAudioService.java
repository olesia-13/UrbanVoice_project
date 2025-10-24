package com.golap.urbanvoice;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences; // <<< ДОДАНО
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaPlayer; // <<< ДОДАНО
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.List;
import java.util.Locale;

// УВАГА: Припускається, що MapDataManager, RouteData та R.raw.* існують.
public class LocationAudioService extends Service {

    private static final String TAG = "AudioService";
    private static final String CHANNEL_ID = "AudioGuideChannel";
    private static final int NOTIFICATION_ID = 101;
    private static final float GEOFENCE_RADIUS = 20.0f; // Радіус тригера (20 метрів)
    private static final String PREFS_NAME = "AppSettings"; // <<< ДОДАНО
    private static final String PREF_MUSIC_GENRE = "music_genre"; // <<< ДОДАНО

    // Action для локального широкомовлення (для RouteMap)
    public static final String ACTION_LOCATION_UPDATE = "com.golap.urbanvoice.LOCATION_UPDATE";
    public static final String EXTRA_NEXT_STATION_NAME = "NEXT_STATION_NAME";
    public static final String EXTRA_ROUTE_FINISHED = "ROUTE_FINISHED";

    // Audio Players
    private MediaPlayer guidePlayer; // <<< ДОДАНО
    private MediaPlayer musicPlayer; // <<< ДОДАНО
    private String musicGenre; // "Melody", "Classical", "None" <<< ДОДАНО

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

        // Ініціалізація плеєрів та їх слухачів
        guidePlayer = new MediaPlayer();
        musicPlayer = new MediaPlayer();
        musicPlayer.setLooping(true); // Музика має грати по колу

        // Встановлення слухача для гіда
        guidePlayer.setOnCompletionListener(mp -> {
            Log.d(TAG, "Аудіогід завершено. Запускаємо фонову музику.");
            startMusic(); // Запуск музики після завершення аудіогіда
        });

        // Налаштування LocationCallback для обробки нових координат
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                Location lastLocation = locationResult.getLastLocation();
                if (lastLocation != null) {
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
        // !!! НОВІ ПАРАМЕТРИ ДЛЯ ВИЗНАЧЕННЯ ПОЧАТКОВОЇ СТАНЦІЇ !!!
        double startLat = intent.getDoubleExtra("START_LAT", 0);
        double startLon = intent.getDoubleExtra("START_LON", 0);

        // 1. Зчитування налаштувань музики
        loadMusicSettings();

        // 2. Завантаження даних маршруту
        RouteData routeData = MapDataManager.getRouteData(routeKey); // Припускаємо, що RouteData існує

        if (routeData != null) {
            currentDirection = directionFromIntent;
            // ВИКОРИСТОВУЄМО ОДНОРАЗОВО ВИЗНАЧЕНИЙ НАПРЯМОК
            stations = MapDataManager.getStationsForDirection(routeKey, currentDirection);

            if (stations != null && !stations.isEmpty()) {
                // !!! ВИЗНАЧАЄМО СТАРТОВУ СТАНЦІЮ НА ОСНОВІ ПОЗИЦІЇ !!!
                currentStationIndex = findClosestStationIndex(startLat, startLon);

                // Перевірка на випадок, якщо користувач вже на кінцевій станції
                if (currentStationIndex == stations.size() - 1 &&
                        calculateDistanceToStation(startLat, startLon, stations.get(currentStationIndex)) < GEOFENCE_RADIUS) {
                    // Якщо ми вже на кінцевій станції і близько до неї, ініціюємо завершення
                    currentStationIndex++; // Перехід за межі списку для завершення при наступній перевірці
                }

                // Якщо індекс -1 (нічого не знайдено), повертаємо 0
                if (currentStationIndex == -1) currentStationIndex = 0;
            }
        }

        Log.d(TAG, "onStartCommand: Запущено маршрут: " + routeKey + ", напрямок: " + currentDirection + ". Початковий індекс: " + currentStationIndex);

        // 3. Створення та запуск Foreground-режиму
        startForeground(NOTIFICATION_ID, buildNotification());

        // 4. Запит на оновлення місцезнаходження
        requestLocationUpdates();

        // 5. Оновлення UI: відображаємо першу цільову станцію
        if (stations != null && currentStationIndex < stations.size() && currentStationIndex >= 0) {
            sendUIUpdate(getString(stations.get(currentStationIndex).getNameResId()), false);
        } else if (stations != null && currentStationIndex >= stations.size()) {
            // Маршрут завершено при старті
            sendUIUpdate(getString(R.string.route_finished), true); // ПОТРІБЕН R.string.route_finished
            stopSelf();
        } else {
            sendUIUpdate(getString(R.string.route_loading_error), false); // ПОТРІБЕН R.string.route_loading_error
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
                    "Аудіогід UrbanVoice", // ПОТРІБЕН R.string.channel_name
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
                .setContentTitle(getString(R.string.app_name)) // ПОТРІБЕН R.string.app_name
                .setContentText("Аудіогід активовано. Слідкування за маршрутом...") // ПОТРІБЕН R.string.notification_text
                .setSmallIcon(R.drawable.ic_bus) // ПОТРІБЕН R.drawable.ic_bus
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    // =======================================================
    // II. ЛОГІКА GPS ТА ТРИГЕРІВ
    // =======================================================

    /**
     * Обчислює відстань до станції (допоміжний метод).
     */
    private float calculateDistanceToStation(double lat, double lon, Station station) {
        Location userLoc = new Location("user");
        userLoc.setLatitude(lat);
        userLoc.setLongitude(lon);

        Location stationLoc = new Location("station");
        stationLoc.setLatitude(station.getLatitude());
        stationLoc.setLongitude(station.getLongitude());

        return userLoc.distanceTo(stationLoc);
    }

    /**
     * Знаходить індекс найближчої станції (яка буде наступною ціллю) при старті.
     * Якщо користувач знаходиться біля станції X, ціллю стає X+1.
     */
    private int findClosestStationIndex(double startLat, double startLon) {
        if (stations == null || stations.isEmpty()) return -1;

        float minDistance = Float.MAX_VALUE;
        int closestIndex = -1;

        for (int i = 0; i < stations.size(); i++) {
            Station station = stations.get(i);
            float distance = calculateDistanceToStation(startLat, startLon, station);

            // Знаходимо найближчу станцію
            if (distance < minDistance) {
                minDistance = distance;
                closestIndex = i;
            }
        }

        if (closestIndex == -1) return 0; // На всякий випадок, повертаємо першу

        // КОРИГУЮЧА ЛОГІКА:
        // Якщо ми знаходимось в радіусі GEOFENCE_RADIUS (20м) від найближчої станції (ClosestIndex)
        if (minDistance <= GEOFENCE_RADIUS) {
            // Ціль повинна бути НАСТУПНА станція (ClosestIndex + 1), якщо вона існує.
            if (closestIndex < stations.size() - 1) {
                return closestIndex + 1;
            } else {
                // Це кінцева станція, ми знаходимося на ній.
                // Ми залишаємо індекс кінцевої станції (closestIndex),
                // оскільки логіка onStartCommand та checkCurrentLocation обробляє це.
                return closestIndex;
            }
        } else {
            // Якщо ми далеко від найближчої станції, наша ціль - сама ClosestIndex
            return closestIndex;
        }
    }


    private void checkCurrentLocation(Location currentLocation) {
        // Перевірка кінця маршруту (Індекс вийшов за межі масиву)
        if (stations == null || stations.isEmpty() || currentStationIndex < 0 || currentStationIndex >= stations.size()) {
            if (stations != null && currentStationIndex >= stations.size()) {
                Log.i(TAG, "Кінець маршруту. Зупиняємо сервіс.");
                // Виклик методу для завершення роботи сервісу та оновлення UI
                handleRouteFinished();
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

            stopMusic(); // Зупиняємо фонову музику перед початком аудіогіда
            triggerAudioGuide(targetStation); // Запускаємо аудіогід

            // Перехід до наступної станції
            currentStationIndex++;

            // Оновлення UI: наступна станція (якщо не кінець)
            if (currentStationIndex < stations.size()) {
                Station nextStation = stations.get(currentStationIndex);
                sendUIUpdate(getString(nextStation.getNameResId()), false);
            } else {
                // Це був останній тригер
                handleRouteFinished();
            }

        } else {
            Log.d(TAG, String.format(Locale.getDefault(),
                    "NEXT: %s. Дистанція: %.1fм", getString(targetStation.getNameResId()), distanceToTarget));
        }
    }

    private void handleRouteFinished() {
        sendUIUpdate(getString(R.string.route_finished), true); // ПОТРІБЕН R.string.route_finished
        stopSelf();
    }


    // =======================================================
    // III. ЛОГІКА АУДІО (Відновлено логіку MediaPlayer)
    // =======================================================

    /**
     * Завантажує налаштування музики з SharedPreferences.
     */
    private void loadMusicSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        // За замовчуванням "Melody"
        musicGenre = prefs.getString(PREF_MUSIC_GENRE, "Melody");
    }

    /**
     * Запускає фонову музику, якщо обрано жанр.
     */
    private void startMusic() {
        if (musicGenre.equals("None")) {
            Log.d(TAG, "Фонова музика вимкнена в налаштуваннях.");
            return;
        }

        int musicResId;

        // ПОТРІБНІ R.raw.music_melody та R.raw.music_classical
        if (musicGenre.equals("Classical")) {
            musicResId = R.raw.music_classical;
        } else { // "Melody" або будь-який інший дефолт
            musicResId = R.raw.music_melody;
        }

        try {
            if (musicPlayer.isPlaying()) {
                musicPlayer.stop();
                musicPlayer.reset();
            }

            musicPlayer.release(); // Звільняємо старий ресурс перед створенням нового
            musicPlayer = MediaPlayer.create(this, musicResId);
            if (musicPlayer != null) {
                musicPlayer.setLooping(true);
                musicPlayer.start();
                Log.i(TAG, "Фонова музика (" + musicGenre + ") ЗАПУЩЕНА.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Помилка відтворення музики", e);
        }
    }

    /**
     * Зупиняє фонову музику.
     */
    private void stopMusic() {
        if (musicPlayer != null && musicPlayer.isPlaying()) {
            musicPlayer.pause(); // Краще пауза, якщо потрібно швидко відновити, або stop/reset
            musicPlayer.seekTo(0);
            Log.i(TAG, "Фонова музика ЗУПИНЕНА.");
        }
    }

    /**
     * Зупиняє та звільняє обидва плеєри.
     */
    private void stopAudioGuide() {
        if (guidePlayer != null) {
            if (guidePlayer.isPlaying()) guidePlayer.stop();
            guidePlayer.release();
        }
        if (musicPlayer != null) {
            if (musicPlayer.isPlaying()) musicPlayer.stop();
            musicPlayer.release();
        }
        // Переініціалізуємо для наступного запуску, якщо сервіс не зупинився
        guidePlayer = new MediaPlayer();
        guidePlayer.setOnCompletionListener(mp -> startMusic());
        musicPlayer = new MediaPlayer();
        musicPlayer.setLooping(true);
    }

    private void triggerAudioGuide(Station station) {
        stopMusic(); // Гарантуємо, що музика зупинена

        String audioKey = station.getAudioResKey();
        Log.i(TAG, "*** ТРИГЕР АУДІО! *** Запустити аудіогід для станції: " + getString(station.getNameResId()));

        // 1. Отримання ID ресурсу
        int audioResId = getResources().getIdentifier(audioKey, "raw", getPackageName());

        if (audioResId == 0) {
            Log.e(TAG, "Аудіофайл не знайдено для ключа: " + audioKey + ". Запускаємо музику.");
            startMusic();
            return;
        }

        try {
            // 2. Ініціалізація та відтворення
            if (guidePlayer != null) {
                if (guidePlayer.isPlaying()) guidePlayer.stop();
                guidePlayer.reset();

                // Встановлюємо слухач, який запустить музику після завершення гіда
                guidePlayer.setOnCompletionListener(mp -> {
                    Log.d(TAG, "Аудіогід завершено. Запускаємо фонову музику.");
                    startMusic();
                });

                guidePlayer = MediaPlayer.create(this, audioResId);
                if (guidePlayer != null) {
                    guidePlayer.start();
                    Log.i(TAG, "Аудіогід ЗАПУЩЕНО: " + audioKey);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Помилка відтворення аудіогіда", e);
            startMusic(); // Помилка відтворення -> запускаємо музику
        }
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

    @SuppressWarnings("MissingPermission")
    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Помилка: Дозволи на місцезнаходження відсутні в сервісі.");
            stopSelf();
            return;
        }

        // LocationRequest: оновлення кожні 5 секунд
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                // ВИПРАВЛЕНО: використання нового методу для встановлення мінімальної відстані
                .setMinUpdateDistanceMeters(10) // Оновлення, якщо пройшли 10 метрів
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        Log.d(TAG, "Запит на оновлення місцезнаходження запущено.");
    }

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
        stopAudioGuide(); // Звільнення плеєрів
        stopForeground(true);

        Log.d(TAG, "onDestroy: Сервіс успішно знищено.");
        super.onDestroy();
    }
}