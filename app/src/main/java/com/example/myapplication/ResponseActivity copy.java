package com.example.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class ResponseActivity extends AppCompatActivity {

    private static final int NOTIFICATION_TIMEOUT = 5000; // 15 seconds
    private double latitude;
    private double longitude;
    private String username;
    private String password;
    private AuthRepository authRepository;
    private String accessToken;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_response);

        latitude = getIntent().getDoubleExtra("latitude", 0.0);
        longitude = getIntent().getDoubleExtra("longitude", 0.0);
        accessToken = getIntent().getStringExtra("access_token");
        username = getIntent().getStringExtra("username");
        password = getIntent().getStringExtra("password");
        Log.d("response act",accessToken);

        Button yesButton = findViewById(R.id.yesButton);
        Button noButton = findViewById(R.id.noButton);

        authRepository = new AuthRepository();

        yesButton.setOnClickListener(v -> {
            Toast.makeText(ResponseActivity.this, "Yes button pressed", Toast.LENGTH_SHORT).show();
            handleYesAction();
            cancelTimeout();
        });

        noButton.setOnClickListener(v -> {
            Toast.makeText(ResponseActivity.this, "No button pressed", Toast.LENGTH_SHORT).show();
            handleNoAction();
            cancelTimeout();
        });

        // Start the timeout countdown
        startTimeoutCountdown();
    }

    private void startTimeoutCountdown() {
        handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(this::handleYesAction, NOTIFICATION_TIMEOUT);
    }

    private void handleYesAction() {
        Log.d("ResponseActivity", "Handling Yes Action");
        enqueueLocationWork("Yes");
        MainActivity.sendSms(ResponseActivity.this);
    }

    private void handleNoAction() {
        Log.d("ResponseActivity", "Handling No Action");
        enqueueLocationWork("No");
    }

    private void enqueueLocationWork(String buttonPressed) {
        Data inputData = new Data.Builder()
                .putDouble("latitude", latitude)
                .putDouble("longitude", longitude)
                .putString("button", buttonPressed)
                .putString("accessToken", accessToken)
                .build();

        OneTimeWorkRequest locationWorkRequest = new OneTimeWorkRequest.Builder(LocationWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(this).enqueue(locationWorkRequest);
        Toast.makeText(this, "Data enqueued", Toast.LENGTH_SHORT).show();
    }

    private void cancelTimeout() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimeout();
    }
}
