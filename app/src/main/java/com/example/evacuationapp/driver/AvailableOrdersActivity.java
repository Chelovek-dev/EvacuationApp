package com.example.evacuationapp.driver;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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

    private RecyclerView rvOrders;
    private Button btnBack;
    private OrderAdapter adapter;
    private List<Order> ordersList = new ArrayList<>();
    private long driverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_orders);

        rvOrders = findViewById(R.id.rvOrders);
        btnBack = findViewById(R.id.btnBack);
        driverId = new PreferenceManager(this).getUserId();

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(ordersList, order -> acceptOrder(order));
        rvOrders.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        loadAvailableOrders();
    }

    private void loadAvailableOrders() {
        Call<List<Order>> call = RetrofitClient.getApiService().getAvailableOrders();
        call.enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ordersList.clear();
                    ordersList.addAll(response.body());
                    adapter.updateOrders(ordersList);

                    if (ordersList.isEmpty()) {
                        Toast.makeText(AvailableOrdersActivity.this,
                                "Нет доступных заказов", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(AvailableOrdersActivity.this,
                            "Ошибка загрузки заказов", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable t) {
                Toast.makeText(AvailableOrdersActivity.this,
                        "Нет связи с сервером", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void acceptOrder(Order order) {
        Map<String, Long> body = new HashMap<>();
        body.put("driverId", driverId);

        Call<Order> call = RetrofitClient.getApiService().acceptOrder(order.getOrderId(), body);
        call.enqueue(new Callback<Order>() {
            @Override
            public void onResponse(Call<Order> call, Response<Order> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Order acceptedOrder = response.body();
                    Toast.makeText(AvailableOrdersActivity.this,
                            "Заказ принят!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(AvailableOrdersActivity.this,
                            CurrentOrderActivity.class);
                    intent.putExtra("order", acceptedOrder);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(AvailableOrdersActivity.this,
                            "Не удалось принять заказ", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Order> call, Throwable t) {
                Toast.makeText(AvailableOrdersActivity.this,
                        "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}