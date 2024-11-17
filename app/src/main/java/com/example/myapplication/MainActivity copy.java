package com.example.myapplication;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
//import android.widget.Toolbar;
import androidx.appcompat.widget.Toolbar; // Import the correct Toolbar class

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.content.SharedPreferences;
import com.google.firebase.FirebaseApp;



import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.example.myapplication.BuildConfig;


import com.google.gson.Gson;

import org.tensorflow.lite.support.image.TensorImage;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final float SUDDEN_DECREASE_THRESHOLD = 5.0f;
    private static final String CHANNEL_ID = "location_alerts";
    private static final int REQUEST_PERMISSIONS = 1;

    private LocationManager locationManager;
    private TextView locationTextView;
    private TextView speedTextView;
    private LocationListener locationListener;
    private Location previousLocation;
    private long previousTime;
    private float previousSpeedKmH;

    private int testSpeed=0;
    private NotificationManager notificationManager;
    private FallDetectionHelper fallDetectionHelper;

    private TextView usernameTextView;
    private ImageView userLogo;

    private static final String TAG = "ObjectDetection";

    private ImageButton navigateToProfileButton;

    private ExecutorService videoExecutorService;
    private AuthRepository authRepository;

    private ExecutorService locationExecutorService;

    private ExecutorService mlModelExecutorService;

    private static SmsHelper smsHelper;

    private TFLiteModel model;
    private TextView angularVelocityTextView;
    private Button startButton;
    private Button stopButton;
    private VideoHelper videoHelper;
    private ApiService apiService;


    private static final int REQUEST_SMS_PERMISSION = 1;

    private static final String MESSAGE = "Your constant message here";
    private  String[] PHONE_NUMBERS = {"+918279942685"};

    private ImageButton profileButton;

    private NumberPlateDetectionHelper detectionHelper;

    private Queue<Bitmap> frameQueue = new LinkedList<>();
    private String accessToken;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);
        apiService = ApiClientUserUpdation.getClient().create(ApiService.class);
        Log.d("Server url",BuildConfig.SERVER_URL);


        String username = getIntent().getStringExtra("username");
        String password = getIntent().getStringExtra("password");
        Boolean isLoggedIn = getIntent().getBooleanExtra("isLoggedIn",false);
        accessToken = getIntent().getStringExtra("access_token");
        Toast.makeText(this, "Welcome " + username, Toast.LENGTH_SHORT).show();
        Log.d("Main user",username);
        Log.d("Access token", accessToken);
        Log.d("Login or not",isLoggedIn.toString());


        initializeViews();
        initializeManagers();
        initializeToolbar(username);
        initializeLocationListener(accessToken);
        createNotificationChannel();
        videoHelper = new VideoHelper(this, frameQueue);
        smsHelper = new SmsHelper(this);
        ImageButton navigateToProfileButton = findViewById(R.id.navigateToProfileButton);
        navigateToProfileButton.setOnClickListener(v -> showPopupMenu(v, username));



        videoExecutorService = Executors.newSingleThreadExecutor();
        locationExecutorService = Executors.newSingleThreadExecutor();
        mlModelExecutorService = Executors.newSingleThreadExecutor();




        startButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                requestLocationUpdates();
                initializeFallDetection();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            }
            videoExecutorService.execute(()->{
                if (checkPermissionsCamera()) {
                    startVideoRecording();
                } else {
                    requestPermissionsCamera();
                }
            });
            mlModelExecutorService.execute(()->{
                loadModelAndRunInference();
                detectionHelper = new NumberPlateDetectionHelper();
                detectionHelper.initializeModels(this,frameQueue);

            });
