package com.example.myapplication;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.PUT;

public interface ApiService {
    @POST("/location")
    Call<Void> sendLocationData(@Body LocationData locationData);

    @PUT("/updateUserData")
    Call<Void> updateUserData(@Body UserData.UserDataPhone userDataPhone) ;
}
