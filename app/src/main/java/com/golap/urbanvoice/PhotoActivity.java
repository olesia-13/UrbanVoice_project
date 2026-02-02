package com.golap.urbanvoice;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

public class PhotoActivity extends AppCompatActivity {

    private TextView routeTitle;
    private LinearLayout photos_container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        routeTitle = findViewById(R.id.route_photo_title);
        photos_container = findViewById(R.id.photos_container);
        LinearLayout backButton = findViewById(R.id.back_button);

        backButton.setOnClickListener(v -> finish());




        String routeDisplayName = getIntent().getStringExtra("ROUTE_DISPLAY_NAME");
        String routeKey = getIntent().getStringExtra("ROUTE_KEY");

        routeTitle.setText(routeDisplayName != null ? routeDisplayName : "Фотографії маршруту");


        loadRoutePhotos(routeKey);
    }

    private void loadRoutePhotos(String routeKey) {
        if (routeKey == null) return;


        String[] photoNames = getPhotoResourcesForRoute(routeKey);

        for (String photoName : photoNames) {
            int resId = getResources().getIdentifier(photoName, "drawable", getPackageName());

            if (resId != 0) {
                addPhotoToContainer(resId);
            }
        }


        if (photos_container.getChildCount() == 0) {
            TextView noPhotosText = new TextView(this);
            noPhotosText.setText("Фотографії для цього маршруту ще не додані");
            noPhotosText.setTextSize(16);
            noPhotosText.setPadding(50, 100, 50, 100);
            photos_container.addView(noPhotosText);
        }
    }

    private void addPhotoToContainer(int imageResId) {
        ImageView imageView = new ImageView(this);


        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(30, 20, 30, 40);
        imageView.setLayoutParams(params);


        imageView.setImageResource(imageResId);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setAdjustViewBounds(true);


        imageView.setClipToOutline(true);

        photos_container.addView(imageView);
    }


    private String[] getPhotoResourcesForRoute(String routeKey) {
        switch (routeKey) {
            case "R001": // Трамвай №1
                return new String[]{
                        "photo_r001_1", "photo_r001_2", "photo_r001_3",
                        "photo_r001_4", "photo_r001_5", "photo_r001_6",
                        "photo_r001_7", "photo_r001_8", "photo_r001_9",
                        "photo_r001_10", "photo_r001_11", "photo_r001_12",
                        "photo_r001_13", "photo_r001_14", "photo_r001_15",
                        "photo_r001_16", "photo_r001_17", "photo_r001_18",
                        "photo_r001_19", "photo_r001_20", "photo_r001_21",
                        "photo_r001_22", "photo_r001_23", "photo_r001_24",
                        "photo_r001_25"
                };

            case "R111": // Тролейбус №111
                return new String[]{
                        "photo_r111_1", "photo_r111_2", "photo_r111_3",
                        "photo_r111_4", "photo_r111_5", "photo_r111_6",
                        "photo_r111_7", "photo_r111_8", "photo_r111_9",
                        "photo_r111_10", "photo_r111_11", "photo_r111_12",
                        "photo_r111_13", "photo_r111_14", "photo_r111_15",
                        "photo_r111_16", "photo_r111_17", "photo_r111_18",
                        "photo_r111_19"
                };

            case "R038": // Тролейбус №38
                return new String[]{
                        "photo_r038_1", "photo_r038_2", "photo_r038_3",
                        "photo_r038_4", "photo_r038_5", "photo_r038_6",
                        "photo_r038_7", "photo_r038_8", "photo_r038_9",
                        "photo_r038_10", "photo_r038_11", "photo_r038_12",
                        "photo_r038_13", "photo_r038_14", "photo_r038_15",
                        "photo_r038_16", "photo_r038_17", "photo_r038_18",
                        "photo_r038_19", "photo_r038_20", "photo_r038_21",
                        "photo_r038_22", "photo_r038_23", "photo_r038_24",
                };

            default:
                return new String[]{};
        }
    }
}