package com.example.myapplication;

import android.content.Context;
import android.telephony.SmsManager;
import android.widget.Toast;

public class SmsSender {

    private Context context;

    public SmsSender(Context context) {
        this.context = context;
    }

    public void sendSmsToMultipleNumbers(String[] phoneNumbers, String message) {
        SmsManager smsManager = SmsManager.getDefault();
        for (String phoneNumber : phoneNumbers) {
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
        }
        Toast.makeText(context, "SMS sent to multiple numbers", Toast.LENGTH_SHORT).show();
    }
}