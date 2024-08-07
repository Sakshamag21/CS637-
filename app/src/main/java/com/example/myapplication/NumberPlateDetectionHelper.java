package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class NumberPlateDetectionHelper {

    private static final String TAG = "NumberPlateDetection";
    private Interpreter tflite;
    private TessBaseAPI tessBaseAPI;
    private VideoHelper videoHelper;
    private static final String DATA_PATH = "tessdata/";
    private static final String LANGUAGE = "lp";
    private static final int INPUT_SIZE = 1088;

    private SharedPreferences prefs;


    public void initializeModels(Context context, Queue<Bitmap> frameQueue) {
        try {
            tflite = new Interpreter(loadModelFile(context));
        } catch (IOException e) {
            e.printStackTrace();
        }
        tessBaseAPI = new TessBaseAPI();
        initializeTess(context);
        prefs = context.getApplicationContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);


        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
//                Queue<Bitmap> frames = videoHelper.getFrameQueue();
                while (!frameQueue.isEmpty()) {
                    Bitmap frame = frameQueue.poll(); // Retrieves and removes the head of the queue
                    if (frame != null) {
                        Log.d(TAG, "Processing frame: " + frame.toString());
                        detectNumberPlate(frame);

                    } else {
                        Log.e(TAG, "Frame is null");
                    }
                }
            }
        }, 10000, 30000);
    }

    private void initializeTess(Context context) {
        Log.d("Number plate","Intilialized tess");
        String dataPath = context.getFilesDir() + "/" + DATA_PATH;
        File dir = new File(dataPath);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e(TAG, "Failed to create directory: " + dataPath);
                return;
            }
        }
        String trainedDataPath = dataPath + LANGUAGE + ".traineddata";
        Log.d("Number Plate",trainedDataPath);
        if (!(new File(trainedDataPath)).exists()) {
            try {
                InputStream in = context.getAssets().open("Tesseract/lp.traineddata");
                OutputStream out = new FileOutputStream(trainedDataPath);
                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                in.close();
                out.close();
            } catch (IOException e) {
                Log.e(TAG, "Error copying eng.traineddata to: " + trainedDataPath, e);
            }
        }
        tessBaseAPI.init(context.getFilesDir() + "/", "lp");
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(context.getAssets().openFd("numberplate1.tflite").getFileDescriptor())) {
            FileChannel fileChannel = fileInputStream.getChannel();
            long startOffset = context.getAssets().openFd("numberplate1.tflite").getStartOffset();
            long declaredLength = context.getAssets().openFd("numberplate1.tflite").getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
    }

    public Bitmap loadImageFromAssets(Context context, String fileName) throws IOException {
        InputStream inputStream = context.getAssets().open(fileName);
        return BitmapFactory.decodeStream(inputStream);
    }
    public Bitmap loadImageFromCache(Context context, String fileName) throws IOException {
        File cacheDir = context.getCacheDir();
        File imageFile = new File(cacheDir, fileName);

        if (!imageFile.exists()) {
            throw new IOException("File not found: " + imageFile.getAbsolutePath());
        }

        return BitmapFactory.decodeFile(imageFile.getAbsolutePath());
    }

    public String performOCR(Bitmap bitmap) {
        if (tessBaseAPI == null) {
            Log.e(TAG, "TessBaseAPI is not initialized");
            return "";
        }
        tessBaseAPI.setImage(bitmap);
        String extractedText = tessBaseAPI.getUTF8Text();
        Log.d(TAG, "Extracted Text: " + extractedText);
        return extractedText;
    }

    public void detectNumberPlate(Bitmap bitmap) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);
        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
        resizedBitmap.getPixels(intValues, 0, resizedBitmap.getWidth(), 0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight());

        float[][][][] input = new float[1][INPUT_SIZE][INPUT_SIZE][3];
        for (int i = 0; i < intValues.length; ++i) {
            int val = intValues[i];
            input[0][i / INPUT_SIZE][i % INPUT_SIZE][0] = ((val >> 16) & 0xFF) / 255.0f;
            input[0][i / INPUT_SIZE][i % INPUT_SIZE][1] = ((val >> 8) & 0xFF) / 255.0f;
            input[0][i / INPUT_SIZE][i % INPUT_SIZE][2] = (val & 0xFF) / 255.0f;
        }

        float[][][] output = new float[1][72828][6];
        tflite.run(input, output);
        Set<String> numberPlates = prefs.getStringSet("number_plates", new HashSet<>());

        List<DetectionResult> results = postProcess(output[0], 0.5f, 0.4f);
        Log.d(TAG,"no detections");
        for (DetectionResult result : results) {
            int xmin = (int) (result.xmin * bitmap.getWidth());
            int ymin = (int) (result.ymin * bitmap.getHeight());
            int xmax = (int) (result.xmax * bitmap.getWidth());
            int ymax = (int) (result.ymax * bitmap.getHeight());
            Log.d(TAG, "Detection: " + result.toString());

            if (xmin >= 0 && ymin >= 0 && xmax <= bitmap.getWidth() && ymax <= bitmap.getHeight()) {
                Bitmap detectedBitmap = Bitmap.createBitmap(bitmap, xmin, ymin, xmax - xmin, ymax - ymin);
                String recognizedText = performOCR(detectedBitmap);
                Log.d(TAG, "OCR Result: " + recognizedText);
                numberPlates.add(recognizedText);
            } else {
                Log.e(TAG, "Invalid region detected: xmin=" + xmin + ", ymin=" + ymin + ", xmax=" + xmax + ", ymax=" + ymax);
            }
        }
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet("number_plates", numberPlates);
        editor.apply();
    }

    private List<DetectionResult> postProcess(float[][] output, float confidenceThreshold, float iouThreshold) {
        List<DetectionResult> results = new ArrayList<>();

        for (float[] detection : output) {
            float x_center = detection[0];
            float y_center = detection[1];
            float width = detection[2];
            float height = detection[3];
            float confidence = detection[4];
            float class_prob = detection[5];

            if (confidence > confidenceThreshold) {
                float xmin = x_center - width / 2;
                float ymin = y_center - height / 2;
                float xmax = x_center + width / 2;
                float ymax = y_center + height / 2;

                DetectionResult result = new DetectionResult(xmin, ymin, xmax, ymax, confidence, class_prob);
                results.add(result);
            }
        }

        return nonMaxSuppression(results, iouThreshold);
    }

    private List<DetectionResult> nonMaxSuppression(List<DetectionResult> detections, float iouThreshold) {
        // Sort detections by confidence
        PriorityQueue<DetectionResult> pq = new PriorityQueue<>((a, b) -> Float.compare(b.confidence, a.confidence));
        pq.addAll(detections);

        List<DetectionResult> finalDetections = new ArrayList<>();
        while (!pq.isEmpty()) {
            DetectionResult best = pq.poll();
            finalDetections.add(best);

            List<DetectionResult> remaining = new ArrayList<>();
            while (!pq.isEmpty()) {
                DetectionResult current = pq.poll();
                if (iou(best, current) < iouThreshold) {
                    remaining.add(current);
                }
            }
            pq.addAll(remaining);
        }
        return finalDetections;
    }

    private float iou(DetectionResult a, DetectionResult b) {
        float areaA = (a.xmax - a.xmin) * (a.ymax - a.ymin);
        float areaB = (b.xmax - b.xmin) * (b.ymax - b.ymin);

        float intersectionMinX = Math.max(a.xmin, b.xmin);
        float intersectionMinY = Math.max(a.ymin, b.ymin);
        float intersectionMaxX = Math.min(a.xmax, b.xmax);
        float intersectionMaxY = Math.min(a.ymax, b.ymax);

        float intersectionArea = Math.max(0, intersectionMaxX - intersectionMinX) * Math.max(0, intersectionMaxY - intersectionMinY);
        return intersectionArea / (areaA + areaB - intersectionArea);
    }

    private static class DetectionResult {
        float xmin, ymin, xmax, ymax, confidence, class_prob;

        DetectionResult(float xmin, float ymin, float xmax, float ymax, float confidence, float class_prob) {
            this.xmin = xmin;
            this.ymin = ymin;
            this.xmax = xmax;
            this.ymax = ymax;
            this.confidence = confidence;
            this.class_prob = class_prob;
        }

        @Override
        public String toString() {
            return "xmin: " + xmin + ", ymin: " + ymin + ", xmax: " + xmax + ", ymax: " + ymax + ", confidence: " + confidence + ", class_prob: " + class_prob;
        }
    }

    public void release() {
        if (tessBaseAPI != null) {
            tessBaseAPI.end();
        }
    }
}
