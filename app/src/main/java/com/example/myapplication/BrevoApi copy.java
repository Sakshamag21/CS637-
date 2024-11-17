package com.example.myapplication;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import com.example.myapplication.BuildConfig;


public interface BrevoApi {
    @Headers({
            "Accept: application/json",
            "Content-Type: application/json",
            "api-key: " + BuildConfig.BREVO_API_KEY
    })
    @POST("smtp/email")
    Call<EmailResponse> sendEmail(@Body EmailRequest emailRequest);
}
