package com.example.evacuationapp.driver;

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
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverHistoryActivity extends AppCompatActivity {

    private RecyclerView rvOrders;
    private Button btnBack;
    private DriverHistoryAdapter adapter;
    private List<Order> ordersList = new ArrayList<>();
    private long driverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_history);

        rvOrders = findViewById(R.id.rvOrders);
        btnBack = findViewById(R.id.btnBack);
        driverId = new PreferenceManager(this).getUserId();

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DriverHistoryAdapter(ordersList);
        rvOrders.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        loadDriverHistory();
    }

    private void loadDriverHistory() {
        Call<List<Order>> call = RetrofitClient.getApiService().getDriverOrders(driverId);
        call.enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ordersList.clear();
                    ordersList.addAll(response.body());
                    adapter.updateOrders(ordersList);

                    if (ordersList.isEmpty()) {
                        Toast.makeText(DriverHistoryActivity.this,
                                "У вас пока нет выполненных заказов", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(DriverHistoryActivity.this,
                            "Ошибка загрузки истории", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable t) {
                Toast.makeText(DriverHistoryActivity.this,
                        "Нет связи с сервером", Toast.LENGTH_SHORT).show();
            }
        });
    }
}