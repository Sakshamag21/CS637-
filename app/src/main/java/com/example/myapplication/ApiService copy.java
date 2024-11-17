package com.example.myapplication;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ApiService {
    @POST("/api/accidents/locations/")
    Call<Void> sendLocationData(
            // @Header("Authorization") String token,
            @Body LocationData locationData
    );
}
