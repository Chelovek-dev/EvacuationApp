package com.example.evacuationapp.driver;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.evacuationapp.R;
import com.example.evacuationapp.models.Order;
import com.example.evacuationapp.network.RetrofitClient;
import com.example.evacuationapp.utils.PreferenceManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AvailableOrdersActivity extends AppCompatActivity {

    private ListView lvOrders;
    private Button btnBack;
    private ArrayAdapter<String> adapter;
    private List<Order> ordersList;
    private long driverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_orders);

        lvOrders = findViewById(R.id.lvOrders);
        btnBack = findViewById(R.id.btnBack);
        driverId = new PreferenceManager(this).getUserId();

        btnBack.setOnClickListener(v -> finish());

        loadAvailableOrders();

        lvOrders.setOnItemClickListener((parent, view, position, id) -> {
            if (ordersList != null && position < ordersList.size()) {
                Order selected = ordersList.get(position);
                acceptOrder(selected.getOrderId());
            }
        });
    }

    private void loadAvailableOrders() {
        Toast.makeText(this, "Загрузка заказов...", Toast.LENGTH_SHORT).show();

        Call<List<Order>> call = RetrofitClient.getApiService().getAvailableOrders();
        call.enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ordersList = response.body();
                    if (ordersList.isEmpty()) {
                        Toast.makeText(AvailableOrdersActivity.this, "Нет доступных заказов", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        String[] titles = new String[ordersList.size()];
                        for (int i = 0; i < ordersList.size(); i++) {
                            Order o = ordersList.get(i);
                            titles[i] = "Заказ #" + o.getOrderId() + " | " + o.getPickupAddress() + " → " + o.getDropoffAddress() + " | " + (int)o.getPrice() + " ₽";
                        }
                        adapter = new ArrayAdapter<>(AvailableOrdersActivity.this, android.R.layout.simple_list_item_1, titles);
                        lvOrders.setAdapter(adapter);
                    }
                } else {
                    String errorMsg = "Ошибка загрузки заказов";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception e) {}
                    Toast.makeText(AvailableOrdersActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable t) {
                Toast.makeText(AvailableOrdersActivity.this, "Нет связи с сервером: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void acceptOrder(long orderId) {
        Map<String, Long> body = new HashMap<>();
        body.put("driverId", driverId);

        Call<Order> call = RetrofitClient.getApiService().acceptOrder(orderId, body);
        call.enqueue(new Callback<Order>() {
            @Override
            public void onResponse(Call<Order> call, Response<Order> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Order acceptedOrder = response.body();
                    Toast.makeText(AvailableOrdersActivity.this, "Заказ принят!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(AvailableOrdersActivity.this, CurrentOrderActivity.class);
                    intent.putExtra("order", acceptedOrder);
                    startActivity(intent);
                    finish();
                } else {
                    String errorMsg = "Не удалось принять заказ";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception e) {}
                    Toast.makeText(AvailableOrdersActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Order> call, Throwable t) {
                Toast.makeText(AvailableOrdersActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}