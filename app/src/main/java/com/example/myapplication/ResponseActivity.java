package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class ResponseActivity extends AppCompatActivity {

    private double latitude;
    private double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_response);

        latitude = getIntent().getDoubleExtra("latitude", 0.0);
        longitude = getIntent().getDoubleExtra("longitude", 0.0);

        Button yesButton = findViewById(R.id.yesButton);
        Button noButton = findViewById(R.id.noButton);

        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ResponseActivity.this, "Yes button pressed", Toast.LENGTH_SHORT).show();
                enqueueLocationWork("Yes");
                MainActivity.sendSms(ResponseActivity.this);
            }
        });

        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ResponseActivity.this, "No button pressed", Toast.LENGTH_SHORT).show();
                enqueueLocationWork("No");
            }
        });

    }

    private void enqueueLocationWork(String buttonPressed) {
        Data inputData = new Data.Builder()
                .putDouble("latitude", latitude)
                .putDouble("longitude", longitude)
                .putString("button", buttonPressed)
                .build();

        OneTimeWorkRequest locationWorkRequest = new OneTimeWorkRequest.Builder(LocationWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(this).enqueue(locationWorkRequest);
        Toast.makeText(this, "Data enqueued", Toast.LENGTH_SHORT).show();
    }
}
