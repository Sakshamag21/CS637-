package com.example.myapplication;

import android.util.Log;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.example.myapplication.BuildConfig;


public class AuthRepository {

    private ApiServiceUser apiService;

    public AuthRepository() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.SERVER_URL + "api/accounts/") // Replace with your backend URL
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiServiceUser.class);
    }

    public void signup(String username, String password, String email, final AuthCallback callback) {
        Log.d("Data received user", username);
        UserData.SignupRequest request = new UserData.SignupRequest(username, password, email);
        Call<UserData.ApiResponse> call = apiService.signup(request);

        call.enqueue(new Callback<UserData.ApiResponse>() {
            @Override
            public void onResponse(Call<UserData.ApiResponse> call, Response<UserData.ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().getAccess());
                } else {
                    callback.onError("Signup failed.");
                }
            }

            @Override
            public void onFailure(Call<UserData.ApiResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void login(String username, String password, final AuthCallback callback) {
        Log.d("Auth repo", username);
        UserData.LoginRequest request = new UserData.LoginRequest(username, password);
        Call<UserData.ApiResponse> call = apiService.login(request);

        call.enqueue(new Callback<UserData.ApiResponse>() {
            @Override
            public void onResponse(Call<UserData.ApiResponse> call, Response<UserData.ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String accessToken = response.body().getAccess(); // Get the access token
                    Log.d("Access Token", accessToken);

                    Log.d("response  login", "success");
                    callback.onSuccess(accessToken);
                } else {
                    callback.onError("Login failed.");
                }
            }

            @Override
            public void onFailure(Call<UserData.ApiResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public interface AuthCallback {
        String onSuccess(String accessToken); // Change to return access token
        void onError(String error);
    }
}