//
//
        });

        stopButton.setOnClickListener(v -> {
            // Stop location updates and fall detection
            locationManager.removeUpdates(locationListener);
            stopFallDetection();

            // Stop video recording and frame extraction
            videoExecutorService.execute(() -> {
                videoHelper.stopRecording();
                videoHelper.stopFrameExtraction();
            });

            // Retrieve number plates from SharedPreferences
            SharedPreferences prefs = getApplicationContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
            Set<String> numberPlates = prefs.getStringSet("number_plates", new HashSet<>());
            numberPlates.add("Try value");
            Log.d("Detected Plates", numberPlates.toString());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("numberPlateString", new ArrayList<>(numberPlates));

            if(accessToken == "noToken"){
                Log.d(TAG, username);

                authRepository.login(username, "password123@", new AuthRepository.AuthCallback() {
                    @Override
                    public String onSuccess(String accessToken) {
                        Toast.makeText(MainActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                        // Handle successful sign-in and print token
                        return accessToken;
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(MainActivity.this, "Login error: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            // Access token
            String accessTokenAuth = "Bearer " + accessToken; // Replace with your method to get the access token

            // Initialize Retrofit

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.SERVER_URL) // Replace with your actual base URL
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            NumberPlateApiService apiService = retrofit.create(NumberPlateApiService.class);

            Call<Void> call = apiService.sendNumberPlates( requestBody);
            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Log.d("API", "Number plates sent successfully");
                    } else {
                        Log.d("API", "Failed to send number plates: " + response.code() + " - " + response.message());
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Log.e("API", "Error sending number plates", t);
                }
            });
        });


    }

    private void showPopupMenu(View view, String username) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenu().add("Profile");
        popupMenu.getMenu().add("Signup");
        popupMenu.getMenu().add("Logout");

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getTitle().toString()) {
                case "Logout":
                    LogOut();
                    Intent logoutIntent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(logoutIntent);
                    return true;
                case "Profile":
                    Intent profileIntent = new Intent(MainActivity.this, ProfileActivity.class);
                    profileIntent.putExtra("username", username);
                    startActivity(profileIntent);
                    return true;
                case "Signup":
                    Intent signupIntent = new Intent(MainActivity.this, SignupActivity.class);
                    startActivity(signupIntent);
                    return true;
                default:
                    return false;
            }
        });

        popupMenu.show();
    }



    private void initializeViews() {
        locationTextView = findViewById(R.id.locationTextView);
        speedTextView = findViewById(R.id.speedTextView);
        angularVelocityTextView = findViewById(R.id.angularVelocityTextView);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        usernameTextView = findViewById(R.id.usernameTextView);
        userLogo = findViewById(R.id.userLogo);
    }

    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(MainActivity.this, view);
        popupMenu.getMenuInflater().inflate(R.menu.menu_user_dropdown, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
//                switch (item.getItemId()) {
//                    case R.id.menu_profile:
//                        navigateToProfile();
//                        return true;
//                    case R.id.menu_signup:
//                        navigateToSignup();
//                        return true;
//                    default:
//                        return false;
//                }
                Log.d("Profile click","Yes");
                return true;
            };
        });

        popupMenu.show();
    }
//    private void startVideoCapture() {
//
//        videoExecutorService.execute(() -> {
//            Intent intent = new Intent(MainActivity.this, VideoCaptureActivity.class);
//            intent.putExtra("recordingDuration", recordingDuration);
//            String username = getIntent().getStringExtra("username");
//            intent.putExtra("username", username);
//            startActivity(intent);
//        });
//
//    }

//    private void stopVideoCapture() {
//        videoExecutorService.execute(() -> {
//            Intent intent = new Intent(MainActivity.this, VideoCaptureActivity.class);
//            intent.setAction("STOP_RECORDING");
//            startActivity(intent);
//        });
//    }

    private void navigateToProfile() {
        Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
        String username = getIntent().getStringExtra("username");
        intent.putExtra("username", username);
        startActivity(intent);
    }

    private void navigateToSignup() {
        Intent intent = new Intent(MainActivity.this, SignupActivity.class);
        startActivity(intent);
    }

//    private void sendSms() {
//        Intent intent = new Intent(MainActivity.this, SendSmsActivity.class);
//        intent.putExtra("phoneNumbers", PHONE_NUMBERS);
//        intent.putExtra("message", MESSAGE);
//        String username = getIntent().getStringExtra("username");
//        intent.putExtra("username", username);
//        startActivity(intent);
//    }
    public static void sendSms(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        Set<String> phoneNumbers = prefs.getStringSet("phone_numbers", new HashSet<>());
        String[] PHONE_NUMBERS = phoneNumbers.toArray(new String[0]);
        smsHelper.sendSmsToMultipleNumbers(PHONE_NUMBERS, MESSAGE);
    }



    private void initializeToolbar(String email) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        usernameTextView.setText(email);
