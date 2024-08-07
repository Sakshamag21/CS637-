package com.example.myapplication;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface BrevoApi {
    @Headers({
            "Accept: application/json",
            "Content-Type: application/json",
            "api-key:   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" // Replace with your Brevo API key
    })
    @POST("smtp/email")
    Call<EmailResponse> sendEmail(@Body EmailRequest emailRequest);
}
