package com.example.evacuationapp.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Order implements Serializable {

    @SerializedName("orderId")
    private long orderId;

    @SerializedName("clientId")
    private long clientId;

    @SerializedName("driverId")
    private long driverId;

    @SerializedName("status")
    private String status;

    @SerializedName("pickupAddress")
    private String pickupAddress;

    @SerializedName("dropoffAddress")
    private String dropoffAddress;

    @SerializedName("price")
    private double price;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("completedAt")
    private String completedAt;

    public Order() {
        this.price = 1000.0;
        this.status = "waiting";
    }

    public Order(long clientId, String pickupAddress, String dropoffAddress) {
        this.clientId = clientId;
        this.pickupAddress = pickupAddress;
        this.dropoffAddress = dropoffAddress;
        this.price = 1000.0;
        this.status = "waiting";
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