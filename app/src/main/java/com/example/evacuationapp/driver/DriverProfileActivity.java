package com.example.evacuationapp.driver;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.evacuationapp.R;
import com.example.evacuationapp.network.RetrofitClient;
import com.example.evacuationapp.utils.PreferenceManager;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverProfileActivity extends AppCompatActivity {

    private EditText etCarModel, etCarNumber;
    private Button btnSave;
    private long driverId;

    // Регулярное выражение для российского госномера
    // Форматы: А999АА99, А999АА999, А999АА99RUS, А999АА999RUS
    private static final Pattern RUSSIAN_LICENSE_PLATE_PATTERN =
            Pattern.compile("^[АВЕКМНОРСТУХ]\\d{3}[АВЕКМНОРСТУХ]{2}\\d{2,3}(RUS)?$", Pattern.CASE_INSENSITIVE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_profile);

        driverId = new PreferenceManager(this).getUserId();

        etCarModel = findViewById(R.id.etCarModel);
        etCarNumber = findViewById(R.id.etCarNumber);
        btnSave = findViewById(R.id.btnSave);

        btnSave.setOnClickListener(v -> saveDriverProfile());
    }

    private void saveDriverProfile() {
        String carModel = etCarModel.getText().toString().trim();
        String carNumber = etCarNumber.getText().toString().trim().toUpperCase();

        if (carModel.isEmpty()) {
            etCarModel.setError("Введите модель эвакуатора");
            return;
        }

        if (carNumber.isEmpty()) {
            etCarNumber.setError("Введите госномер");
            return;
        }

        // Проверка российского госномера
        if (!isValidRussianPlate(carNumber)) {
            etCarNumber.setError("Введите номер в формате: А999АА99 или А999АА999");
            Toast.makeText(this, "Пример: А123ВС77 или А123ВС177", Toast.LENGTH_LONG).show();
            return;
        }

        // Сохраняем данные в БД через сервер
        Map<String, String> body = new HashMap<>();
        body.put("carModel", carModel);
        body.put("carNumber", carNumber);

        Call<Void> call = RetrofitClient.getApiService().updateDriverProfile(driverId, body);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Помечаем, что профиль заполнен
                    PreferenceManager prefManager = new PreferenceManager(DriverProfileActivity.this);
                    prefManager.setDriverProfileFilled(true);

                    Toast.makeText(DriverProfileActivity.this, "Данные сохранены!", Toast.LENGTH_SHORT).show();

                    // Переход на главный экран водителя
                    Intent intent = new Intent(DriverProfileActivity.this, DriverMainActivity.class);
                    intent.putExtra("userId", driverId);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(DriverProfileActivity.this, "Ошибка сохранения", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(DriverProfileActivity.this, "Нет связи с сервером", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Проверяет, соответствует ли строка формату российского госномера
     * Форматы:
     * - А999АА99 (буква, 3 цифры, 2 буквы, 2 цифры)
     * - А999АА999 (буква, 3 цифры, 2 буквы, 3 цифры)
     * - А999АА99RUS (с регионом RUS)
     * - А999АА999RUS (с регионом RUS)
     *
     * Допустимые буквы: А, В, Е, К, М, Н, О, Р, С, Т, У, Х
     */
    private boolean isValidRussianPlate(String plate) {
        if (plate == null || plate.isEmpty()) {
            return false;
        }

        // Убираем возможные пробелы и приводим к верхнему регистру
        String cleanPlate = plate.replaceAll("\\s+", "").toUpperCase();

        return RUSSIAN_LICENSE_PLATE_PATTERN.matcher(cleanPlate).matches();
    }
}