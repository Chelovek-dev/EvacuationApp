package com.example.evacuationapp.client;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.evacuationapp.R;
import com.example.evacuationapp.auth.LoginActivity;  // ← добавить импорт
import com.example.evacuationapp.utils.PreferenceManager;

public class ClientMainActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private Button btnCreateOrder, btnHistory, btnLogout;
    private long userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_main);

        userId = getIntent().getLongExtra("userId", 0);
        if (userId == 0) {
            userId = new PreferenceManager(this).getUserId();
        }

        tvWelcome = findViewById(R.id.tvWelcome);
        btnCreateOrder = findViewById(R.id.btnCreateOrder);
        btnHistory = findViewById(R.id.btnHistory);
        btnLogout = findViewById(R.id.btnLogout);

        String phone = getIntent().getStringExtra("phone");
        if (phone != null) {
            tvWelcome.setText("Добро пожаловать, " + phone);
        } else {
            tvWelcome.setText("Добро пожаловать, клиент!");
        }

        btnCreateOrder.setOnClickListener(v -> {
            Intent intent = new Intent(ClientMainActivity.this, CreateOrderActivity.class);
            intent.putExtra("clientId", userId);
            startActivity(intent);
        });

        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(ClientMainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            PreferenceManager prefManager = new PreferenceManager(ClientMainActivity.this);
            prefManager.clear();

            Intent intent = new Intent(ClientMainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}