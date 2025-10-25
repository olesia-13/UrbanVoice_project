package com.golap.urbanvoice;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaPlayer;
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
    // Радіус тригера (20 метрів) - ПРИЙНЯТНЕ ЗНАЧЕННЯ ДЛЯ ПІШОГО/ГРОМАДСЬКОГО ТРАНСПОРТУ
    private static final float GEOFENCE_RADIUS = 20.0f;
    private static final String PREFS_NAME = "AppSettings";
    private static final String PREF_MUSIC_GENRE = "music_genre";

    // Action для локального широкомовлення (для RouteMap)
    public static final String ACTION_LOCATION_UPDATE = "com.golap.urbanvoice.LOCATION_UPDATE";
    // ВИДАЛЕНО: public static final String EXTRA_NEXT_STATION_NAME = "NEXT_STATION_NAME";
    public static final String EXTRA_ROUTE_FINISHED = "ROUTE_FINISHED";

    // Audio Players
    private MediaPlayer guidePlayer;
    private MediaPlayer musicPlayer;
    private String musicGenre; // "Melody", "Classical", "None"

    // GPS Клієнт та запити
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    // Дані маршруту
    private List<Station> stations;
    /* КРИТИЧНО: currentStationIndex ВКАЗУЄ НА ПОТОЧНУ АКТИВНУ СТАНЦІЮ
    (ту, від якої ми від'їжджаємо або на якій ми зараз знаходимося).
    На UI відображається назва станції за цим індексом.
    */
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
        musicPlayer.setLooping(true);

        // Встановлення слухача для гіда (перенесено до triggerAudioGuide для коректного reset)
        // guidePlayer.setOnCompletionListener(mp -> startMusic());

        // Налаштування LocationCallback для обробки нових координат
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                Location lastLocation = locationResult.getLastLocation();
                if (lastLocation != null) {
                    // !!! checkCurrentLocation тепер перевіряє НАСТУПНУ ціль і автоматично перемикає індекс !!!
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
        double startLat = intent.getDoubleExtra("START_LAT", 0);
        double startLon = intent.getDoubleExtra("START_LON", 0);

        // 1. Зчитування налаштувань музики
        loadMusicSettings();

        // 2. Завантаження даних маршруту
        RouteData routeData = MapDataManager.getRouteData(routeKey);

        if (routeData != null) {
            currentDirection = directionFromIntent;
            // ВИКОРИСТОВУЄМО ОДНОРАЗОВО ВИЗНАЧЕНИЙ НАПРЯМОК
            stations = MapDataManager.getStationsForDirection(routeKey, currentDirection);

            if (stations != null && !stations.isEmpty()) {
                // !!! ВИЗНАЧАЄМО СТАРТОВУ АКТИВНУ СТАНЦІЮ НА ОСНОВІ ПОЗИЦІЇ !!!
                // Ця логіка тепер коректно встановлює індекс
                currentStationIndex = findClosestStationIndex(startLat, startLon);

                // *** ВИПРАВЛЕННЯ: Додаткова перевірка завершення маршруту при старті ***
                if (currentStationIndex == stations.size() - 1) {
                    float distToLast = calculateDistanceToStation(startLat, startLon, stations.get(currentStationIndex));
                    if (distToLast < GEOFENCE_RADIUS) {
                        Log.i(TAG, "Користувач стартував на кінцевій станції. Завершуємо маршрут.");
                        // ЗМІНЕНО: Видалено аргумент назви станції
                        sendUIUpdate(true);
                        stopSelf();
                        return START_STICKY;
                    }
                }
                // *** Кінець виправлення ***

                // Забезпечуємо, що індекс не від'ємний
                if (currentStationIndex < 0) currentStationIndex = 0;
            }
        }

        Log.d(TAG, "onStartCommand: Запущено маршрут: " + routeKey + ", напрямок: " + currentDirection + ". Початковий активний індекс: " + currentStationIndex);

        // 3. Створення та запуск Foreground-режиму
        startForeground(NOTIFICATION_ID, buildNotification());

        // 4. Запит на оновлення місцезнаходження
        requestLocationUpdates();

        // 5. Оновлення UI: Всі виклики, які посилали назву станції, ВИДАЛЕНО або ЗМІНЕНО
        // Видалення: if (stations != null && !stations.isEmpty() && currentStationIndex < stations.size()) {
        //                updateUIForCurrentStation();
        //            } else {
        //                sendUIUpdate(getString(R.string.route_loading_error), false);
        //            }

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
                    "Аудіогід UrbanVoice",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // Інтент для повернення до RouteMap
        Intent notificationIntent = new Intent(this, RouteMap.class);
        // Додаємо FLAG_IMMUTABLE
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // Створення сповіщення
        // ПОТРІБЕН R.drawable.ic_bus
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Аудіогід активовано. Слідкування за маршрутом...")
                .setSmallIcon(R.drawable.ic_bus)
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
     * ЛОГІКА ІНІЦІАЛІЗАЦІЇ: Знаходить індекс ПОЧАТКОВОЇ АКТИВНОЇ СТАНЦІЇ.
     * @return Індекс станції.
     */
    private int findClosestStationIndex(double startLat, double startLon) {
        if (stations == null || stations.isEmpty()) return 0; // Повертаємо 0, якщо список порожній

        float minDistance = Float.MAX_VALUE;
        int closestIndex = 0;

        // 1. Знаходимо АБСОЛЮТНО найближчу станцію
        for (int i = 0; i < stations.size(); i++) {
            float distance = calculateDistanceToStation(startLat, startLon, stations.get(i));

            if (distance < minDistance) {
                minDistance = distance;
                closestIndex = i;
            }
        }

        // 2. Логіка визначення ПОЧАТКОВОЇ АКТИВНОЇ СТАНЦІЇ

        // A. Якщо ми знаходимося ВЖЕ НА СТАНЦІЇ (в межах GEOFENCE_RADIUS):
        if (minDistance <= GEOFENCE_RADIUS) {
            // Ми на станції 'X'. Вона і є нашою поточною активною станцією.
            Log.d(TAG, "findClosestStation: В межах радіуса станції. Активна: " + closestIndex);
            return closestIndex;
        }

        // B. Якщо ми знаходимося МІЖ станціями (далеко від найближчої):
        if (closestIndex > 0) {
            // Ми далеко від ClosestIndex (наприклад, C), отже, ми рухаємося ДО неї.
            // Поточною АКТИВНОЮ (пройденою) має бути ClosestIndex - 1 (наприклад, B).
            Log.d(TAG, "findClosestStation: Між станціями. Активна станція " + (closestIndex - 1));
            return closestIndex - 1;
        } else {
            // Якщо найближча станція 0 (перша), і ми від неї далеко.
            // Поточна активна станція = 0.
            Log.d(TAG, "findClosestStation: Близько до початку. Активна станція 0.");
            return 0;
        }
    }


    /**
     * КРИТИЧНА ЗМІНА: Перевіряє розташування відносно НАСТУПНОЇ ЦІЛІ (currentStationIndex + 1).
     * Якщо досягнуто ціль, currentStationIndex збільшується, і UI оновлюється.
     */
    private void checkCurrentLocation(Location currentLocation) {
        if (stations == null || stations.isEmpty() || currentStationIndex < 0) {
            handleRouteFinished();
            return;
        }

        // Цільовий індекс - це наступна станція
        int targetIndex = currentStationIndex + 1;

        // 1. Перевірка завершення маршруту
        if (targetIndex >= stations.size()) {
            // Ми вже на останній активній станції. Маршрут по суті завершено.
            handleRouteFinished();
            return;
        }

        // 2. Отримання наступної цільової станції
        Station targetStation = stations.get(targetIndex);

        // Обчислення відстані
        Location targetLocation = new Location("target");
        targetLocation.setLatitude(targetStation.getLatitude());
        targetLocation.setLongitude(targetStation.getLongitude());

        float distanceToTarget = currentLocation.distanceTo(targetLocation);

        // 3. Тригер: Якщо ми в радіусі GEOFENCE_RADIUS
        if (distanceToTarget <= GEOFENCE_RADIUS) {
            Log.d(TAG, String.format(Locale.getDefault(),
                    "ДОСЯГНУТО ЦІЛЬ: Станція %s. Дистанція: %.1fм", getString(targetStation.getNameResId()), distanceToTarget));

            stopMusic();
            // !!! ЗАПУСКАЄМО АУДІОГІД ДЛЯ ЦІЄЇ НОВОЇ СТАНЦІЇ !!!
            triggerAudioGuide(targetStation);

            // КРОК ВПЕРЕД: currentStationIndex стає новою активною станцією
            currentStationIndex = targetIndex;

            // ***** ВИДАЛЕНО: АВТОМАТИЧНЕ ОНОВЛЕННЯ СТАНЦІЇ В UI *****
            // Видалено: updateUIForCurrentStation();
            // *******************************************************

        } else {
            // Логування відстані
            Log.d(TAG, String.format(Locale.getDefault(),
                    "ACTIVE: %s -> NEXT TARGET: %s. Дистанція: %.1fм",
                    getString(stations.get(currentStationIndex).getNameResId()),
                    getString(targetStation.getNameResId()), distanceToTarget));
        }
    }

    /**
     * ЗМІНЕНО: Цей метод тепер не потрібен для UI, але ми зберігаємо його для логічної структури
     * та перевірки на завершення маршруту.
     */
    private void updateUIForCurrentStation() {
        // ЛОГІКА ОНОВЛЕННЯ UI (відправки назви станції) ПОВНІСТЮ ВИДАЛЕНА
        if (stations == null || currentStationIndex < 0) return;

        if (currentStationIndex >= stations.size()) {
            // Якщо індекс вийшов за межі
            handleRouteFinished();
        }
    }


    private void handleRouteFinished() {
        // ПОТРІБЕН R.string.route_finished
        // ЗМІНЕНО: Видалено аргумент назви станції
        sendUIUpdate(true);
        stopSelf();
    }


    // =======================================================
    // III. ЛОГІКА АУДІО
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
            // ПОТРІБНА КОРЕКТНА ІНТЕГРАЦІЯ З MusicManager, якщо ви його використовуєте
            // Наразі залишаємо оригінальну логіку з R.raw
            musicResId = R.raw.music_classical;
        } else { // "Melody" або будь-який інший дефолт
            musicResId = R.raw.music_melody;
        }

        try {
            // *** ВИПРАВЛЕННЯ: Звільнення ресурсів перед створенням нового ***
            if (musicPlayer != null) {
                if (musicPlayer.isPlaying()) musicPlayer.stop();
                musicPlayer.reset();
                musicPlayer.release();
            }
            // Переініціалізація
            musicPlayer = new MediaPlayer();

            // Створення нового екземпляра
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
            musicPlayer.pause();
            // Немає потреби seekTo(0), якщо ми її потім відновимо або reset
            Log.i(TAG, "Фонова музика ЗУПИНЕНА.");
        }
    }

    /**
     * Зупиняє та звільняє обидва плеєри. (Викликається в onDestroy)
     */
    private void releasePlayers() {
        if (guidePlayer != null) {
            if (guidePlayer.isPlaying()) guidePlayer.stop();
            guidePlayer.release();
            guidePlayer = null;
        }
        if (musicPlayer != null) {
            if (musicPlayer.isPlaying()) musicPlayer.stop();
            musicPlayer.release();
            musicPlayer = null;
        }
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
                guidePlayer.release(); // Звільняємо старий плеєр
            }

            guidePlayer = MediaPlayer.create(this, audioResId);

            if (guidePlayer != null) {
                // Встановлюємо слухач, який запустить музику після завершення гіда
                guidePlayer.setOnCompletionListener(mp -> {
                    Log.d(TAG, "Аудіогід завершено. Запускаємо фонову музику.");
                    startMusic();
                });

                guidePlayer.start();
                Log.i(TAG, "Аудіогід ЗАПУЩЕНО: " + audioKey);
            }
        } catch (Exception e) {
            Log.e(TAG, "Помилка відтворення аудіогіда", e);
            startMusic(); // Помилка відтворення -> запускаємо музику
        }
    }

    // =======================================================
    // IV. ОНОВЛЕННЯ UI (LocalBroadcast)
    // =======================================================

    /**
     * ЗМІНЕНО: Тепер надсилається лише статус завершення маршруту, без назви станції.
     */
    private void sendUIUpdate(boolean isFinished) {
        Intent intent = new Intent(ACTION_LOCATION_UPDATE);
        // ВИДАЛЕНО: intent.putExtra(EXTRA_NEXT_STATION_NAME, nextStationName);
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

        // LocationRequest: оновлення кожні 5 секунд, мінімальна відстань 10 метрів
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateDistanceMeters(10)
                .build();

        // Використовуємо Looper.getMainLooper()
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
        releasePlayers(); // Звільнення плеєрів
        stopForeground(true);

        Log.d(TAG, "onDestroy: Сервіс успішно знищено.");
        super.onDestroy();
    }
}