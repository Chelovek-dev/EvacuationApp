package com.example.evacuationapp.client;

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

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView rvOrders;
    private Button btnBack;
    private ClientHistoryAdapter adapter;
    private List<Order> ordersList = new ArrayList<>();
    private long clientId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        rvOrders = findViewById(R.id.rvOrders);
        btnBack = findViewById(R.id.btnBack);
        clientId = new PreferenceManager(this).getUserId();

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ClientHistoryAdapter(ordersList);
        rvOrders.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        loadClientHistory();
    }

    private void loadClientHistory() {
        Toast.makeText(this, "Загрузка истории...", Toast.LENGTH_SHORT).show();

        Call<List<Order>> call = RetrofitClient.getApiService().getClientOrders(clientId);
        call.enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ordersList.clear();
                    ordersList.addAll(response.body());
                    adapter.updateOrders(ordersList);

                    if (ordersList.isEmpty()) {
                        Toast.makeText(HistoryActivity.this, "У вас пока нет заказов", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(HistoryActivity.this, "Ошибка загрузки истории", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable t) {
                Toast.makeText(HistoryActivity.this, "Нет связи с сервером", Toast.LENGTH_SHORT).show();
            }
        });
    }
}