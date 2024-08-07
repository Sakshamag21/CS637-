package com.example.myapplication;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiServiceUser {

    @POST("postUserData")
    Call<UserData.ApiResponse> signup(@Body UserData.SignupRequest signupRequest);

    @POST("getUserData")
    Call<UserData.ApiResponse> login(@Body UserData.LoginRequest loginRequest);
}
