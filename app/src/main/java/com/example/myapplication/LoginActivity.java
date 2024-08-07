package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView signupTextView;
//    private Button googleSignInButton;

    private AuthRepository authRepository;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    private static final String TAG = "LoginActivity";
    private static final String PREFS_NAME = "MyAppPrefs";
    private static final String PREF_KEY_LOGGED_IN = "isLoggedIn";
    private static final String PREF_KEY_USERNAME = "username";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        signupTextView = findViewById(R.id.signupTextView);
//        googleSignInButton = findViewById(R.id.googleSignInButton);

        authRepository = new AuthRepository();

        // Check if user is already logged in
        if (isLoggedIn()) {
            String username = getUsername();
            redirectToMainActivity(username);
            return;
        }

        loginButton.setOnClickListener(v -> login());

        signupTextView.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

//        googleSignInButton.setOnClickListener(v -> signInWithGoogle());

        googleSignInLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        handleGoogleSignInResult(task);
                    }
                });
    }

    private boolean isLoggedIn() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(PREF_KEY_LOGGED_IN, false);
    }

    private String getUsername() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(PREF_KEY_USERNAME, "");
    }

    private void setLoggedIn(boolean isLoggedIn, String username) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREF_KEY_LOGGED_IN, isLoggedIn);
        editor.putString(PREF_KEY_USERNAME, username);
        editor.apply();
    }

    private void clearLoggedIn() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(PREF_KEY_LOGGED_IN);
        editor.remove(PREF_KEY_USERNAME);
        editor.apply();
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            // Signed in successfully, authenticate with Firebase
            // firebaseAuthWithGoogle(account);
            String displayName = account.getDisplayName();
            // Save login state
            handleSignInSuccess(displayName);
        } catch (ApiException e) {
            // Google Sign In failed, update UI appropriately
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            Toast.makeText(LoginActivity.this, "Google Sign In failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSignInSuccess(String username) {
        setLoggedIn(true, username);
        redirectToMainActivity(username);
    }

    private void login() {
        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        authRepository.login(username, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(UserData.ApiResponse response) {
                Toast.makeText(LoginActivity.this, "Login successful: " + response.getMessage(), Toast.LENGTH_SHORT).show();
                // Handle successful sign-in
                handleSignInSuccess(username);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(LoginActivity.this, "Login error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void redirectToMainActivity(String username) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("username", username);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clear login state on logout if needed
//        clearLoggedIn();
    }
}