//        profileButton.setOnClickListener(v -> {
//            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
//            intent.putExtra("email",email);
//            startActivity(intent);
//        });
    }

    private void initializeManagers() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private void initializeFallDetection() {
        fallDetectionHelper = new FallDetectionHelper(this, angularVelocityTextView);
        locationExecutorService.execute(() -> {
            fallDetectionHelper.startListeningForFallDetection();
        });
    }

    private void stopFallDetection(){
        locationExecutorService.execute(() -> {
            fallDetectionHelper.stopListeningForFallDetection();
        });
    }


    private void loadModelAndRunInference() {
        try {
            TFLiteModel model = new TFLiteModel(this, "model_optimized6.tflite");
            Log.d("model error", "Image loading started");

            long startTime = SystemClock.uptimeMillis();
            float[][] result = model.runInference(this, "img.jpg");
            long endTime = SystemClock.uptimeMillis();
            long inferenceTime = endTime - startTime;
            Log.d("time", String.valueOf(inferenceTime));

            Map<Float, Integer> valueCounts = new HashMap<>();
            for (float[] row : result) {
                for (float value : row) {
                    if (value != -1) {
                        valueCounts.put(value, valueCounts.getOrDefault(value, 0) + 1);
                    } else {
                        break;
                    }
                }
            }
//            enqueueObjectWork(valueCounts);

            for (Map.Entry<Float, Integer> entry : valueCounts.entrySet()) {
                Log.d("UniqueValueCount", "Value: " + entry.getKey() + ", Count: " + entry.getValue());
            }
        } catch (IOException e) {
            Log.d("model error", "Failed to load model");
            throw new RuntimeException(e);
        }
    }


//    private void loadModalAndRunInterfaceNumberPlate() {
//
//    }

    private void initializeLocationListener(String accessToken) {
        Log.d("init location listner",accessToken);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                float speed = location.getSpeed();


                if (speed == 0.0f && previousLocation != null) {
                    long currentTime = System.currentTimeMillis();
                    float distance = location.distanceTo(previousLocation);
                    float timeDelta = (currentTime - previousTime) / 1000.0f;
                    speed = distance / timeDelta;
                }

                previousLocation = location;
                previousTime = System.currentTimeMillis();
                float currentSpeedKmH = speed * 3.6f;
                if(testSpeed == 3) {
                    sendNotification("Sudden Decrease Detected", "Previous speed: " + previousSpeedKmH + " km/h, Current speed: " + currentSpeedKmH + " km/h", accessToken);
                }
                testSpeed++;
                Log.d("Speed Test", String.valueOf(testSpeed));

//                if (previousSpeedKmH > 0 && previousSpeedKmH - currentSpeedKmH >= SUDDEN_DECREASE_THRESHOLD) {
//                    Log.d("Speed", "Sudden decrease detected! Previous speed: " + previousSpeedKmH + " km/h, Current speed: " + currentSpeedKmH + " km/h");
//                    sendNotification("Sudden Decrease Detected", "Previous speed: " + previousSpeedKmH + " km/h, Current speed: " + currentSpeedKmH + " km/h", accessToken);
//
//                }

                previousSpeedKmH = currentSpeedKmH;
                String locationString = "Latitude: " + latitude + "\nLongitude: " + longitude;
                String speedString = "Speed: " + currentSpeedKmH + " km/h";

                runOnUiThread(() -> {
                    locationTextView.setText(locationString);
                    speedTextView.setText(speedString);
                });

                Log.d("Location", locationString);
                Log.d("Speed", speedString);
//                enqueueLocationWork("0", latitude, longitude);
            }
        };
    }


