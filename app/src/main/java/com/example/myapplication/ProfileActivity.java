package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ProfileActivity extends AppCompatActivity {

    private TextView usernameTextView;
    private TextView emailTextView;
    private ImageView userLogoImageView;
    private Button addPhoneNumberButton;
    private LinearLayout linearLayoutPhoneNumbers;
    private Button savePhoneNumbersButton;
    private ScrollView scrollViewPhoneNumbers;
    private Spinner timeSpinner;

    private SharedPreferences sharedPreferences;
    private static final String SHARED_PREFS_KEY = "MyAppPrefs";
    private static final String PHONE_NUMBERS_KEY = "phone_numbers";
    private static final String SELECTED_TIME_KEY = "selected_time";
    private String email;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize views
        usernameTextView = findViewById(R.id.usernameTextView);
        emailTextView = findViewById(R.id.emailTextView);
        userLogoImageView = findViewById(R.id.userLogo);
        addPhoneNumberButton = findViewById(R.id.buttonAddPhoneNumber);
        linearLayoutPhoneNumbers = findViewById(R.id.linearLayoutPhoneNumbers);
        savePhoneNumbersButton = findViewById(R.id.savePhoneNumbersButton);
        timeSpinner = findViewById(R.id.timeSpinner);
        scrollViewPhoneNumbers = findViewById(R.id.scrollViewPhoneNumbers);

        sharedPreferences = getSharedPreferences(SHARED_PREFS_KEY, MODE_PRIVATE);

        email = getIntent().getStringExtra("username");
        emailTextView.setText(email);

        boolean newSignup = getIntent().getBooleanExtra("newSignup", false);
        if (newSignup) {
            Toast.makeText(ProfileActivity.this, "Fill Your Information ", Toast.LENGTH_SHORT).show();
        }
        fetchProfileData(email);

        // Set click listeners
        addPhoneNumberButton.setOnClickListener(v -> addPhoneNumberField());
        savePhoneNumbersButton.setOnClickListener(v -> savePhoneNumbers());

        // Load cached phone numbers
        Set<String> phoneNumbers = loadPhoneNumbers();
        for (String phoneNumber : phoneNumbers) {
            addPhoneNumberField(phoneNumber);
        }

        setupTimeSpinner();
    }

    private void setupTimeSpinner() {
        // Options for time chooser
        String[] timeOptions = {"30 sec", "45 sec", "60 sec", "90 sec", "120 sec"};

        // Create an ArrayAdapter using the custom spinner item layout
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, timeOptions);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

        // Apply the adapter to the spinner
        timeSpinner.setAdapter(adapter);

        // Select the saved time, if any
        int selectedTime = getSelectedTime();
        if (selectedTime != -1) {
            int position = getPositionForTime(selectedTime);
            if (position != -1) {
                timeSpinner.setSelection(position);
            }
        }

        // Handle spinner item selection
        timeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selectedTime = getTimeForPosition(position);
                saveSelectedTime(selectedTime);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private int getPositionForTime(int time) {
        switch (time) {
            case 30:
                return 0;
            case 45:
                return 1;
            case 60:
                return 2;
            case 90:
                return 3;
            case 120:
                return 4;
            default:
                return -1;
        }
    }

    private int getTimeForPosition(int position) {
        switch (position) {
            case 0:
                return 30;
            case 1:
                return 45;
            case 2:
                return 60;
            case 3:
                return 90;
            case 4:
                return 120;
            default:
                return -1;
        }
    }

    private void fetchProfileData(String email) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://api.example.com/getData/" + email)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String responseData = response.body().string();
                    Log.d("Data Profile", responseData);
                    ProfileActivity.this.runOnUiThread(() -> {
                        try {
                            JSONObject json = new JSONObject(responseData);
                            String username = json.getString("username");
                            String email = json.getString("email");
                            String profileImageUrl = json.getString("profileImageUrl");

                            usernameTextView.setText(username);
                            emailTextView.setText(email);

                            Glide.with(ProfileActivity.this)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.ic_profile) // Placeholder image while loading
                                    .error(R.drawable.ic_profile) // Error image if loading fails
                                    .into(userLogoImageView);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        });
    }

    private void addPhoneNumberField() {
        addPhoneNumberField("");
    }

    private void addPhoneNumberField(String phoneNumber) {
        EditText phoneNumberEditText = new EditText(this);
        phoneNumberEditText.setHintTextColor(getResources().getColor(R.color.white)); // Set hint text color to white
        phoneNumberEditText.setTextColor(getResources().getColor(R.color.white));
        phoneNumberEditText.setHint("Enter phone number");
        phoneNumberEditText.setText(phoneNumber);
        linearLayoutPhoneNumbers.addView(phoneNumberEditText);
    }

    private void savePhoneNumbers() {
        Set<String> phoneNumbers = new HashSet<>();
        int childCount = linearLayoutPhoneNumbers.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = linearLayoutPhoneNumbers.getChildAt(i);
            if (child instanceof EditText) {
                String newPhoneNumber = ((EditText) child).getText().toString().trim();
                if (!newPhoneNumber.isEmpty()) {
                    phoneNumbers.add(newPhoneNumber);
                    Log.d("Saved Phone Number", newPhoneNumber);
                }
            }
        }
        // Save phone numbers locally
        savePhoneNumbersLocally(phoneNumbers);
        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
        intent.putExtra("username",email);
        startActivity(intent);

    }

    private Set<String> loadPhoneNumbers() {
        return sharedPreferences.getStringSet(PHONE_NUMBERS_KEY, new HashSet<>());
    }

    private void savePhoneNumbersLocally(Set<String> phoneNumbers) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(PHONE_NUMBERS_KEY, phoneNumbers);
        editor.apply();
    }

    private void saveSelectedTime(int selectedTime) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(SELECTED_TIME_KEY, selectedTime);
        editor.apply();
    }

    private int getSelectedTime() {
        return sharedPreferences.getInt(SELECTED_TIME_KEY, -1);
    }
}
