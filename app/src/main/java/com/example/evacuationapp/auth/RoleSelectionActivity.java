package com.example.evacuationapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.evacuationapp.R;
import com.example.evacuationapp.client.ClientMainActivity;
import com.example.evacuationapp.driver.DriverMainActivity;
import com.example.evacuationapp.network.RetrofitClient;
import com.example.evacuationapp.utils.PreferenceManager;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RoleSelectionActivity extends AppCompatActivity {

    private Button btnClient, btnDriver;
    private String phone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        phone = getIntent().getStringExtra("phone");
        if (phone == null) phone = "";

        btnClient = findViewById(R.id.btnClient);
        btnDriver = findViewById(R.id.btnDriver);

        btnClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginWithRole("client");
            }
        });

        btnDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginWithRole("driver");
            }
        });
    }

    private void loginWithRole(String role) {
        Map<String, String> body = new HashMap<>();
        body.put("phone", phone);
        body.put("role", role);
        body.put("name", "");

        Call<Map<String, Object>> call = RetrofitClient.getApiService().login(body);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = response.body();
                    String token = (String) data.get("token");
                    Map<String, Object> userMap = (Map<String, Object>) data.get("user");

                    Number userIdNumber = (Number) userMap.get("user_id");
                    long userId = userIdNumber.longValue();
                    String userRole = (String) userMap.get("role");

                    PreferenceManager prefManager = new PreferenceManager(RoleSelectionActivity.this);
                    prefManager.saveToken(token);
                    prefManager.saveUserId(userId);

                    Intent intent;
                    if ("client".equals(userRole)) {
                        intent = new Intent(RoleSelectionActivity.this, ClientMainActivity.class);
                    } else {
                        intent = new Intent(RoleSelectionActivity.this, DriverMainActivity.class);
                    }
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(RoleSelectionActivity.this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(RoleSelectionActivity.this, "Нет связи с сервером", Toast.LENGTH_SHORT).show();
            }
        });
    }
}