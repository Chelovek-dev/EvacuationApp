package com.example.evacuationapp.driver;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.evacuationapp.R;
import com.example.evacuationapp.models.Order;
import com.example.evacuationapp.network.RetrofitClient;
import com.example.evacuationapp.utils.PreferenceManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AvailableOrdersActivity extends AppCompatActivity {

    private ListView lvOrders;
    private ArrayAdapter<String> adapter;
    private List<Order> ordersList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_orders);

        lvOrders = findViewById(R.id.lvOrders);
        loadAvailableOrders();

        lvOrders.setOnItemClickListener((parent, view, position, id) -> {
            Order selected = ordersList.get(position);
            acceptOrder(selected.getOrderId());
        });
    }

    private void loadAvailableOrders() {
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
                            titles[i] = "Заказ #" + o.getOrderId() + " | " + o.getPickupAddress() + " → " + o.getDropoffAddress();
                        }
                        adapter = new ArrayAdapter<>(AvailableOrdersActivity.this, android.R.layout.simple_list_item_1, titles);
                        lvOrders.setAdapter(adapter);
                    }
                } else {
                    Toast.makeText(AvailableOrdersActivity.this, "Ошибка загрузки", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable t) {
                Toast.makeText(AvailableOrdersActivity.this, "Нет связи с сервером", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void acceptOrder(long orderId) {
        long driverId = new PreferenceManager(this).getUserId();
        Map<String, Long> body = new HashMap<>();
        body.put("driverId", driverId);

        Call<Order> call = RetrofitClient.getApiService().acceptOrder(orderId, body);
        call.enqueue(new Callback<Order>() {
            @Override
            public void onResponse(Call<Order> call, Response<Order> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AvailableOrdersActivity.this, "Заказ принят!", Toast.LENGTH_SHORT).show();
                    // TODO: можно перейти на экран управления текущим заказом
                    finish();
                } else {
                    Toast.makeText(AvailableOrdersActivity.this, "Не удалось принять заказ", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Order> call, Throwable t) {
                Toast.makeText(AvailableOrdersActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }
}