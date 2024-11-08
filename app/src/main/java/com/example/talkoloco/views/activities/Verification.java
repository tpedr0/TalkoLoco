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
// import com.google.firebase.FirebaseTooManyRequestsException;
// import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
// import com.google.firebase.auth.FirebaseAuthSettings;

/**
 * the Verification class handles the phone number verification code input and verification process.
 */
public class  Verification extends AppCompatActivity {
    private EditText codeInput;
    private TextView instructionsText;
    private AuthController authController;
    private String verificationId;
    private String phoneNumber;
    private static final String TAG = "VerificationActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        authController = AuthController.getInstance();

        // get verification ID and phone number from intent
        verificationId = getIntent().getStringExtra("verificationId");
        phoneNumber = getIntent().getStringExtra("phoneNumber");

        initializeViews();
        setupCodeInput();
        setupInstructions();
    }

    /**
     * initializes the views and sets up the event listeners.
     */
    private void initializeViews() {
        codeInput = findViewById(R.id.codeInput);
        TextView backButton = findViewById(R.id.backButton);
        instructionsText = findViewById(R.id.instructionsText);
        TextView resendButton = findViewById(R.id.resendButton);

        backButton.setOnClickListener(v -> finish());
        resendButton.setOnClickListener(v -> onResendClick());
    }

    /**
     * sets up the instructions text based on the user's phone number.
     */
    private void setupInstructions() {
        String instructions = getString(R.string.verify_instructions, phoneNumber);
        instructionsText.setText(instructions);
    }

    /**
     * sets up the code input field and handles the input using a TextWatcher.
     */
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

    /**
     * verifies the entered code using the Firebase Authentication API.
     *
     * @param code the verification code entered by the user
     */
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

    /**
     * handles the successful verification of the phone number.
     */
    private void onVerificationSuccess() {
        // navigate to profile creation
        Intent intent = new Intent(this, ProfileCreation.class);
        startActivity(intent);
        finishAffinity(); // close all previous activities
    }

    /**
     * handles the failure of the phone number verification.
     */
    private void onVerificationFailed() {
        Toast.makeText(this, "Invalid code. Please try again.", Toast.LENGTH_SHORT).show();
        codeInput.setText("");
    }

    /**
     * handles the click event of the "Resend" button, which is a placeholder for the resend code functionality.
     */
    private void onResendClick() {
        // TO-DO: implement resend logic here
        Toast.makeText(this, "Resending code...", Toast.LENGTH_SHORT).show();
        // call AuthController here to resend the code
    }
}