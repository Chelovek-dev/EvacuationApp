package com.example.evacuationapp.client;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.evacuationapp.R;
import com.example.evacuationapp.models.Order;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ClientHistoryAdapter extends RecyclerView.Adapter<ClientHistoryAdapter.OrderViewHolder> {

    private List<Order> orders;

    public ClientHistoryAdapter(List<Order> orders) {
        this.orders = orders;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_card_client, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);
        holder.bind(order);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    public void updateOrders(List<Order> newOrders) {
        this.orders = newOrders;
        notifyDataSetChanged();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {

        private TextView tvOrderId, tvStatus, tvDate, tvPickupAddress, tvDropoffAddress, tvPrice;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvPickupAddress = itemView.findViewById(R.id.tvPickupAddress);
            tvDropoffAddress = itemView.findViewById(R.id.tvDropoffAddress);
            tvPrice = itemView.findViewById(R.id.tvPrice);
        }

        public void bind(Order order) {
            tvOrderId.setText("Заказ №" + order.getOrderId());
            tvPickupAddress.setText(order.getPickupAddress());
            tvDropoffAddress.setText(order.getDropoffAddress());
            tvPrice.setText("💰 Стоимость: " + (int) order.getPrice() + " ₽");

            // Форматируем дату
            String dateStr = "";
            if (order.getCreatedAt() != null && !order.getCreatedAt().isEmpty()) {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                    SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
                    Date date = inputFormat.parse(order.getCreatedAt());
                    dateStr = outputFormat.format(date);
                } catch (Exception e) {
                    dateStr = order.getCreatedAt();
                }
            }
            tvDate.setText(dateStr);

            // Устанавливаем цвет статуса
            String status = order.getStatus();
            tvStatus.setText(order.getStatusText());

            if ("completed".equals(status)) {
                tvStatus.setBackgroundColor(0xFF4CAF50); // зелёный
            } else if ("cancelled".equals(status)) {
                tvStatus.setBackgroundColor(0xFFF44336); // красный
            } else if ("accepted".equals(status)) {
                tvStatus.setBackgroundColor(0xFF2196F3); // синий
            } else if ("in_progress".equals(status)) {
                tvStatus.setBackgroundColor(0xFF9C27B0); // фиолетовый
            } else {
                tvStatus.setBackgroundColor(0xFFFF9800); // оранжевый
            }
        }
    }
}