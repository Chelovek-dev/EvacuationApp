package com.example.evacuationapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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

public class LoginActivity extends AppCompatActivity {

    private EditText etPhone;
    private Button btnLogin;
    private TextView tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etPhone = findViewById(R.id.etPhone);
        btnLogin = findViewById(R.id.btnLogin);
        tvError = findViewById(R.id.tvError);

        btnLogin.setOnClickListener(v -> {
            String phone = etPhone.getText().toString().trim();
            if (TextUtils.isEmpty(phone)) {
                showError("Введите номер телефона");
                return;
            }
            if (phone.length() < 10) {
                showError("Неверный формат номера");
                return;
            }
            checkUserExists(phone);
        });
    }

    private void checkUserExists(String phone) {
        Call<Map<String, Object>> call = RetrofitClient.getApiService().checkUserExists(phone);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    boolean exists = (boolean) response.body().get("exists");
                    if (exists) {
                        String role = (String) response.body().get("role");
                        loginWithRole(phone, role);
                    } else {
                        Intent intent = new Intent(LoginActivity.this, RoleSelectionActivity.class);
                        intent.putExtra("phone", phone);
                        startActivity(intent);
                    }
                } else {
                    showError("Ошибка проверки пользователя");
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                showError("Нет связи с сервером");
            }
        });
    }

    private void loginWithRole(String phone, String role) {
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
                    long userId = ((Number) userMap.get("userId")).longValue();
                    String userRole = (String) userMap.get("role");

                    PreferenceManager prefManager = new PreferenceManager(LoginActivity.this);
                    prefManager.saveToken(token);
                    prefManager.saveUserId(userId);

                    Intent intent;
                    if ("client".equals(userRole)) {
                        intent = new Intent(LoginActivity.this, ClientMainActivity.class);
                    } else {
                        intent = new Intent(LoginActivity.this, DriverMainActivity.class);
                    }
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                    finish();
                } else {
                    showError("Ошибка входа");
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                showError("Нет связи с сервером");
            }
        });
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }
}