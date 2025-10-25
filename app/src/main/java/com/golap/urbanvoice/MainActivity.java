package com.golap.urbanvoice;

import android.content.res.Configuration;
import android.content.res.Resources;
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

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "UrbanVoiceSettings";
    private static final String PREF_LANGUAGE_KEY = "selected_language";
    private static final String PREF_MUSIC_GENRE_KEY = "music_genre"; // Уніфікуємо ключ із сервісом!
    private DrawerLayout drawerLayout;
    private ImageButton btnSettings;
    private ImageButton btnCloseDrawer;
    private Button btnStartJourney;
    private RadioGroup musicGenreGroup;
    private ImageView langEn;
    private ImageView langUa;
    private TextView tvHowAppWorks;
    private TextView myroutes;
    private TextView myexit;

    private String selectedLanguage = "ua";

    @Override
    protected void attachBaseContext(Context newBase) {

        SharedPreferences settings = newBase.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String lang = settings.getString(PREF_LANGUAGE_KEY, "ua");


        Context context = setLocale(newBase, lang);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(android.graphics.Color.parseColor("#FFFAE6"));
        getWindow().setNavigationBarColor(android.graphics.Color.parseColor("#FFFAE6"));

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR |
                            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            );
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }

        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.main);
        btnSettings = findViewById(R.id.btnSettings);
        btnCloseDrawer = findViewById(R.id.btnCloseDrawer);
        btnStartJourney = findViewById(R.id.btnStartJourney);
        musicGenreGroup = findViewById(R.id.musicGenreGroup);
        langEn = findViewById(R.id.lang_en);
        langUa = findViewById(R.id.lang_ua);
        tvHowAppWorks = findViewById(R.id.tvHowAppWorks);
        myroutes = findViewById(R.id.myroutes);
        myexit = findViewById(R.id.myexit);

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
                Intent intent = new Intent(MainActivity.this, SelectRoute.class);
                startActivity(intent);
            }
        });


        tvHowAppWorks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.closeDrawer(GravityCompat.END);
                Intent intent = new Intent(MainActivity.this, HowWorks.class);
                startActivity(intent);
            }
        });

        myexit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
                startActivity(intent);
            }
        });

        myroutes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, YourRouteActivity.class);
                startActivity(intent);
            }
        });


        View.OnClickListener langClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newLanguage;
                if (v.getId() == R.id.lang_en) {
                    newLanguage = "en";
                } else if (v.getId() == R.id.lang_ua) {
                    newLanguage = "ua";
                } else {
                    return;
                }


                if (!newLanguage.equals(selectedLanguage)) {
                    selectedLanguage = newLanguage;
                    saveSettings();

                    recreate();
                }

                updateLanguageUI();
                Toast.makeText(MainActivity.this, "Мова встановлена: " + selectedLanguage, Toast.LENGTH_SHORT).show();
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
                    finish();
                }
            }
        });
    }

    private void loadSettings() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // NOTE: Ми вже завантажили мову в attachBaseContext, але оновлюємо UI
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
        final float UNSELECTED_ALPHA = 0.3f;

        // Ці методи тепер працюють лише з візуальним відображенням кнопок,
        // а не фактичною зміною мови
        langUa.setAlpha(selectedLanguage.equals("ua") ? SELECTED_ALPHA : UNSELECTED_ALPHA);
        langEn.setAlpha(selectedLanguage.equals("en") ? SELECTED_ALPHA : UNSELECTED_ALPHA);
    }

    // =======================================================
    // 2. ДОПОМІЖНИЙ МЕТОД ДЛЯ ВСТАНОВЛЕННЯ ЛОКАЛІ
    // =======================================================
    @SuppressWarnings("deprecation")
    public static Context setLocale(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();

        // Оновлення конфігурації для API 24+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            config.setLocale(locale);
            return context.createConfigurationContext(config);
        } else {
            // Оновлення конфігурації для старих версій
            config.locale = locale;
            resources.updateConfiguration(config, resources.getDisplayMetrics());
            return context;
        }
    }




}