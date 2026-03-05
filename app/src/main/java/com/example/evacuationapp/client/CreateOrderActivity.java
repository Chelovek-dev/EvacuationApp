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
import java.util.UUID;

public class CreateOrderActivity extends AppCompatActivity {

    private EditText etPickup, etDropoff;
    private Button btnCreate, btnCancel;
    private String clientId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_order);

        clientId = getIntent().getStringExtra("clientId");
        if (clientId == null) clientId = "client_123"; // Для теста

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

        // Создаем заказ
        Order order = new Order();
        order.setOrderId(UUID.randomUUID().toString());
        order.setClientId(clientId);
        order.setPickupAddress(pickup);
        order.setDropoffAddress(dropoff);
        order.setStatus("waiting");

        // Передаем заказ в TrackOrderActivity
        Intent intent = new Intent(CreateOrderActivity.this, TrackOrderActivity.class);
        intent.putExtra("order", order);
        startActivity(intent);
        finish();
    }
}