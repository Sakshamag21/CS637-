package com.example.myapplication;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ObjectWorker extends Worker {

    private ApiServiceObject apiService;

    public ObjectWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://44d4-14-139-38-139.ngrok-free.app/") // Replace with your server IP
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiServiceObject.class);
    }

    @NonNull
    @Override
    public Result doWork() {
//        Map<String, Object> objectData = getInputData().getKeyValueMap();
//        String json = new Gson().toJson(objectData);
        String json= getInputData().getString("objectDataVal");
        Log.d("Object Worker",json);

        Data inputData = new Data.Builder()
                .putString("objectData", json)
                .build();

        try {
            String jsonString = inputData.getString("objectData");

            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> objectDataMap = new Gson().fromJson(jsonString, type);

            ObjectData objectDataObject = new ObjectData(objectDataMap);

            Call<Void> call = apiService.sendObjectData(objectDataObject);
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
