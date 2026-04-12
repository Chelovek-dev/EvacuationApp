package com.example.evacuationapp.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Order implements Serializable {

    @SerializedName("order_id")
    private long orderId;

    @SerializedName("client_id")
    private long clientId;

    @SerializedName("driver_id")
    private long driverId;

    @SerializedName("status")
    private String status;

    @SerializedName("pickup_address")
    private String pickupAddress;

    @SerializedName("dropoff_address")
    private String dropoffAddress;

    @SerializedName("price")
    private double price;

    @SerializedName("created_at")
    private String createdAt;   // сервер возвращает строку ISO

    @SerializedName("completed_at")
    private String completedAt; // может быть null

    public Order() {
        this.price = 1000.0;
        this.status = "waiting";
        this.createdAt = null;
        this.completedAt = null;
    }

    public Order(long clientId, String pickupAddress, String dropoffAddress) {
        this.clientId = clientId;
        this.pickupAddress = pickupAddress;
        this.dropoffAddress = dropoffAddress;
        this.price = 1000.0;
        this.status = "waiting";
        this.createdAt = null;
        this.completedAt = null;
    }

    // Геттеры и сеттеры
    public long getOrderId() { return orderId; }
    public void setOrderId(long orderId) { this.orderId = orderId; }

    public long getClientId() { return clientId; }
    public void setClientId(long clientId) { this.clientId = clientId; }

    public long getDriverId() { return driverId; }
    public void setDriverId(long driverId) { this.driverId = driverId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

    public String getDropoffAddress() { return dropoffAddress; }
    public void setDropoffAddress(String dropoffAddress) { this.dropoffAddress = dropoffAddress; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }

    public String getStatusText() {
        switch (status) {
            case "waiting": return "Ожидание водителя";
            case "accepted": return "Водитель назначен";
            case "in_progress": return "Выполняется";
            case "completed": return "Завершен";
            default: return status;
        }
    }
}