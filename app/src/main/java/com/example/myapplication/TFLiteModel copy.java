package com.example.myapplication;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;

public class TFLiteModel {

    private Interpreter interpreter;
    private int inputWidth;
    private int inputHeight;
    private int inputChannels;

    public TFLiteModel(Context context, String modelPath) throws IOException {
        interpreter = new Interpreter(loadModelFile(context, modelPath));
        int[] inputShape = interpreter.getInputTensor(0).shape();
        inputHeight = inputShape[1];
        inputWidth = inputShape[2];
        inputChannels = inputShape[3];

    }

    private MappedByteBuffer loadModelFile(Context context, String modelPath) throws IOException {
        InputStream is = context.getAssets().open(modelPath);
        byte[] modelBytes = new byte[is.available()];
        is.read(modelBytes);
        ByteBuffer buffer = ByteBuffer.allocateDirect(modelBytes.length);
        buffer.order(ByteOrder.nativeOrder());
        buffer.put(modelBytes);
        return (MappedByteBuffer) buffer.asReadOnlyBuffer();
    }

    public ByteBuffer preprocessImage(Context context, String imagePath) {
        Bitmap bitmap = null;
        try {
            InputStream is = context.getAssets().open(imagePath);
            bitmap = BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (bitmap == null) {
            throw new NullPointerException("Bitmap is null. Check the image path.");
        }

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1 * inputWidth * inputHeight * inputChannels);
        byteBuffer.order(ByteOrder.nativeOrder());

        int[] intValues = new int[inputWidth * inputHeight];
        resizedBitmap.getPixels(intValues, 0, resizedBitmap.getWidth(), 0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight());

        for (int i = 0; i < intValues.length; ++i) {
            int pixelValue = intValues[i];
            byteBuffer.put((byte) ((pixelValue >> 16) & 0xFF));
            byteBuffer.put((byte) ((pixelValue >> 8) & 0xFF));
            byteBuffer.put((byte) (pixelValue & 0xFF));
        }

        return byteBuffer;
    }

    public float[][] runInference(Context context, String imagePath) {
        ByteBuffer inputBuffer = preprocessImage(context, imagePath);
//        float[][] output = new float[1][interpreter.getOutputTensor(0).shape()[1]];
        float[][] output = new float[1][100];

        interpreter.run(inputBuffer, output);

        return output;
    }
}
