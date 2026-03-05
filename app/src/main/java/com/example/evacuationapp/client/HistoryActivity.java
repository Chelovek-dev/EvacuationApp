package com.example.evacuationapp.client;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.evacuationapp.R;
import com.example.evacuationapp.models.Order;
import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private ListView lvOrders;
    private Button btnBack;
    private ArrayAdapter<String> adapter;
    private List<String> orderStrings = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        lvOrders = findViewById(R.id.lvOrders);
        btnBack = findViewById(R.id.btnBack);

        // Для теста создадим несколько заказов
        loadTestOrders();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, orderStrings);
        lvOrders.setAdapter(adapter);

        lvOrders.setOnItemClickListener((parent, view, position, id) -> {
            Toast.makeText(HistoryActivity.this, "Заказ: " + orderStrings.get(position), Toast.LENGTH_SHORT).show();
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void loadTestOrders() {
        orderStrings.add("Заказ #1 - ул. Ленина 10 → ул. Пушкина 5 - 1000 ₽ (Завершен)");
        orderStrings.add("Заказ #2 - пр. Мира 15 → ул. Гагарина 8 - 1000 ₽ (Завершен)");
        orderStrings.add("Заказ #3 - ул. Советская 3 → ул. Кирова 12 - 1000 ₽ (Завершен)");
        orderStrings.add("Заказ #4 - ул. Лесная 7 → ул. Полевая 4 - 1000 ₽ (Отменен)");
    }
}