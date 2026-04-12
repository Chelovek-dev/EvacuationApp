package com.example.evacuationapp.driver;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.evacuationapp.R;
import com.example.evacuationapp.utils.PreferenceManager;

public class DriverMainActivity extends AppCompatActivity {

    private TextView tvWelcome, tvStatus;
    private Switch switchOnline;
    private Button btnAvailableOrders, btnHistory, btnLogout;
    private long userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_main);

        userId = getIntent().getLongExtra("userId", 0);
        if (userId == 0) {
            userId = new PreferenceManager(this).getUserId();
        }

        tvWelcome = findViewById(R.id.tvWelcome);
        tvStatus = findViewById(R.id.tvStatus);
        switchOnline = findViewById(R.id.switchOnline);
        btnAvailableOrders = findViewById(R.id.btnAvailableOrders);
        btnHistory = findViewById(R.id.btnHistory);
        btnLogout = findViewById(R.id.btnLogout);

        String phone = getIntent().getStringExtra("phone");
        if (phone != null) {
            tvWelcome.setText("Водитель: " + phone);
        } else {
            tvWelcome.setText("Водитель");
        }

        switchOnline.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                tvStatus.setText("Статус: На линии");
                tvStatus.setTextColor(getColor(android.R.color.holo_green_dark));
                Toast.makeText(DriverMainActivity.this, "Вы на линии", Toast.LENGTH_SHORT).show();
                // TODO: отправить статус на сервер
            } else {
                tvStatus.setText("Статус: Не на линии");
                tvStatus.setTextColor(getColor(android.R.color.darker_gray));
                Toast.makeText(DriverMainActivity.this, "Вы не на линии", Toast.LENGTH_SHORT).show();
            }
        });

        btnAvailableOrders.setOnClickListener(v -> {
            if (!switchOnline.isChecked()) {
                Toast.makeText(DriverMainActivity.this, "Включите статус 'На линии'", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(DriverMainActivity.this, AvailableOrdersActivity.class);
            startActivity(intent);
        });

        btnHistory.setOnClickListener(v -> {
            // TODO: показать историю заказов водителя
            Toast.makeText(DriverMainActivity.this, "История заказов", Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> {
            new PreferenceManager(DriverMainActivity.this).clear();
            finish();
        });
    }
}