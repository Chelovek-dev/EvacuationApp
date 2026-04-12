package com.example.evacuationapp.client;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.evacuationapp.R;
import com.example.evacuationapp.models.Order;
import com.example.evacuationapp.network.RetrofitClient;
import com.example.evacuationapp.utils.PreferenceManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateOrderActivity extends AppCompatActivity {

    private EditText etPickup, etDropoff;
    private Button btnCreate, btnCancel;
    private long clientId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_order);

        // Получаем clientId из SharedPreferences (сохранили при логине)
        clientId = new PreferenceManager(this).getUserId();

        etPickup = findViewById(R.id.etPickupAddress);
        etDropoff = findViewById(R.id.etDropoffAddress);
        btnCreate = findViewById(R.id.btnCreateOrder);
        btnCancel = findViewById(R.id.btnCancel);

        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createOrder();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void createOrder() {
        String pickup = etPickup.getText().toString().trim();
        String dropoff = etDropoff.getText().toString().trim();

        if (pickup.isEmpty() || dropoff.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        // Создаём объект Order для отправки (без ID, сервер сам назначит)
        Order order = new Order();
        order.setClientId(clientId);
        order.setPickupAddress(pickup);
        order.setDropoffAddress(dropoff);
        order.setStatus("waiting"); // будет перезаписано сервером
        order.setPrice(1000.0);

        // Вызываем API
        Call<Order> call = RetrofitClient.getApiService().createOrder(order);
        call.enqueue(new Callback<Order>() {
            @Override
            public void onResponse(Call<Order> call, Response<Order> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Order createdOrder = response.body();
                    Intent intent = new Intent(CreateOrderActivity.this, TrackOrderActivity.class);
                    intent.putExtra("orderId", createdOrder.getOrderId());
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(CreateOrderActivity.this, "Ошибка создания заказа", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Order> call, Throwable t) {
                Toast.makeText(CreateOrderActivity.this, "Нет связи с сервером", Toast.LENGTH_SHORT).show();
            }
        });
    }
}