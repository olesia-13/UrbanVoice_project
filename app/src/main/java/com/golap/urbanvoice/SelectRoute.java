package com.golap.urbanvoice;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SelectRoute extends AppCompatActivity implements RouteAdapter.OnRouteClickListener {

    private LinearLayout goBackButton;
    private EditText editSearch;
    private RecyclerView recyclerView;
    private RouteAdapter adapter;
    private List<Route> allRoutes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_select_route);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        goBackButton = findViewById(R.id.go_back_button);
        editSearch = findViewById(R.id.edit_name);
        recyclerView = findViewById(R.id.routes_recycler_view);

        allRoutes = generateSampleRoutes();

        adapter = new RouteAdapter(allRoutes, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        setupSearchListener();

        if (goBackButton != null) {
            goBackButton.setOnClickListener(v -> {
                Intent intent = new Intent(SelectRoute.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }
    }

    private void setupSearchListener() {
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterRoutes(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterRoutes(String query) {
        List<Route> filteredList = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase(Locale.ROOT);

        for (Route route : allRoutes) {
            if (route.getSearchName().toLowerCase(Locale.ROOT).contains(lowerCaseQuery)) {
                filteredList.add(route);
            }
        }

        adapter.filterList(filteredList);
    }

    @Override
    public void onRouteClick(Route route) {
        Toast.makeText(this,
                "Обрано маршрут: " + route.getDisplayText(),
                Toast.LENGTH_SHORT).show();

        // 2. Створення Intent для запуску RouteMap
        Intent mapIntent = new Intent(this, RouteMap.class);

        // 3. Передача даних маршруту, які RouteMap використає для ініціалізації
        mapIntent.putExtra("ROUTE_KEY", route.getKey());
        mapIntent.putExtra("ROUTE_DISPLAY_NAME", route.getDisplayText());
        mapIntent.putExtra("ROUTE_ICON_ID", route.getIconResId());

        // 4. Запуск нової активності
        startActivity(mapIntent);
    }

    private List<Route> generateSampleRoutes() {
        List<Route> routes = new ArrayList<>();

        routes.add(new Route(
                getString(R.string.route_type_bus),
                "№111",
                getString(R.string.route_111_desc_ab),
                "R111",
                R.drawable.ic_bus
        ));

        routes.add(new Route(
                getString(R.string.route_type_bus),
                "№38",
                getString(R.string.route_38_desc_ab),
                "R038",
                R.drawable.ic_bus
        ));

        routes.add(new Route(
                getString(R.string.route_type_tram),
                "№1",
                getString(R.string.route_1_desc_ab),
                "R001",
                R.drawable.ic_tram
        ));

        return routes;
    }
}
