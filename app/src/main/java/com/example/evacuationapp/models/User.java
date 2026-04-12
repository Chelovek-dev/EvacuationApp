package com.example.evacuationapp.models;

import java.io.Serializable;

public class User implements Serializable {
    private long userId;      // SERIAL → long
    private String phone;
    private String role;      // "client" или "driver"
    private String name;
    private String registeredAt;
    private String lastLogin;

    public User() {}

    // Геттеры и сеттеры
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(String registeredAt) { this.registeredAt = registeredAt; }

    public String getLastLogin() { return lastLogin; }
    public void setLastLogin(String lastLogin) { this.lastLogin = lastLogin; }
}