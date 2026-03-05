package com.example.evacuationapp.driver;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.evacuationapp.R;

public class DriverMainActivity extends AppCompatActivity {

    private TextView tvWelcome, tvStatus;
    private Switch switchOnline;
    private Button btnAvailableOrders, btnHistory, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_main);

        tvWelcome = findViewById(R.id.tvWelcome);
        tvStatus = findViewById(R.id.tvStatus);
        switchOnline = findViewById(R.id.switchOnline);
        btnAvailableOrders = findViewById(R.id.btnAvailableOrders);
        btnHistory = findViewById(R.id.btnHistory);
        btnLogout = findViewById(R.id.btnLogout);

        String phone = getIntent().getStringExtra("phone");
        if (phone != null) {
            tvWelcome.setText("Водитель: " + phone);
        }

        switchOnline.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                tvStatus.setText("Статус: На линии");
                tvStatus.setTextColor(getColor(android.R.color.holo_green_dark));
            } else {
                tvStatus.setText("Статус: Не на линии");
                tvStatus.setTextColor(getColor(android.R.color.darker_gray));
            }
        });

        btnAvailableOrders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!switchOnline.isChecked()) {
                    Toast.makeText(DriverMainActivity.this, "Включите статус 'На линии'", Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(DriverMainActivity.this, "Поиск заказов...", Toast.LENGTH_SHORT).show();
            }
        });

        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(DriverMainActivity.this, "История заказов", Toast.LENGTH_SHORT).show();
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