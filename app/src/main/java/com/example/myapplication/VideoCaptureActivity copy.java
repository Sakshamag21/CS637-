//package com.example.myapplication;
//
//import android.Manifest;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.hardware.camera2.CameraAccessException;
//import android.hardware.camera2.CameraCaptureSession;
//import android.hardware.camera2.CameraDevice;
//import android.hardware.camera2.CameraManager;
//import android.hardware.camera2.CaptureRequest;
//import android.media.MediaRecorder;
//import android.os.Bundle;
//import android.os.Environment;
//import android.util.Log;
//import android.view.Surface;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//
//import java.io.File;
//import java.io.IOException;
//import java.text.SimpleDateFormat;
//import java.util.Arrays;
//import java.util.Date;
//import java.util.Timer;
//import java.util.TimerTask;
//
//public class VideoCaptureActivity extends AppCompatActivity {
//    private static final int REQUEST_CAMERA_PERMISSION = 200;
//    private static final String TAG = "VideoCaptureActivity";
//
//    private CameraDevice cameraDevice;
//    private MediaRecorder mediaRecorder;
//    private Timer timer;
//    private CameraCaptureSession captureSession;
//    private int recordingInterval;
//    private String lastVideoFileName;
//    private String username;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_video_capture);
//
//        Intent intent = getIntent();
//        recordingInterval = intent.getIntExtra("recordingDuration", 30000); // Default to 30 seconds
//        username = intent.getStringExtra("username");
//
//        if ("STOP_RECORDING".equals(intent.getAction())) {
//            stopRecording();
//        } else {
//            if (arePermissionsGranted()) {
//                startRecording();
//            } else {
//                requestPermissions();
//            }
//        }
//    }
//
//    private boolean arePermissionsGranted() {
//        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
//                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
//                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
//    }
//
//    private void requestPermissions() {
//        ActivityCompat.requestPermissions(this, new String[]{
//                Manifest.permission.CAMERA,
//                Manifest.permission.RECORD_AUDIO,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE
//        }, REQUEST_CAMERA_PERMISSION);
//    }
//
//    private void startRecording() {
//        Log.d("wrong record","wrong recoreder");
//        if (prepareVideoRecorder()) {
//            timer = new Timer();
//            timer.schedule(new TimerTask() {
//                @Override
//                public void run() {
//                    runOnUiThread(() -> {
//                        stopRecording();
//                        startRecording();
//                    });
//                }
//            }, recordingInterval, recordingInterval);
//        } else {
//            releaseMediaRecorder();
//        }
//    }
//
//    private void stopRecording() {
//        if (mediaRecorder != null) {
//            try {
//                mediaRecorder.stop();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            releaseMediaRecorder();
//
//            if (timer != null) {
//                timer.cancel();
//            }
//        }
//    }
//
//    private boolean prepareVideoRecorder() {
//        try {
//            setUpMediaRecorder();
//            CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
//            if (cameraManager != null) {
//                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//                    return false;
//                }
//                cameraManager.openCamera(cameraManager.getCameraIdList()[0], new CameraDevice.StateCallback() {
//                    @Override
//                    public void onOpened(@NonNull CameraDevice camera) {
//                        cameraDevice = camera;
//                        try {
//                            createCameraCaptureSession();
//                        } catch (CameraAccessException e) {
//                            e.printStackTrace();
//                            releaseMediaRecorder();
//                        }
//                    }
//
//                    @Override
//                    public void onDisconnected(@NonNull CameraDevice camera) {
//                        releaseMediaRecorder();
//                    }
//
//                    @Override
//                    public void onError(@NonNull CameraDevice camera, int error) {
//                        releaseMediaRecorder();
//                    }
//                }, null);
//            }
//            return true;
//        } catch (CameraAccessException | IOException e) {
//            e.printStackTrace();
//            releaseMediaRecorder();
//            return false;
//        }
//    }
//
//    private void createCameraCaptureSession() throws CameraAccessException {
//        CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
//        Surface recorderSurface = mediaRecorder.getSurface();
//        builder.addTarget(recorderSurface);
//
//        cameraDevice.createCaptureSession(Arrays.asList(recorderSurface), new CameraCaptureSession.StateCallback() {
//            @Override
//            public void onConfigured(@NonNull CameraCaptureSession session) {
//                captureSession = session;
//                try {
//                    session.setRepeatingRequest(builder.build(), null, null);
//                    mediaRecorder.start();
//                } catch (CameraAccessException e) {
//                    e.printStackTrace();
//                    releaseMediaRecorder();
//                }
//            }
//
//            @Override
//            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
//                releaseMediaRecorder();
//            }
//        }, null);
//    }
//
//    private void setUpMediaRecorder() throws IOException {
//        mediaRecorder = new MediaRecorder();
//        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
//        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
//        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
//        File outputFile = getOutputMediaFile();
//        if (outputFile != null) {
//            mediaRecorder.setOutputFile(outputFile.toString());
//            lastVideoFileName = outputFile.getName();
//        } else {
//            throw new IOException("Failed to create output file");
//        }
//        mediaRecorder.setVideoEncodingBitRate(10000000);
//        mediaRecorder.setVideoFrameRate(30);
//        mediaRecorder.setVideoSize(1920, 1080);
//        mediaRecorder.prepare();
//    }
//
//    private void releaseMediaRecorder() {
//        if (mediaRecorder != null) {
//            mediaRecorder.reset();
//            mediaRecorder.release();
//            mediaRecorder = null;
//        }
//        if (cameraDevice != null) {
//            cameraDevice.close();
//            cameraDevice = null;
//        }
//        if (captureSession != null) {
//            captureSession.close();
//            captureSession = null;
//        }
//    }
//
//    private File getOutputMediaFile() {
//        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "MyApp");
//        if (!mediaStorageDir.exists()) {
//            if (!mediaStorageDir.mkdirs()) {
//                Log.e(TAG, "Failed to create directory: " + mediaStorageDir.getPath());
//                return null;
//            }
//        }
//
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//        return new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == REQUEST_CAMERA_PERMISSION) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                startRecording();
//            } else {
//                Toast.makeText(this, "Camera and storage permissions are necessary", Toast.LENGTH_SHORT).show();
//                finish();
//            }
//        }
//    }
//}
