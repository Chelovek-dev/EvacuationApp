package com.example.evacuationapp.client;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.evacuationapp.R;

public class ClientMainActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private Button btnCreateOrder, btnHistory, btnLogout;
    private String clientId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_main);

        tvWelcome = findViewById(R.id.tvWelcome);
        btnCreateOrder = findViewById(R.id.btnCreateOrder);
        btnHistory = findViewById(R.id.btnHistory);
        btnLogout = findViewById(R.id.btnLogout);

        String phone = getIntent().getStringExtra("phone");
        clientId = "client_" + (phone != null ? phone : "123");

        if (phone != null) {
            tvWelcome.setText("Добро пожаловать, " + phone);
        }

        btnCreateOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ClientMainActivity.this, CreateOrderActivity.class);
                intent.putExtra("clientId", clientId);
                startActivity(intent);
            }
        });

        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ClientMainActivity.this, HistoryActivity.class);
                startActivity(intent);
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}