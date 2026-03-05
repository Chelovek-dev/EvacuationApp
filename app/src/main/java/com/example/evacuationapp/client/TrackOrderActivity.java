package com.example.evacuationapp.client;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.evacuationapp.R;
import com.example.evacuationapp.models.Order;

public class TrackOrderActivity extends AppCompatActivity {

    private TextView tvOrderId, tvStatus, tvPickup, tvDropoff, tvPrice;
    private Button btnCancel, btnBack;
    private Order order;
    private Handler handler = new Handler();
    private Runnable updateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_order);

        order = (Order) getIntent().getSerializableExtra("order");

        tvOrderId = findViewById(R.id.tvOrderId);
        tvStatus = findViewById(R.id.tvStatus);
        tvPickup = findViewById(R.id.tvPickupAddress);
        tvDropoff = findViewById(R.id.tvDropoffAddress);
        tvPrice = findViewById(R.id.tvPrice);
        btnCancel = findViewById(R.id.btnCancelOrder);
        btnBack = findViewById(R.id.btnBack);

        if (order != null) {
            tvOrderId.setText("Заказ №" + order.getOrderId().substring(0, 8));
            tvPickup.setText("Откуда: " + order.getPickupAddress());
            tvDropoff.setText("Куда: " + order.getDropoffAddress());
            tvPrice.setText("Стоимость: " + (int)order.getPrice() + " ₽");
        }

        // Имитация обновления статуса
        startStatusSimulation();

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (order != null && !"completed".equals(order.getStatus())) {
                    order.setStatus("cancelled");
                    tvStatus.setText("Статус: Отменен");
                    Toast.makeText(TrackOrderActivity.this, "Заказ отменен", Toast.LENGTH_SHORT).show();
                    btnCancel.setEnabled(false);
                }
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void startStatusSimulation() {
        updateRunnable = new Runnable() {
            int step = 0;
            @Override
            public void run() {
                if (order == null) return;

                switch (step) {
                    case 0:
                        tvStatus.setText("Статус: " + order.getStatusText());
                        step++;
                        handler.postDelayed(this, 3000);
                        break;
                    case 1:
                        order.setStatus("accepted");
                        tvStatus.setText("Статус: " + order.getStatusText());
                        tvStatus.append("\nВодитель: Иван (номер: 123)");
                        step++;
                        handler.postDelayed(this, 4000);
                        break;
                    case 2:
                        order.setStatus("in_progress");
                        tvStatus.setText("Статус: " + order.getStatusText());
                        tvStatus.append("\nВодитель едет к вам");
                        step++;
                        handler.postDelayed(this, 5000);
                        break;
                    case 3:
                        order.setStatus("completed");
                        tvStatus.setText("Статус: " + order.getStatusText());
                        btnCancel.setEnabled(false);
                        Toast.makeText(TrackOrderActivity.this, "Заказ завершен", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };

        handler.post(updateRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
    }
}