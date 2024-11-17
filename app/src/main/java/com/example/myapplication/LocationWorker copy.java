package com.example.myapplication;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.example.myapplication.BuildConfig;


public class LocationWorker extends Worker {

    private ApiService apiService;

    public LocationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.SERVER_URL) // Base URL for your API
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    @NonNull
    @Override
    public Result doWork() {
        double latitude = getInputData().getDouble("latitude", 0.0);
        double longitude = getInputData().getDouble("longitude", 0.0);
        String button = getInputData().getString("button");
        String accessToken = getInputData().getString("accessToken");

        Log.d("Worker access:", button);
        Log.d("Worker Token", accessToken);
        LocationData locationData = new LocationData(latitude, longitude, button);

        try {
            String tokenWithBearer = "Bearer " + accessToken; // Add "Bearer" prefix
            Call<Void> call = apiService.sendLocationData( locationData);
            Response<Void> response = call.execute();
            if (response.isSuccessful()) {
                return Result.success();
            } else {
                Log.d("Worker", "Work failed with response code: " + response.code() + " and message: " + response.message());
                return Result.retry();
            }
        } catch (Exception e) {
            Log.e("Worker", "Work failed with exception: " + e.getMessage(), e);
            return Result.retry();
        }
    }
}
