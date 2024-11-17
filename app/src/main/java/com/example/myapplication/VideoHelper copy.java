package com.example.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

public class VideoHelper {
    private static final String TAG = "VideoHelper";
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private static final long FRAME_EXTRACTION_INTERVAL = 10000; // Interval to extract frames (10 seconds)
    private static final long RECORDING_INTERVAL = 20000; // 20 seconds in milliseconds

    private Context context;
    private CameraDevice cameraDevice;
    private MediaRecorder mediaRecorder;
    private CameraCaptureSession captureSession;

    private Timer frameExtractionTimer;
    private boolean isRecording;
    private String lastVideoFileName;
    private String lastSecondVideoFileName;
    private Timer timer;
    private Queue<Bitmap> frameQueue;
    private int frameCounter = 0;  // Frame counter for tracking the 100th frame

    public VideoHelper(Context context, Queue<Bitmap> frameQueue) {
        this.context = context;
        this.frameQueue = frameQueue;
    }

    public void startRecording() {
        if (prepareVideoRecorder()) {
            // Schedule the first recording segment
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    stopRecording();
                    lastSecondVideoFileName = lastVideoFileName;
                    new Handler(Looper.getMainLooper()).post(() -> startRecording());
                }
            }, RECORDING_INTERVAL);
        } else {
            releaseMediaRecorder();
        }
    }

    private boolean prepareVideoRecorder() {
        try {
            setUpMediaRecorder();
            CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            if (cameraManager != null) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
                // Use Handler to run cameraManager.openCamera() on the main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        cameraManager.openCamera(cameraManager.getCameraIdList()[0], new CameraDevice.StateCallback() {
                            @Override
                            public void onOpened(@NonNull CameraDevice camera) {
                                cameraDevice = camera;
                                try {
                                    createCameraCaptureSession();
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                    releaseMediaRecorder();
                                }
                            }

                            @Override
                            public void onDisconnected(@NonNull CameraDevice camera) {
                                releaseMediaRecorder();
                            }

                            @Override
                            public void onError(@NonNull CameraDevice camera, int error) {
                                releaseMediaRecorder();
                            }
                        }, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                        releaseMediaRecorder();
                    }
                });
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            releaseMediaRecorder();
            return false;
        }
    }

    public void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
            releaseMediaRecorder();

            if (timer != null) {
                timer.cancel();
                timer = null;
            }
        }
    }

    private void startRecordingSegment() {
        isRecording = true;
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                stopRecordingSegment();
            }
        }, RECORDING_INTERVAL, RECORDING_INTERVAL);
    }

    public void stopRecordingSegment() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.reset();
                setUpMediaRecorder();
                createCameraCaptureSession();
                mediaRecorder.start();
            } catch (Exception e) {
                Log.e(TAG, "Error restarting media recorder: " + e.getMessage());
                releaseMediaRecorder();
            }
        }
    }

    private void createCameraCaptureSession() throws CameraAccessException {
        CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
        Surface recorderSurface = mediaRecorder.getSurface();
        ((CaptureRequest.Builder) builder).addTarget(recorderSurface);

        cameraDevice.createCaptureSession(Arrays.asList(recorderSurface), new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                captureSession = session;
                try {
                    session.setRepeatingRequest(builder.build(), null, null);
                    mediaRecorder.start();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                    releaseMediaRecorder();
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                releaseMediaRecorder();
            }
        }, null);
    }

    private void setUpMediaRecorder() throws IOException {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        File outputFile = getOutputMediaFile();
        if (outputFile != null) {
            mediaRecorder.setOutputFile(outputFile.toString());
            lastVideoFileName = outputFile.getName();
            Log.d(TAG, "Video will be saved to " + outputFile.getAbsolutePath());
        } else {
            throw new IOException("Failed to create output file");
        }
        mediaRecorder.setVideoEncodingBitRate(10000000);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoSize(1920, 1080);
        mediaRecorder.prepare();
        Log.d(TAG, "Media Prepared");
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
    }

    private File getOutputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "MyApp");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.e(TAG, "Failed to create directory: " + mediaStorageDir.getPath());
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void startFrameExtraction() {
        frameExtractionTimer = new Timer();
        frameExtractionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    extractFrameFromLastVideo();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 5000, FRAME_EXTRACTION_INTERVAL);
    }

    private void extractFrameFromLastVideo() throws IOException {
        if (lastSecondVideoFileName == null) {
            Log.e(TAG, "No video recorded yet.");
            return;
        }

        File videoFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "MyApp" + File.separator + lastSecondVideoFileName);
        if (!videoFile.exists()) {
            Log.e(TAG, "Video file not found: " + videoFile.getAbsolutePath());
            return;
        }
        Log.d(TAG, "Video file path: " + videoFile.getAbsolutePath());

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(videoFile.getAbsolutePath());

        try {
            Bitmap frame = retriever.getFrameAtTime(0);
            if (frame != null) {
                synchronized (frameQueue) {
                    frameQueue.offer(frame);
                    Log.d(TAG, "Frame extracted and added to queue. Queue size: " + frameQueue.size());

                    frameCounter++;
                    Log.d(TAG, String.valueOf(frameCounter));
                    if (frameCounter % 10 == 0) {
                        // Upload the 100th frame
                        Log.d(TAG,"Uploading started");
                        uploadFrame(frame);
                    }
                }
            } else {
                Log.e(TAG, "Failed to extract frame from video: frame is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting frame from video: " + e.getMessage());
        } finally {
            retriever.release();
        }
    }

    private void uploadFrame(Bitmap frame) {
        FirebaseUploader uploader = new FirebaseUploader();

        // Convert Bitmap to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        frame.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] data = baos.toByteArray();

        String fileName = "frame_" + System.currentTimeMillis() + ".png";
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("frames/" + fileName);

        storageRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    // Save the URL to Firestore
                    uploader.saveFrameUrlToFirestore(fileName, downloadUri.toString());
                }))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to upload frame: " + e.getMessage()));
    }

    public Queue<Bitmap> getFrameQueue() {
        return frameQueue;
    }

    public void stopFrameExtraction() {
        if (frameExtractionTimer != null) {
            frameExtractionTimer.cancel();
            frameExtractionTimer = null;
        }
    }
}

