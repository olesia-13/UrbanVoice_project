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


public class LocationAudioService extends Service {

    private static final String TAG = "AudioService";
    private static final String CHANNEL_ID = "AudioGuideChannel";
    private static final int NOTIFICATION_ID = 101;
    private static final float GEOFENCE_RADIUS = 20.0f;
    private static final String PREFS_NAME = "UrbanVoiceSettings";
    private static final String PREF_MUSIC_GENRE = "music_genre";

    public static final String ACTION_LOCATION_UPDATE = "com.golap.urbanvoice.LOCATION_UPDATE";
    public static final String EXTRA_ROUTE_FINISHED = "ROUTE_FINISHED";

    private MediaPlayer guidePlayer;
    private MediaPlayer musicPlayer;
    private String musicGenre;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private List<Station> stations;
    private int currentStationIndex = -1;
    private String routeKey;
    private String currentDirection;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: Сервіс створено.");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        guidePlayer = null;
        musicPlayer = null;

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
        double startLat = intent.getDoubleExtra("START_LAT", 0);
        double startLon = intent.getDoubleExtra("START_LON", 0);

        loadMusicSettings();

        RouteData routeData = MapDataManager.getRouteData(routeKey);

        if (routeData != null) {
            currentDirection = directionFromIntent;
            stations = MapDataManager.getStationsForDirection(routeKey, currentDirection);

            if (stations != null && !stations.isEmpty()) {
                currentStationIndex = findClosestStationIndex(startLat, startLon);

                if (currentStationIndex == stations.size() - 1) {
                    float distToLast = calculateDistanceToStation(startLat, startLon, stations.get(currentStationIndex));
                    if (distToLast < GEOFENCE_RADIUS) {
                        Log.i(TAG, "Користувач стартував на кінцевій станції. Завершуємо маршрут.");
                        sendUIUpdate(true);
                        stopSelf();
                        return START_STICKY;
                    }
                }
                if (currentStationIndex < 0) currentStationIndex = 0;
            }
        }

        Log.d(TAG, "onStartCommand: Запущено маршрут: " + routeKey + ", напрямок: " + currentDirection + ". Початковий активний індекс: " + currentStationIndex);

        startForeground(NOTIFICATION_ID, buildNotification());
        requestLocationUpdates();


        startMusic();

