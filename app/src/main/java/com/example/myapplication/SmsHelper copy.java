package com.example.myapplication;

import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class SmsHelper {
    private static final int REQUEST_SMS_PERMISSION = 1;
    private Context context;

    public SmsHelper(Context context) {
        this.context = context;
    }

    public void sendSmsToMultipleNumbers(String[] phoneNumbers, String message) {
        for (String phoneNumber : phoneNumbers) {
            sendSms(phoneNumber, message);
        }
    }

    private void sendSms(String phoneNumber, String message) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((MainActivity) context, new String[]{android.Manifest.permission.SEND_SMS}, REQUEST_SMS_PERMISSION);
        } else {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                Toast.makeText(context, "SMS sent to " + phoneNumber, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(context, "SMS sending failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
