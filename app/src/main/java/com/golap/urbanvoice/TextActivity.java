package com.golap.urbanvoice;

import android.os.Bundle;
import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class TextActivity extends AppCompatActivity {

    private TextView titleTextView;
    private TextView contentTextView;
    private LinearLayout backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_text);

        titleTextView = findViewById(R.id.text_guide_title);
        contentTextView = findViewById(R.id.text_guide_content);
        backButton = findViewById(R.id.back_button);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 3. Обробка кнопки "Повернутися"
        backButton.setOnClickListener(v -> finish());

        // 4. Завантаження та відображення даних
        loadTextData();
    }

    private void loadTextData() {
        Intent intent = getIntent();
        String routeDisplayName = intent.getStringExtra("ROUTE_DISPLAY_NAME");
        int textResId = intent.getIntExtra("TEXT_RES_ID", 0);

        // Встановлення заголовка маршруту
        if (titleTextView != null) {
            titleTextView.setText(routeDisplayName != null ? routeDisplayName : "Текстовий гід");
        }

        // Встановлення вмісту тексту
        if (contentTextView != null) {
            if (textResId != 0) {
                contentTextView.setText(getString(textResId));
            } else {
                contentTextView.setText("Текст гіда не знайдено. Переконайтеся, що ви передали правильний ресурсний ID.");
            }
        }
    }

}