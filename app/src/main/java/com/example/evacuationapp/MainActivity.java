package com.example.evacuationapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.example.evacuationapp.auth.LoginActivity;
import com.example.evacuationapp.client.ClientMainActivity;
import com.example.evacuationapp.driver.DriverMainActivity;
import com.example.evacuationapp.utils.PreferenceManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                PreferenceManager prefManager = new PreferenceManager(MainActivity.this);
                if (prefManager.isLoggedIn()) {
                    long userId = prefManager.getUserId();
                    String role = prefManager.getUserRole();
                    Intent intent;
                    if ("driver".equals(role)) {
                        intent = new Intent(MainActivity.this, DriverMainActivity.class);
                    } else {
                        intent = new Intent(MainActivity.this, ClientMainActivity.class);
                    }
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                } else {
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                }
                finish();
            }
        }, 2000);
    }
}