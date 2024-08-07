package com.example.myapplication;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiServiceObject {
    @POST("/objectDetection")
    Call<Void> sendObjectData(@Body ObjectData objectData);
}
