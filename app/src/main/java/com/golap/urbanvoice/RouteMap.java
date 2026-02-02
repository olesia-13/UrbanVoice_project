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

    private static final String TAG = "RouteMap";


    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean isAudioGuideRunning = false;
    private ActivityResultLauncher<String> requestPermissionLauncher;


    private static final long LOCATION_REQUEST_INTERVAL = 1000;
    private static final int MAX_DIRECTION_UPDATES = 3;
    private LocationCallback directionCheckLocationCallback;
    private Location lastValidLocation;
    private boolean isCheckingDirection = false;
    private int updateCount = 0;



    private TextView routeTitle;

    private ImageButton startAudioButton;
    private ImageView routeIcon;
    private ImageButton textButton;
    private ImageButton photoButton;


    private String routeKey;
    private String routeDisplayName;
    private RouteData currentRouteData;

    private String currentDirection = null;


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


        setupPermissionLauncher();


        Intent intent = getIntent();
        String fullRouteKey = intent.getStringExtra("ROUTE_KEY");


        routeKey = fullRouteKey;

        if (routeKey != null && (routeKey.endsWith("_A") || routeKey.endsWith("_B"))) {
            routeKey = routeKey.substring(0, routeKey.length() - 2);
        }

        routeDisplayName = intent.getStringExtra("ROUTE_DISPLAY_NAME");
        int iconId = intent.getIntExtra("ROUTE_ICON_ID", R.drawable.ic_bus);


        currentRouteData = MapDataManager.getRouteData(routeKey);


        routeTitle = findViewById(R.id.route_map_title);
        startAudioButton = findViewById(R.id.start_audio_button);
        routeIcon = findViewById(R.id.route_map_icon);
        textButton = findViewById(R.id.text_button);
        photoButton = findViewById(R.id.photo_button);

        routeTitle.setText(routeDisplayName != null ? routeDisplayName : "Маршрут");
        routeIcon.setImageResource(iconId);



        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        findViewById(R.id.home_button).setOnClickListener(v -> {
            Intent homeIntent = new Intent(this, MainActivity.class);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(homeIntent);
        });


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        startAudioButton.setOnClickListener(v -> toggleAudioGuide());
        updateButtonUI(false);


        setupLocationUpdateReceiver();


        textButton.setOnClickListener(v -> showFullTextGuide());
        photoButton.setOnClickListener(v -> showPhotos());
    }



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



    private void setupLocationUpdateReceiver() {
        locationUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (LocationAudioService.ACTION_LOCATION_UPDATE.equals(intent.getAction())) {
                    boolean isFinished = intent.getBooleanExtra(LocationAudioService.EXTRA_ROUTE_FINISHED, false);



                    if (isFinished) {
                        stopAudioGuide(true);
                        Toast.makeText(context, getString(R.string.route_finished), Toast.LENGTH_LONG).show();
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



    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        if (currentRouteData != null) {

            String encodedPolyline = currentRouteData.getPolylineEncoded();
            drawRouteOnMap(encodedPolyline);

            placeAllRouteMarkers();


            List<Station> stations = currentRouteData.getForwardStations();

            if (!stations.isEmpty()) {
                Station firstStation = stations.get(0);
                LatLng startPoint = new LatLng(firstStation.getLatitude(), firstStation.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPoint, 12f));
            }
        } else {

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
                    .width(10)
                    .color(ContextCompat.getColor(this, R.color.route_line_color));

            mMap.addPolyline(polylineOptions);

        } catch (Exception e) {
            Toast.makeText(this, "Помилка декодування маршруту", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }


    private void placeAllRouteMarkers() {
        if (mMap == null || currentRouteData == null) return;


        Set<String> placedStationLocations = new HashSet<>();


        placeMarkersFromList(currentRouteData.getForwardStations(), placedStationLocations);


        placeMarkersFromList(currentRouteData.getBackwardStations(), placedStationLocations);
    }


    private void placeMarkersFromList(List<Station> stations, Set<String> placedStationLocations) {
        BitmapDescriptor stationIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_station_mark);

        for (Station station : stations) {
            String locationId = String.format(Locale.US, "%.6f,%.6f", station.getLatitude(), station.getLongitude());


            if (!placedStationLocations.contains(locationId)) {
                LatLng position = new LatLng(station.getLatitude(), station.getLongitude());

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(position)
                        .title(getString(station.getNameResId()))
                        .icon(stationIcon);

                mMap.addMarker(markerOptions);
                placedStationLocations.add(locationId);
            }
        }
    }

    @SuppressWarnings("MissingPermission")
    private void enableUserLocationLayer() {
        if (checkLocationPermission() && mMap != null) {
            mMap.setMyLocationEnabled(true);
        }
    }



    private void setupPermissionLauncher() {
        requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        enableUserLocationLayer();
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


    private void toggleAudioGuide() {
        if (isAudioGuideRunning) {
            stopAudioGuide(false);
        } else {
            if (currentRouteData == null) {
                Toast.makeText(this, "Помилка: Не знайдено даних маршруту.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (checkLocationPermission()) {
                determineRouteDirectionAndStartGuide();
            } else {
                requestLocationPermission();
            }
        }
    }


    @SuppressWarnings("MissingPermission")
    private void determineRouteDirectionAndStartGuide() {
        if (!checkLocationPermission()) {
            Toast.makeText(this, "Потрібен дозвіл на місцезнаходження.", Toast.LENGTH_SHORT).show();
            return;
        }


        startDirectionCheck();
    }


    @SuppressWarnings("MissingPermission")
    private void startDirectionCheck() {
        if (isCheckingDirection) return;
        isCheckingDirection = true;
        updateCount = 0;
        lastValidLocation = null;

        Toast.makeText(this, "Визначаємо напрямок руху (3 сек.)...", Toast.LENGTH_SHORT).show();


        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, LOCATION_REQUEST_INTERVAL)
                .setMinUpdateDistanceMeters(1)
                .setMaxUpdates(MAX_DIRECTION_UPDATES)
                .build();


        directionCheckLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location currentLocation = locationResult.getLastLocation();


                if (currentLocation == null) {
                    updateCount++;
                    if (updateCount >= MAX_DIRECTION_UPDATES) {
                        handleDirectionFailure(null, "Не вдалося отримати GPS-координати.");
                    }
                    return;
                }


                boolean movementDetected = false;

                if (lastValidLocation != null) {
                    if (currentLocation.distanceTo(lastValidLocation) >= 2.0f) {
                        movementDetected = true;
                    }
                }


                lastValidLocation = currentLocation;
                updateCount++;


                if (!movementDetected && updateCount < MAX_DIRECTION_UPDATES) {
                    return;
                }


                float bearing = 0.0f;


                if (currentLocation.hasBearing() && currentLocation.getSpeed() > 0.5f) {
                    bearing = currentLocation.getBearing();
                }

                else if (movementDetected) {

                    if (lastValidLocation != null) {
                        bearing = lastValidLocation.bearingTo(currentLocation);
                    }
                }


                LatLng userLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

                String determinedDirection = MapDataManager.determineOptimalDirectionWithBearing(
                        routeKey,
                        userLocation,
                        bearing
                );


                if (determinedDirection != null) {
                    currentDirection = determinedDirection;
                    startAudioGuideService(determinedDirection, currentLocation);
                } else {
                    handleDirectionFailure(currentLocation, "Напрямок не визначено або ви далеко від маршруту. Спробуйте почати рух.");
                }

                stopDirectionCheck();
            }
        };


        fusedLocationClient.requestLocationUpdates(locationRequest, directionCheckLocationCallback, Looper.getMainLooper());
    }

    private void handleDirectionFailure(Location location, String message) {
        Toast.makeText(RouteMap.this, message, Toast.LENGTH_LONG).show();
        stopDirectionCheck();

        updateButtonUI(false);
    }


    private void stopDirectionCheck() {
        if (isCheckingDirection) {
            fusedLocationClient.removeLocationUpdates(directionCheckLocationCallback);
            lastValidLocation = null;
            isCheckingDirection = false;
            updateCount = 0;
        }
    }



    private void startAudioGuideService(String direction, Location location) {
        Log.d(TAG, "Starting service with Direction: " + direction + " for RouteKey: " + routeKey);

        Intent serviceIntent = new Intent(this, LocationAudioService.class);
        serviceIntent.putExtra("ROUTE_KEY", routeKey);
        serviceIntent.putExtra("DIRECTION", direction);
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


    private void stopAudioGuide(boolean isRouteFinished) {
        Intent serviceIntent = new Intent(this, LocationAudioService.class);
        stopService(serviceIntent);

        isAudioGuideRunning = false;
        updateButtonUI(false);
        Toast.makeText(this, "Аудіогід ЗУПИНЕНО.", Toast.LENGTH_SHORT).show();

        if (isRouteFinished) {
            currentDirection = null;
            Log.d(TAG, "Route finished. Direction reset to null.");
        } else {

            Log.d(TAG, "Audio guide stopped manually. Direction remains: " + currentDirection);
        }
    }


    private void stopAudioGuide() {
        stopAudioGuide(false);
    }

    private void updateButtonUI(boolean isRunning) {

        if (isRunning) {
            startAudioButton.setImageResource(R.drawable.ic_stop);
        } else {
            startAudioButton.setImageResource(R.drawable.ic_start);
        }
    }



    private void showFullTextGuide() {

        if (currentRouteData == null || currentDirection == null) {

            Toast.makeText(this, "Спочатку запустіть аудіогід для визначення напрямку.", Toast.LENGTH_LONG).show();
            return;
        }


        int textResId = MapDataManager.getTextResIdForDirection(routeKey, currentDirection);

        Log.d(TAG, "Showing text for saved Direction: " + currentDirection + " with ResId: " + textResId);

        if (textResId != 0) {

            Intent textIntent = new Intent(this, TextActivity.class);
            textIntent.putExtra("ROUTE_DISPLAY_NAME", routeDisplayName);
            textIntent.putExtra("TEXT_RES_ID", textResId);

            textIntent.putExtra("ROUTE_KEY", routeKey);

            textIntent.putExtra("DIRECTION", currentDirection);
            startActivity(textIntent);
        } else {
            Toast.makeText(this, "Текстовий гід не знайдено для напрямку " + currentDirection, Toast.LENGTH_SHORT).show();
        }
    }

    private void showPhotos() {
        Intent photoIntent = new Intent(this, PhotoActivity.class);
        photoIntent.putExtra("ROUTE_DISPLAY_NAME", routeDisplayName);
        photoIntent.putExtra("ROUTE_KEY", routeKey);
        startActivity(photoIntent);
    }
}