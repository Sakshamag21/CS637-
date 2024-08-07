package com.example.myapplication;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class OTPDialogFragment extends DialogFragment {

    private EditText otpEditText;
    private Button verifyOtpButton;
    private OTPDialogListener listener;

    public interface OTPDialogListener {
        void onVerifyOtp(String otp);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_otp, container, false);

        otpEditText = view.findViewById(R.id.otpEditText);
        verifyOtpButton = view.findViewById(R.id.verifyOtpButton);

        verifyOtpButton.setOnClickListener(v -> {
            String enteredOtp = otpEditText.getText().toString();
            if (enteredOtp.isEmpty()) {
                Toast.makeText(getActivity(), "Please enter OTP", Toast.LENGTH_SHORT).show();
            } else {
                listener.onVerifyOtp(enteredOtp);
                dismiss();
            }
        });

        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (OTPDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OTPDialogListener");
        }
    }
}
