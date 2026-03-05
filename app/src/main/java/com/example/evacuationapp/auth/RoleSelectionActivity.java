package com.example.evacuationapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.evacuationapp.R;
import com.example.evacuationapp.client.ClientMainActivity;
import com.example.evacuationapp.driver.DriverMainActivity;

public class RoleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        String phone = getIntent().getStringExtra("phone");

        Button btnClient = findViewById(R.id.btnClient);
        Button btnDriver = findViewById(R.id.btnDriver);

        btnClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RoleSelectionActivity.this, ClientMainActivity.class);
                intent.putExtra("phone", phone);
                startActivity(intent);
            }
        });

        btnDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RoleSelectionActivity.this, DriverMainActivity.class);
                intent.putExtra("phone", phone);
                startActivity(intent);
            }
        });
    }
}