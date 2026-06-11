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
import com.example.evacuationapp.network.RetrofitClient;
import com.example.evacuationapp.utils.PreferenceManager;  // ← добавить импорт
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etPhone, etEmail;
    private Button btnRegister;
    private TextView tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        btnRegister = findViewById(R.id.btnRegister);
        tvError = findViewById(R.id.tvError);

        // Получаем номер из Intent (передан с LoginActivity)
        String phoneFromIntent = getIntent().getStringExtra("phone");
        if (phoneFromIntent != null && !phoneFromIntent.isEmpty()) {
            etPhone.setText(phoneFromIntent);
            etPhone.setEnabled(false);
        }

        btnRegister.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String email = etEmail.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                showError("Введите имя");
                return;
            }
            if (TextUtils.isEmpty(phone)) {
                showError("Введите номер телефона");
                return;
            }
            if (phone.length() != 10) {
                showError("Введите 10 цифр (например, 9123456789)");
                return;
            }
            if (TextUtils.isEmpty(email)) {
                showError("Введите email");
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showError("Введите корректный email");
                return;
            }

            registerUser(phone, name, email);
        });
    }

    private void registerUser(String phone, String name, String email) {
        btnRegister.setEnabled(false);
        btnRegister.setText("Регистрация...");

        // Сначала отправляем код
        Map<String, String> body = new HashMap<>();
        body.put("phone", phone);
        body.put("name", name);
        body.put("email", email);
        body.put("role", "client");

        Call<Map<String, Object>> call = RetrofitClient.getApiService().sendCode(body);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                btnRegister.setEnabled(true);
                btnRegister.setText("Зарегистрироваться");

                if (response.isSuccessful()) {
                    // Код отправлен, переходим на экран подтверждения
                    Intent intent = new Intent(RegisterActivity.this, VerifyCodeActivity.class);
                    intent.putExtra("phone", phone);
                    intent.putExtra("name", name);
                    intent.putExtra("email", email);
                    startActivity(intent);
                    finish();
                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        if (errorBody.contains("email уже существует")) {
                            showError("Пользователь с таким email уже зарегистрирован");
                        } else {
                            showError("Ошибка: " + errorBody);
                        }
                    } catch (Exception e) {
                        showError("Ошибка отправки кода");
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                btnRegister.setEnabled(true);
                btnRegister.setText("Зарегистрироваться");
                showError("Нет связи с сервером");
            }
        });
    }
    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }
}