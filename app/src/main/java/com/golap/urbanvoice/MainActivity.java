package com.golap.urbanvoice;

import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "UrbanVoiceSettings";
    private static final String PREF_LANGUAGE_KEY = "selected_language";
    private static final String PREF_MUSIC_GENRE_KEY = "music_genre_id";
    private DrawerLayout drawerLayout;
    private ImageButton btnSettings;
    private ImageButton btnCloseDrawer;
    private Button btnStartJourney;
    private RadioGroup musicGenreGroup;
    private ImageView langEn;
    private ImageView langUa;
    private TextView tvHowAppWorks;

    private String selectedLanguage = "ua";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        drawerLayout = findViewById(R.id.main);
        btnSettings = findViewById(R.id.btnSettings);
        btnCloseDrawer = findViewById(R.id.btnCloseDrawer);
        btnStartJourney = findViewById(R.id.btnStartJourney);
        musicGenreGroup = findViewById(R.id.musicGenreGroup);
        langEn = findViewById(R.id.lang_en);
        langUa = findViewById(R.id.lang_ua);
        tvHowAppWorks = findViewById(R.id.tvHowAppWorks);

        loadSettings();


        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.END);
            }
        });


        btnCloseDrawer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.closeDrawer(GravityCompat.END);
            }
        });


        btnStartJourney.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Intent intent = new Intent(MainActivity.this, NextActivity.class);
                // startActivity(intent);
            }
        });


        tvHowAppWorks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.closeDrawer(GravityCompat.END);
                // Intent intent = new Intent(MainActivity.this, InstructionsActivity.class);
                // startActivity(intent);
            }
        });


        View.OnClickListener langClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.lang_en) {
                    selectedLanguage = "en";
                } else if (v.getId() == R.id.lang_ua) {
                    selectedLanguage = "ua";
                }

                updateLanguageUI();
                saveSettings();
                Toast.makeText(MainActivity.this, "Мова збережена: " + selectedLanguage, Toast.LENGTH_SHORT).show();
            }
        };

        langEn.setOnClickListener(langClickListener);
        langUa.setOnClickListener(langClickListener);



        musicGenreGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                saveSettings();
                RadioButton selectedRadio = findViewById(checkedId);
                if (selectedRadio != null) {
                    Toast.makeText(MainActivity.this, "Жанр збережено: " + selectedRadio.getText(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END);
                } else {
                    setEnabled(false);
                    MainActivity.super.onBackPressed();
                    setEnabled(true);
                }
            }
        });


    }

    private void loadSettings() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        selectedLanguage = settings.getString(PREF_LANGUAGE_KEY, "ua");
        updateLanguageUI();

        int savedRadioId = settings.getInt(PREF_MUSIC_GENRE_KEY, R.id.radioMelody);
        musicGenreGroup.check(savedRadioId);
    }

    private void saveSettings() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        editor.putInt(PREF_MUSIC_GENRE_KEY, musicGenreGroup.getCheckedRadioButtonId());

        editor.putString(PREF_LANGUAGE_KEY, selectedLanguage);

        editor.apply();
    }

    private void updateLanguageUI() {
        final float SELECTED_ALPHA = 1.0f;
        final float UNSELECTED_ALPHA = 0.4f;


        langUa.setAlpha(selectedLanguage.equals("ua") ? SELECTED_ALPHA : UNSELECTED_ALPHA);
        langEn.setAlpha(selectedLanguage.equals("en") ? SELECTED_ALPHA : UNSELECTED_ALPHA);
    }





}