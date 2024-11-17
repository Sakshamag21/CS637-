package com.example.myapplication;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiServiceUser {

    @POST("register/") // Modified path for signup
    Call<UserData.ApiResponse> signup(@Body UserData.SignupRequest signupRequest);

    @POST("login/") // Modified path for login
    Call<UserData.ApiResponse> login(@Body UserData.LoginRequest loginRequest);
}
