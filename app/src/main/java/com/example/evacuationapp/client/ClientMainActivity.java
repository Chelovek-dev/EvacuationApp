package com.example.evacuationapp.client;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.evacuationapp.R;
import com.example.evacuationapp.utils.PreferenceManager;

public class ClientMainActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private Button btnCreateOrder, btnHistory, btnLogout;
    private long userId; // теперь long

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_main);

        // Получаем userId из Intent (если передали) или из SharedPreferences
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

        btnCreateOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ClientMainActivity.this, CreateOrderActivity.class);
                intent.putExtra("clientId", userId); // передаём long
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
                // Очищаем данные и возвращаемся на экран входа
                new PreferenceManager(ClientMainActivity.this).clear();
                finish();
            }
        });
    }
}