        return START_STICKY;
    }



    private Notification buildNotification() {

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


        Intent notificationIntent = new Intent(this, RouteMap.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);


        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Аудіогід активовано. Слідкування за маршрутом...")
                .setSmallIcon(R.drawable.ic_bus)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }


    private float calculateDistanceToStation(double lat, double lon, Station station) {
        Location userLoc = new Location("user");
        userLoc.setLatitude(lat);
        userLoc.setLongitude(lon);

        Location stationLoc = new Location("station");
        stationLoc.setLatitude(station.getLatitude());
        stationLoc.setLongitude(station.getLongitude());

        return userLoc.distanceTo(stationLoc);
    }


    private int findClosestStationIndex(double startLat, double startLon) {
        if (stations == null || stations.isEmpty()) return 0; // Повертаємо 0, якщо список порожній

        float minDistance = Float.MAX_VALUE;
        int closestIndex = 0;


        for (int i = 0; i < stations.size(); i++) {
            float distance = calculateDistanceToStation(startLat, startLon, stations.get(i));

            if (distance < minDistance) {
                minDistance = distance;
                closestIndex = i;
            }
        }


        if (minDistance <= GEOFENCE_RADIUS) {

            Log.d(TAG, "findClosestStation: В межах радіуса станції. Активна: " + closestIndex);
            return closestIndex;
        }


        if (closestIndex > 0) {

            Log.d(TAG, "findClosestStation: Між станціями. Активна станція " + (closestIndex - 1));
            return closestIndex - 1;
        } else {

            Log.d(TAG, "findClosestStation: Близько до початку. Активна станція 0.");
            return 0;
        }
    }



    private void checkCurrentLocation(Location currentLocation) {
        if (stations == null || stations.isEmpty() || currentStationIndex < 0) {
            handleRouteFinished();
            return;
        }

        int targetIndex = currentStationIndex + 1;

        if (targetIndex >= stations.size()) {
            handleRouteFinished();
            return;
        }

        Station targetStation = stations.get(targetIndex);

        Location targetLocation = new Location("target");
        targetLocation.setLatitude(targetStation.getLatitude());
        targetLocation.setLongitude(targetStation.getLongitude());

        float distanceToTarget = currentLocation.distanceTo(targetLocation);

        if (distanceToTarget <= GEOFENCE_RADIUS) {
            Log.d(TAG, String.format(Locale.getDefault(),
                    "ДОСЯГНУТО ЦІЛЬ: Станція %s. Дистанція: %.1fм", getString(targetStation.getNameResId()), distanceToTarget));

            stopMusic();
            triggerAudioGuide(targetStation);

            currentStationIndex = targetIndex;

        } else {
            Log.d(TAG, String.format(Locale.getDefault(),
                    "ACTIVE: %s -> NEXT TARGET: %s. Дистанція: %.1fм",
                    getString(stations.get(currentStationIndex).getNameResId()),
                    getString(targetStation.getNameResId()), distanceToTarget));
        }
    }


    private void handleRouteFinished() {
        sendUIUpdate(true);
        stopSelf();
    }



    private void loadMusicSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);


        int savedRadioId = prefs.getInt(PREF_MUSIC_GENRE, R.id.radioMelody);


        musicGenre = getMusicGenreFromId(savedRadioId);

        Log.d(TAG, "Налаштування музики завантажено: " + musicGenre + ", (ID: " + savedRadioId + ")");
    }

    private String getMusicGenreFromId(int radioId) {
        if (radioId == R.id.radioClassical) {
            return MusicManager.GENRE_CLASSICAL;
        } else if (radioId == R.id.radioNone) {
            return MusicManager.GENRE_NONE;
        } else {
            return MusicManager.GENRE_MELODY;
        }
    }


    private void startMusic() {

        if (musicGenre.equals(MusicManager.GENRE_NONE)) {
            Log.d(TAG, "Фонова музика вимкнена в налаштуваннях.");
            return;
        }

        try {

            int musicResId = MusicManager.getRandomAudioResId(this, musicGenre);

            if (musicResId == 0) {

                Log.e(TAG, "Не знайдено музичних файлів для жанру: " + musicGenre);
                return;
            }


            if (musicPlayer != null) {
                if (musicPlayer.isPlaying()) {
                    musicPlayer.stop();
                }
                musicPlayer.release();
                musicPlayer = null;
            }


            musicPlayer = MediaPlayer.create(this, musicResId);

            if (musicPlayer != null) {
                musicPlayer.setLooping(true);
                musicPlayer.start();
                Log.i(TAG, "Фонова музика (" + musicGenre + ") ЗАПУЩЕНА. Ресурс: " + getResources().getResourceEntryName(musicResId));
            }
        } catch (Exception e) {
            Log.e(TAG, "Помилка відтворення музики", e);
        }
    }


    private void stopMusic() {
        if (musicPlayer != null && musicPlayer.isPlaying()) {
            musicPlayer.pause();
            Log.i(TAG, "Фонова музика ЗУПИНЕНА.");
        }
    }


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
        stopMusic();

        String audioKey = station.getAudioResKey();
        Log.i(TAG, "Запустити аудіогід для станції: " + getString(station.getNameResId()) + " з ключем: " + audioKey); // Додано логування ключа

        int audioResId = MusicManager.getSpecificAudioResId(this, audioKey);

        if (audioResId == 0) {
            Log.e(TAG, "Аудіофайл не знайдено для ключа: " + audioKey + ". Запускаємо музику.");
            startMusic();
            return;
        }

        try {

            if (guidePlayer != null) {
                if (guidePlayer.isPlaying()) guidePlayer.stop();
                guidePlayer.release();
                guidePlayer = null;
            }


            guidePlayer = MediaPlayer.create(this, audioResId);

            if (guidePlayer != null) {
                guidePlayer.setOnCompletionListener(mp -> {
                    Log.d(TAG, "Аудіогід завершено. Запускаємо фонову музику.");
                    startMusic();
                });

                guidePlayer.start();
                Log.i(TAG, "Аудіогід ЗАПУЩЕНО: " + getResources().getResourceEntryName(audioResId));
            }
        } catch (Exception e) {
            Log.e(TAG, "Помилка відтворення аудіогіда", e);
            startMusic();
        }
    }


    private void sendUIUpdate(boolean isFinished) {
        Intent intent = new Intent(ACTION_LOCATION_UPDATE);

        intent.putExtra(EXTRA_ROUTE_FINISHED, isFinished);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }



    @SuppressWarnings("MissingPermission")
    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Помилка: Дозволи на місцезнаходження відсутні в сервісі.");
            stopSelf();
            return;
        }


        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateDistanceMeters(10)
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
        releasePlayers();
        stopForeground(true);

        Log.d(TAG, "onDestroy: Сервіс успішно знищено.");
        super.onDestroy();
    }
}