package com.example.evacuationapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Вариант с setClassName
                Intent intent = new Intent();
                intent.setClassName("com.example.evacuationapp",
                        "com.example.evacuationapp.auth.LoginActivity");
                startActivity(intent);
                finish();
            }
        }, 2000);
    }
}