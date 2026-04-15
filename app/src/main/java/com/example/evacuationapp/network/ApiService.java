package com.example.evacuationapp.network;

import com.example.evacuationapp.models.DriverLocation;
import com.example.evacuationapp.models.Order;

import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;
import java.util.Map;

public interface ApiService {
    @POST("/api/auth/login")
    Call<Map<String, Object>> login(@Body Map<String, String> body);

    @POST("/api/orders")
    Call<Order> createOrder(@Body Order order);

    @GET("/api/orders/{orderId}")
    Call<Order> getOrder(@Path("orderId") long orderId);

    @GET("/api/orders/client/{clientId}")
    Call<List<Order>> getClientOrders(@Path("clientId") long clientId);

    @GET("/api/orders/available")
    Call<List<Order>> getAvailableOrders();

    @PUT("/api/orders/{orderId}/accept")
    Call<Order> acceptOrder(@Path("orderId") long orderId, @Body Map<String, Long> body);

    @PUT("/api/orders/{orderId}/status")
    Call<Order> updateOrderStatus(@Path("orderId") long orderId, @Body Map<String, String> status);

    @POST("/api/location")
    Call<Void> updateLocation(@Body DriverLocation location);

    @GET("/api/location/{driverId}")
    Call<DriverLocation> getDriverLocation(@Path("driverId") long driverId);
    @GET("/api/orders/driver/{driverId}")
    Call<List<Order>> getDriverOrders(@Path("driverId") long driverId);
    @GET("/api/auth/check/{phone}")
    Call<Map<String, Object>> checkUserExists(@Path("phone") String phone);
}
