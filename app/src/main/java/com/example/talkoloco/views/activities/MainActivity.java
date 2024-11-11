package com.example.talkoloco.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.talkoloco.R;
import com.example.talkoloco.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.FirebaseTooManyRequestsException;

import java.util.concurrent.TimeUnit;

// initializes initial phone number input and verification flow
public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private boolean isFormatting;
    private String lastFormatted = "";
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // initializes Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        initializeViews();
        setupPhoneNumberInput();
    }

    // initializes the views and sets up phone number input field
    private void initializeViews() {
        binding.phoneInput.setText("+1 ");
        binding.phoneInput.setSelection(binding.phoneInput.length());
        binding.btnNext.setEnabled(false);
        binding.btnNext.setOnClickListener(v -> onNextButtonClick());
    }

    /**
     * handles the click event of the "Next" button, which shows a confirmation dialog.
     */
    private void onNextButtonClick() {
        String phoneNumber = binding.phoneInput.getText().toString();
        showConfirmationDialog(phoneNumber);
    }

    /**
     * sets up the phone number input field, including formatting the input and enabling/disabling the next button.
     */
    private void setupPhoneNumberInput() {
        binding.phoneInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;
                isFormatting = true;

                // gets just the numbers
                String text = s.toString();
                String numbers = text.replaceAll("[^\\d]", "");

                // limits to 11 digits (including the country code)
                if (numbers.length() > 11) {
                    numbers = numbers.substring(0, 11);
                }

                if (!text.startsWith("+1")) {
                    if (!numbers.startsWith("1")) {
                        numbers = "1" + numbers;
                    }
                }

                // formats the number
                StringBuilder formatted = new StringBuilder("+1 ");
                if (numbers.length() > 1) {
                    numbers = numbers.substring(1); // removes the 1 from country code
                    if (numbers.length() > 0) {
                        formatted.append("(");
                        formatted.append(numbers.substring(0, Math.min(3, numbers.length())));

                        if (numbers.length() > 3) {
                            formatted.append(") ");
                            formatted.append(numbers.substring(3, Math.min(6, numbers.length())));

                            if (numbers.length() > 6) {
                                formatted.append("-");
                                formatted.append(numbers.substring(6, Math.min(10, numbers.length())));
                            }
                        } else {
                            formatted.append(")");
                        }
                    }
                }

                String formattedText = formatted.toString();
                if (!formattedText.equals(lastFormatted)) {
                    lastFormatted = formattedText;
                    s.replace(0, s.length(), formattedText);
                    binding.phoneInput.setSelection(formattedText.length());
                }

                // enable/disable next button based on phone number length
                String digitsOnly = formattedText.replaceAll("\\D", "");
                binding.btnNext.setEnabled(digitsOnly.length() == 11);

                isFormatting = false;
            }
        });
    }

    /**
     * displays a confirmation dialog for the entered phone number and starts the phone number verification process.
     *
     * @param phoneNumber the phone number entered by the user
     */
    private void showConfirmationDialog(final String phoneNumber) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_confirm_number, null);
        builder.setView(dialogView);

        TextView numberText = dialogView.findViewById(R.id.numberText);
        Button editButton = dialogView.findViewById(R.id.editButton);
        Button confirmButton = dialogView.findViewById(R.id.confirmButton);

        numberText.setText(phoneNumber);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawableResource(R.drawable.dialog_background);
            }
        });

        editButton.setOnClickListener(v -> dialog.dismiss());

        confirmButton.setOnClickListener(v -> {
            dialog.dismiss();
            startPhoneNumberVerification(phoneNumber);
        });

        dialog.show();
    }

    /**
     * initiates the phone number verification process using the Firebase Authentication API.
     *
     * @param phoneNumber the phone number to be verified
     */
    private void startPhoneNumberVerification(String phoneNumber) {
        final String cleanNumber = phoneNumber.replaceAll("[^\\d+]", "").startsWith("+")
                ? phoneNumber.replaceAll("[^\\d+]", "")
                : "+" + phoneNumber.replaceAll("[^\\d]", "");

        PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        handleVerificationCompleted(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        handleVerificationFailed(e);
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        handleCodeSent(verificationId, phoneNumber);
                    }
                };

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(cleanNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks)
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    /**
     * handles the successful completion of the phone number verification process.
     *
     * @param credential the phone auth credential used for sign-in
     */
    private void handleVerificationCompleted(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        navigateToProfileCreation();
                    } else {
                        String message = "Authentication failed";
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            message = "Invalid verification code";
                        }
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * handles the failure of the phone number verification process.
     *
     * @param e the FirebaseException that caused the verification failure
     */
    private void handleVerificationFailed(FirebaseException e) {
        String message = "Verification failed";
        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            message = "Invalid phone number format";
        } else if (e instanceof FirebaseTooManyRequestsException) {
            message = "Too many requests. Please try again later.";
        }
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * handles the successful sending of the verification code.
     *
     * @param verificationId the ID of the verification process
     * @param phoneNumber    the phone number being verified
     */
    private void handleCodeSent(String verificationId, String phoneNumber) {
        Intent intent = new Intent(MainActivity.this, VerificationActivity.class);
        intent.putExtra("verificationId", verificationId);
        intent.putExtra("phoneNumber", phoneNumber);
        startActivity(intent);
    }

    /**
     * navigates to the ProfileCreation activity after successful phone number verification.
     */
    private void navigateToProfileCreation() {
        Intent intent = new Intent(MainActivity.this, ProfileCreationActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}