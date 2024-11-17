package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class SignupActivity extends AppCompatActivity implements OTPDialogFragment.OTPDialogListener {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private EditText emailEditText;
    private Button signupButton;
    private TextView loginTextView;

    private AuthRepository authRepository;
    private String generatedOtp;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        emailEditText = findViewById(R.id.emailEditText);
        signupButton = findViewById(R.id.signupButton);
        loginTextView = findViewById(R.id.loginTextView);

        authRepository = new AuthRepository();

        signupButton.setOnClickListener(v -> sendOtp());
        loginTextView.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }

    private void sendOtp() {
        String email = emailEditText.getText().toString();

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        generatedOtp = generateOtp();
        // Send OTP to email
        new SendEmailTask().execute(email, generatedOtp);
    }

    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // Generates a 6-digit OTP
        return String.valueOf(otp);
    }

    private void sendOtpEmail(String email, String otp) {
        Retrofit retrofit = ApiClient.getClient();
        BrevoApi brevoApi = retrofit.create(BrevoApi.class);

        EmailRequest.Sender sender = new EmailRequest.Sender("Your Name", "your_email@example.com");
        List<EmailRequest.Recipient> recipients = new ArrayList<>();
        recipients.add(new EmailRequest.Recipient(email));

        EmailRequest emailRequest = new EmailRequest(sender, recipients, "Your OTP Code", "Your OTP code is: " + otp);

        Call<EmailResponse> call = brevoApi.sendEmail(emailRequest);
        call.enqueue(new Callback<EmailResponse>() {
            @Override
            public void onResponse(Call<EmailResponse> call, Response<EmailResponse> response) {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(SignupActivity.this, "OTP sent to your email", Toast.LENGTH_SHORT).show();
                        showOtpDialog();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(SignupActivity.this, "Failed to send OTP. Please try again.", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onFailure(Call<EmailResponse> call, Throwable t) {
                runOnUiThread(() -> Toast.makeText(SignupActivity.this, "Error sending email. Check your internet connection.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showOtpDialog() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        OTPDialogFragment otpDialogFragment = new OTPDialogFragment();
        otpDialogFragment.show(fragmentManager, "otp_dialog");
    }

    @Override
    public void onVerifyOtp(String otp) {
        if (otp.equals(generatedOtp)) {
            signup();
        } else {
            Toast.makeText(this, "Invalid OTP. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private class SendEmailTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            String email = params[0];
            String otp = params[1];
            try {
                sendOtpEmail(email, otp);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (!success) {
                Toast.makeText(SignupActivity.this, "Failed to send OTP. Please try again.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void signup() {
        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String email = emailEditText.getText().toString();

        authRepository.signup(username, password, email, new AuthRepository.AuthCallback() {
            public void onSuccess(UserData.ApiResponse response) {
                Toast.makeText(SignupActivity.this, "Signup successful: " + response.getMessage(), Toast.LENGTH_SHORT).show();
                // Redirect to the ProfileActivity
                redirectToProfileActivity(email);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(SignupActivity.this, "Signup error: " + error, Toast.LENGTH_SHORT).show();
            }

            @Override
            public String onSuccess(String token) {
                Log.d("SignupActivity", "Received Token: " + token);
                redirectToProfileActivity(email);
                return token;
            }
        });
    }

    private void redirectToProfileActivity(String email) {
        Intent intent = new Intent(SignupActivity.this, ProfileActivity.class);
        intent.putExtra("newSignup", true);
        intent.putExtra("username", email);
        startActivity(intent);
        finish();
    }
}
