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

public class LoginActivity extends AppCompatActivity {

    private EditText etPhone;
    private Button btnLogin;
    private TextView tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etPhone = findViewById(R.id.etPhone);
        btnLogin = findViewById(R.id.btnLogin);   // ← используем id из XML
        tvError = findViewById(R.id.tvError);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phone = etPhone.getText().toString().trim();

                if (TextUtils.isEmpty(phone)) {
                    showError("Введите номер телефона");
                    return;
                }

                if (phone.length() < 10) {
                    showError("Неверный формат номера");
                    return;
                }

                Intent intent = new Intent(LoginActivity.this, RoleSelectionActivity.class);
                intent.putExtra("phone", phone);
                startActivity(intent);
            }
        });
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }
}