package com.example.talkoloco.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.talkoloco.R;
import com.example.talkoloco.controllers.AuthController;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

public class VerificationActivity extends AppCompatActivity {
    private EditText codeInput;
    private TextView instructionsText;
    private AuthController authController;
    private String verificationId;
    private String phoneNumber;
    private boolean isUpdating;
    private static final String TAG = "VerificationActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        authController = AuthController.getInstance();

        // Get verification ID and phone number from intent
        verificationId = getIntent().getStringExtra("verificationId");
        phoneNumber = getIntent().getStringExtra("phoneNumber");
        isUpdating = getIntent().getBooleanExtra("isUpdating", false);

        initializeViews();
        setupCodeInput();
        setupInstructions();
    }

    private void initializeViews() {
        codeInput = findViewById(R.id.codeInput);
        TextView backButton = findViewById(R.id.backButton);
        instructionsText = findViewById(R.id.instructionsText);
        TextView resendButton = findViewById(R.id.resendButton);

        backButton.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
        resendButton.setOnClickListener(v -> onResendClick());
    }

    private void setupInstructions() {
        String instructions = getString(R.string.verify_instructions, phoneNumber);
        instructionsText.setText(instructions);
    }

    private void setupCodeInput() {
        codeInput.requestFocus();
        codeInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 6) {
                    verifyCode(s.toString());
                }
            }
        });
    }

    private void verifyCode(String code) {
        Log.d(TAG, "Attempting to verify code");

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        authController.signInWithPhoneAuthCredential(credential, task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Verification successful");
                onVerificationSuccess();
            } else {
                Log.e(TAG, "Verification failed", task.getException());
                onVerificationFailed();
            }
        });
    }

    private void onVerificationSuccess() {
        if (isUpdating) {
            // Return to Settings with success result and the new phone number
            Intent resultIntent = new Intent();
            resultIntent.putExtra("phoneNumber", phoneNumber);
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            // Original flow for new user registration
            Intent intent = new Intent(this, ProfileCreationActivity.class);
            startActivity(intent);
            finishAffinity();
        }
    }


    private void onVerificationFailed() {
        Toast.makeText(this, "Invalid code. Please try again.", Toast.LENGTH_SHORT).show();
        codeInput.setText("");
    }

    private void onResendClick() {
        // TODO: implement resend logic here
        Toast.makeText(this, "Resending code...", Toast.LENGTH_SHORT).show();
        // call AuthController here to resend the code
    }
}