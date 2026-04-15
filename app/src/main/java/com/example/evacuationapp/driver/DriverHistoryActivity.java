package com.example.evacuationapp.driver;

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
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverHistoryActivity extends AppCompatActivity {

    private ListView lvOrders;
    private ArrayAdapter<String> adapter;
    private List<String> orderStrings = new ArrayList<>();
    private long driverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_history);

        lvOrders = findViewById(R.id.lvOrders);
        driverId = new PreferenceManager(this).getUserId();

        Toast.makeText(this, "Загрузка истории...", Toast.LENGTH_SHORT).show();
        loadOrders();
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadOrders() {
        Call<List<Order>> call = RetrofitClient.getApiService().getDriverOrders(driverId);
        call.enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Order> orders = response.body();
                    if (orders.isEmpty()) {
                        orderStrings.add("У вас пока нет выполненных заказов");
                    } else {
                        for (Order order : orders) {
                            String date = order.getCreatedAt() != null ? order.getCreatedAt().substring(0, 10) : "дата не указана";
                            String statusText = order.getStatusText();
                            String line = "Заказ №" + order.getOrderId() + " | " + date + " | " + statusText + "\n" +
                                    order.getPickupAddress() + " → " + order.getDropoffAddress() + "\n" +
                                    "Стоимость: " + (int) order.getPrice() + " ₽";
                            orderStrings.add(line);
                        }
                    }
                    adapter = new ArrayAdapter<>(DriverHistoryActivity.this, android.R.layout.simple_list_item_1, orderStrings);
                    lvOrders.setAdapter(adapter);
                } else {
                    Toast.makeText(DriverHistoryActivity.this, "Ошибка загрузки истории", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable t) {
                Toast.makeText(DriverHistoryActivity.this, "Нет связи с сервером", Toast.LENGTH_SHORT).show();
            }
        });
    }
}