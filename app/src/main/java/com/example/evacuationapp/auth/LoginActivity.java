package com.example.evacuationapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.evacuationapp.R;
import com.example.evacuationapp.network.RetrofitClient;
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
            if (phone.length() != 10) {
                showError("Введите 10 цифр (например, 9123456789)");
                return;
            }
            checkUserExists(phone);
        });
    }

    private void checkUserExists(String phone) {
        btnLogin.setEnabled(false);
        btnLogin.setText("Проверка...");

        Call<Map<String, Object>> call = RetrofitClient.getApiService().checkUserExists(phone);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Войти");

                if (response.isSuccessful() && response.body() != null) {
                    boolean exists = (boolean) response.body().get("exists");
                    if (exists) {
                        String role = (String) response.body().get("role");
                        sendCode(phone, role);
                    } else {
                        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                        intent.putExtra("phone", phone);
                        startActivity(intent);
                    }
                } else {
                    showError("Ошибка проверки. Попробуйте позже.");
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Войти");
                showError("Нет связи с сервером. Проверьте интернет.");
            }
        });
    }

    private void sendCode(String phone, String role) {
        Map<String, String> body = new HashMap<>();
        body.put("phone", phone);
        body.put("role", role);

        Call<Map<String, Object>> call = RetrofitClient.getApiService().sendCode(body);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Intent intent = new Intent(LoginActivity.this, VerifyCodeActivity.class);
                    intent.putExtra("phone", phone);
                    intent.putExtra("role", role);
                    startActivity(intent);
                } else {
                    showError("Ошибка отправки кода");
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