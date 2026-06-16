package com.example.evacuationapp.network;

import com.example.evacuationapp.models.DriverLocation;
import com.example.evacuationapp.models.Order;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    @POST("/api/auth/login")
    Call<Map<String, Object>> login(@Body Map<String, String> body);
    @POST("/api/auth/send-code")
    Call<Map<String, Object>> sendCode(@Body Map<String, String> body);
    @POST("/api/auth/verify-code")
    Call<Map<String, Object>> verifyCode(@Body Map<String, String> body);
    @GET("/api/auth/check/{phone}")
    Call<Map<String, Object>> checkUserExists(@Path("phone") String phone);
    @GET("/api/users/{userId}")
    Call<Map<String, Object>> getUserById(@Path("userId") long userId);
    @POST("/api/orders")
    Call<Order> createOrder(@Body Order order);
    @GET("/api/orders/{orderId}")
    Call<Order> getOrder(@Path("orderId") long orderId);
    @GET("/api/orders/client/{clientId}")
    Call<List<Order>> getClientOrders(@Path("clientId") long clientId);
    @GET("/api/orders/driver/{driverId}")
    Call<List<Order>> getDriverOrders(@Path("driverId") long driverId);
    @GET("/api/orders/available")
    Call<List<Order>> getAvailableOrders();
    @PUT("/api/orders/{orderId}/accept")
    Call<Order> acceptOrder(@Path("orderId") long orderId, @Body Map<String, Long> body);
    @PUT("/api/orders/{orderId}/status")
    Call<Order> updateOrderStatus(@Path("orderId") long orderId, @Body Map<String, String> status);
    @PUT("/api/orders/{orderId}/cancel")
    Call<Order> cancelOrder(@Path("orderId") long orderId);

    @POST("/api/location")
    Call<Void> updateLocation(@Body DriverLocation location);

    @GET("/api/location/{driverId}")
    Call<DriverLocation> getDriverLocation(@Path("driverId") long driverId);
    @PUT("/api/orders/{orderId}/cancel-by-driver")
    Call<Order> cancelOrderByDriver(@Path("orderId") long orderId, @Body Map<String, Long> body);
    @PUT("/api/drivers/status")
    Call<Void> updateDriverStatus(@Body Map<String, Object> body);
    @GET("/api/drivers/{driverId}/status")
    Call<Map<String, Object>> getDriverStatus(@Path("driverId") long driverId);
    @POST("/api/auth/check-email")
    Call<Map<String, Object>> checkEmailExists(@Body Map<String, String> body);
    @PUT("/api/drivers/{driverId}/profile")
    Call<Void> updateDriverProfile(@Path("driverId") long driverId, @Body Map<String, String> body);
}