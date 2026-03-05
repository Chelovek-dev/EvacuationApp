package com.example.evacuationapp.models;

import java.io.Serializable;

public class Order implements Serializable {
    private String orderId;
    private String clientId;
    private String driverId;
    private String status; // waiting, accepted, in_progress, completed
    private String pickupAddress;
    private String dropoffAddress;
    private double price;
    private long createdAt;
    private long completedAt;

    public Order() {
        this.price = 1000.0;
        this.status = "waiting";
        this.createdAt = System.currentTimeMillis();
    }

    public Order(String clientId, String pickupAddress, String dropoffAddress) {
        this.clientId = clientId;
        this.pickupAddress = pickupAddress;
        this.dropoffAddress = dropoffAddress;
        this.price = 1000.0;
        this.status = "waiting";
        this.createdAt = System.currentTimeMillis();
    }

    // Геттеры и сеттеры
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

    public String getDropoffAddress() { return dropoffAddress; }
    public void setDropoffAddress(String dropoffAddress) { this.dropoffAddress = dropoffAddress; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }

    // Для удобного отображения статуса на русском
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