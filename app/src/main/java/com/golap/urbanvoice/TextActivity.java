package com.golap.urbanvoice;

import android.os.Bundle;
import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.text.Html;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class TextActivity extends AppCompatActivity {

    private TextView titleTextView;
    private TextView contentTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_text);

        titleTextView = findViewById(R.id.text_guide_title);
        contentTextView = findViewById(R.id.text_guide_content);
        LinearLayout backButton = findViewById(R.id.back_button);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        backButton.setOnClickListener(v -> finish());
        loadTextData();
    }

    private void loadTextData() {
        Intent intent = getIntent();
        String routeDisplayName = intent.getStringExtra("ROUTE_DISPLAY_NAME");
        String routeKey = intent.getStringExtra("ROUTE_KEY");
        String direction = intent.getStringExtra("DIRECTION");
        int textResId = intent.getIntExtra("TEXT_RES_ID", 0);

        if (titleTextView != null) {
            titleTextView.setText(routeDisplayName != null ? routeDisplayName : "Текстовий гід");
        }

        if (contentTextView == null) return;


        if (routeKey != null && direction != null) {
            StringBuilder fullText = new StringBuilder();


            String baseName = String.format("text_%s_%s_",
                    routeKey.toLowerCase(), direction.toLowerCase());

            int partIndex = 1;
            while (true) {
                int resId = getResources().getIdentifier(baseName + partIndex, "string", getPackageName());
                if (resId == 0) break;
                String partText = getString(resId);
                if (partIndex > 1) fullText.append("<br><br><hr><br><br>");
                fullText.append(partText);
                partIndex++;
            }

            if (fullText.length() > 0) {
                contentTextView.setText(Html.fromHtml(fullText.toString(), Html.FROM_HTML_MODE_LEGACY));
                return;
            }
        }


        if (textResId != 0) {
            String shortText = getString(textResId);
            contentTextView.setText(Html.fromHtml(shortText, Html.FROM_HTML_MODE_LEGACY));
        } else {
            contentTextView.setText("Текст гіда не знайдено для цього маршруту або напрямку.");
        }
    }
}
