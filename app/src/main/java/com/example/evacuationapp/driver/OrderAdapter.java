package com.example.evacuationapp.driver;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.evacuationapp.R;
import com.example.evacuationapp.models.Order;
import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private List<Order> orders;
    private OnOrderAcceptListener listener;

    public interface OnOrderAcceptListener {
        void onAccept(Order order);
    }

    public OrderAdapter(List<Order> orders, OnOrderAcceptListener listener) {
        this.orders = orders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_card, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);
        holder.bind(order, listener);
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

        private TextView tvOrderId, tvStatus, tvPickupAddress, tvDropoffAddress, tvPrice;
        private Button btnAccept;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPickupAddress = itemView.findViewById(R.id.tvPickupAddress);
            tvDropoffAddress = itemView.findViewById(R.id.tvDropoffAddress);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnAccept = itemView.findViewById(R.id.btnAccept);
        }

        public void bind(Order order, OnOrderAcceptListener listener) {
            tvOrderId.setText("Заказ №" + order.getOrderId());
            tvPickupAddress.setText(order.getPickupAddress());
            tvDropoffAddress.setText(order.getDropoffAddress());
            tvPrice.setText("💰 " + (int) order.getPrice() + " ₽");

            // Устанавливаем статус
            tvStatus.setText(order.getStatusText());
            if ("waiting".equals(order.getStatus())) {
                tvStatus.setBackgroundColor(0xFFFF9800); // оранжевый
            } else {
                tvStatus.setBackgroundColor(0xFF4CAF50); // зелёный
            }

            btnAccept.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAccept(order);
                }
            });
        }
    }
}