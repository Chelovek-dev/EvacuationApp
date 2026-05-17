package com.example.evacuationapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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

public class VerifyCodeActivity extends AppCompatActivity {

    private EditText etCode;
    private Button btnVerify;
    private TextView tvMessage;
    private String phoneNumber;
    private String userName;
    private String userEmail;
    private String existingRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_code);

        etCode = findViewById(R.id.etCode);
        btnVerify = findViewById(R.id.btnVerify);
        tvMessage = findViewById(R.id.tvMessage);

        phoneNumber = getIntent().getStringExtra("phone");
        userName = getIntent().getStringExtra("name");
        userEmail = getIntent().getStringExtra("email");
        existingRole = getIntent().getStringExtra("role");

        // Отображаем, куда отправлен код
        if (userEmail != null && !userEmail.isEmpty()) {
            tvMessage.setText("Код отправлен на " + userEmail);
        } else {
            tvMessage.setText("Код отправлен на вашу почту");
        }

        btnVerify.setOnClickListener(v -> verifyCode());
    }

    private void verifyCode() {
        String code = etCode.getText().toString().trim();
        if (code.isEmpty() || code.length() < 6) {
            Toast.makeText(this, "Введите 6-значный код", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> body = new HashMap<>();
        body.put("phone", phoneNumber);
        body.put("code", code);

        Call<Map<String, Object>> call = RetrofitClient.getApiService().verifyCode(body);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    // Если пользователь новый (есть имя) → на выбор роли
                    if (userName != null && !userName.isEmpty()) {
                        Intent intent = new Intent(VerifyCodeActivity.this, RoleSelectionActivity.class);
                        intent.putExtra("phone", phoneNumber);
                        intent.putExtra("name", userName);
                        intent.putExtra("email", userEmail);
                        startActivity(intent);
                        finish();
                    } else if (existingRole != null) {
                        // Существующий пользователь → вход
                        loginWithRole(phoneNumber, existingRole);
                    } else {
                        checkUserAndLogin();
                    }
                } else {
                    Toast.makeText(VerifyCodeActivity.this, "Неверный код", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(VerifyCodeActivity.this, "Нет связи с сервером", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkUserAndLogin() {
        Call<Map<String, Object>> call = RetrofitClient.getApiService().checkUserExists(phoneNumber);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    boolean exists = (boolean) response.body().get("exists");
                    if (exists) {
                        String role = (String) response.body().get("role");
                        loginWithRole(phoneNumber, role);
                    } else {
                        Intent intent = new Intent(VerifyCodeActivity.this, RoleSelectionActivity.class);
                        intent.putExtra("phone", phoneNumber);
                        startActivity(intent);
                        finish();
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(VerifyCodeActivity.this, "Ошибка проверки", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginWithRole(String phone, String role) {
        Map<String, String> body = new HashMap<>();
        body.put("phone", phone);
        body.put("role", role);
        body.put("name", userName != null ? userName : "");

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

                    PreferenceManager prefManager = new PreferenceManager(VerifyCodeActivity.this);
                    prefManager.saveToken(token);
                    prefManager.saveUserId(userId);

                    Intent intent;
                    if ("client".equals(userRole)) {
                        intent = new Intent(VerifyCodeActivity.this, ClientMainActivity.class);
                    } else {
                        intent = new Intent(VerifyCodeActivity.this, DriverMainActivity.class);
                    }
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(VerifyCodeActivity.this, "Ошибка входа", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(VerifyCodeActivity.this, "Нет связи с сервером", Toast.LENGTH_SHORT).show();
            }
        });
    }
}