//    private void enqueueLocationWork(String buttonPressed,Double latitude,Double longitude) {
//        Data inputData = new Data.Builder()
//                .putDouble("latitude", latitude)
//                .putDouble("longitude", longitude)
//                .putString("button", buttonPressed)
//                .build();
//
//        OneTimeWorkRequest locationWorkRequest = new OneTimeWorkRequest.Builder(LocationWorker.class)
//                .setInputData(inputData)
//                .build();
//
//        WorkManager.getInstance(this).enqueue(locationWorkRequest);
////        Toast.makeText(this, "Data enqueued", Toast.LENGTH_SHORT).show();
//        Log.d("Worker","Data enqueded");
//    }
//
//    private void enqueueObjectWork(Map<Float, Integer> objectDataVal) {
//        String json = new Gson().toJson(objectDataVal);
//        Log.d("Worker object","Started");
//
//        Data inputData = new Data.Builder()
//                .putString("objectDataVal", json)
//                .build();
//
//        OneTimeWorkRequest objectWorkRequest = new OneTimeWorkRequest.Builder(ObjectWorker.class)
//                .setInputData(inputData)
//                .build();
//
//        WorkManager.getInstance(this).enqueue(objectWorkRequest);
//        Toast.makeText(this, "Data enqueued Object", Toast.LENGTH_SHORT).show();
//        Log.d("Worker Object", "Data enqueued");
//    }

    private void requestLocationUpdates() {
        locationExecutorService.execute(() -> {
            Log.d("Thread 2","Inside");
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                runOnUiThread(() -> {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
                });
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLocationUpdates();
        } else {
            Log.d("Location", "Location permission denied");
        }

        if (requestCode == REQUEST_SMS_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                smsHelper.sendSmsToMultipleNumbers(PHONE_NUMBERS, MESSAGE);
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
        videoExecutorService.shutdown();
        locationExecutorService.shutdown();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Location Alerts";
            String description = "Notifications for sudden decrease in speed";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 1000});

            Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.notification_sound);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            channel.setSound(soundUri, audioAttributes);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


    private void sendNotification(String title, String message, String accessToken) {
        Intent intent = new Intent(this, ResponseActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("latitude", previousLocation.getLatitude());
        intent.putExtra("longitude", previousLocation.getLongitude());
        intent.putExtra("access_token",accessToken);
        Log.d("access token notify",accessToken);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.notification_sound);
        long[] vibrationPattern = {0, 500, 1000};

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(soundUri)
                .setVibrate(vibrationPattern)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(1, builder.build());
    }

    private void LogOut(){
        SharedPreferences prefs= getSharedPreferences("MyAppPrefs",MODE_PRIVATE);

        Set<String> phoneNumbers = prefs.getStringSet("phone_numbers", new HashSet<>());
        int selectedTime = prefs.getInt("selected_time", -1);
        String username = prefs.getString("username", "none");
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        String phoneNumbersString = TextUtils.join("|", phoneNumbers);

        Log.d(TAG, "Phone Numbers: " + phoneNumbersString);
        Log.d(TAG, "Selected Time: " + selectedTime);
        Log.d(TAG, "Username: " + username);
        Log.d(TAG, "Is Logged In: " + isLoggedIn);

        UserData.UserDataPhone userData = new UserData.UserDataPhone();
        userData.setEmail(username);
        userData.setPhone_number(phoneNumbersString);
        userData.setTime_period(String.valueOf(selectedTime));
        updateUserData(userData);



        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("isLoggedIn");
        editor.remove("username");
        editor.remove("phone_numbers");
        editor.remove("selected_time");
        editor.apply();
    }

    private void updateUserData(UserData.UserDataPhone userDataPhone) {
//        Call<Void> call = apiService.updateUserData(userDataPhone);
//        call.enqueue(new Callback<Void>() {
//            @Override
//            public void onResponse(Call<Void> call, Response<Void> response) {
//                if (response.isSuccessful()) {
//                    Log.d("MainActivity updation", "User data updated successfully");
//                } else {
//                    Log.d("MainActivity updation", "Failed to update user data");
//                }
//            }
//
//            @Override
//            public void onFailure(Call<Void> call, Throwable t) {
//                Log.e("MainActivity updation", "Error: " + t.getMessage());
//            }
//        });
    }

    private boolean checkPermissionsCamera() {
        int cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int writeStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int recordAudioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        return cameraPermission == PackageManager.PERMISSION_GRANTED &&
                writeStoragePermission == PackageManager.PERMISSION_GRANTED &&
                recordAudioPermission == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissionsCamera() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
        }, REQUEST_PERMISSIONS);
        startVideoRecording();
    }

    private void startVideoRecording() {
        Log.d("recorder","started");
        videoHelper.startRecording();
        videoHelper.startFrameExtraction();
    }





}
