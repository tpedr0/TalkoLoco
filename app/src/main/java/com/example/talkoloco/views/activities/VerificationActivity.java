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
import com.example.talkoloco.controllers.UserController;
import com.example.talkoloco.utils.Constants;
import com.example.talkoloco.utils.PreferenceManager;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import org.signal.libsignal.protocol.state.PreKeyBundle;

/**
 * Activity handling phone number verification process using Firebase Authentication.
 * This activity manages the verification code input, verification process, and subsequent
 * user flow based on whether the user is new or existing.
 */
public class VerificationActivity extends AppCompatActivity {
    private EditText codeInput;
    private TextView instructionsText;
    private AuthController authController;
    private UserController userController;
    private String verificationId;
    private String phoneNumber;
    private boolean isUpdating;
    private PreferenceManager preferenceManager;
    private static final String TAG = "VerificationActivity";
    private long lastResendTime = 0;
    private static final int RESEND_COOLDOWN_SECONDS = 30;

    /**
     * Initializes the activity, sets up the UI components, and retrieves necessary data
     * from the intent that started this activity.
     *
     * @param savedInstanceState If the activity is being re-initialized after being shut down,
     *                          this contains the most recently saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        // Initialize controllers and preference manager
        authController = AuthController.getInstance();
        userController = UserController.getInstance();
        preferenceManager = new PreferenceManager(getApplicationContext());

        // Retrieve verification data from intent
        verificationId = getIntent().getStringExtra("verificationId");
        phoneNumber = getIntent().getStringExtra("phoneNumber");
        isUpdating = getIntent().getBooleanExtra("isUpdating", false);

        initializeViews();
        setupCodeInput();
        setupInstructions();
    }

    /**
     * Initializes and sets up all UI components including the code input field,
     * back button, instructions text, and resend button with their respective click listeners.
     */
    private void initializeViews() {
        codeInput = findViewById(R.id.codeInput);
        TextView backButton = findViewById(R.id.backButton);
        instructionsText = findViewById(R.id.instructionsText);
        TextView resendButton = findViewById(R.id.resendButton);

        // Set click listeners for navigation and resend functionality
        backButton.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
        resendButton.setOnClickListener(v -> onResendClick());
    }

    /**
     * Sets up the verification instructions text by formatting it with the user's phone number.
     */
    private void setupInstructions() {
        String instructions = getString(R.string.verify_instructions, phoneNumber);
        instructionsText.setText(instructions);
    }

    /**
     * Configures the verification code input field with automatic verification
     * trigger when the correct number of digits is entered.
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
     * Attempts to verify the entered verification code with Firebase Authentication.
     * On success, proceeds with user existence check and appropriate navigation.
     *
     * @param code The 6-digit verification code entered by the user
     */
    private void verifyCode(String code) {
        Log.d(TAG, "Attempting to verify code");

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        authController.signInWithPhoneAuthCredential(credential, task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Verification successful");
                checkUserExistsAndProceed();
            } else {
                Log.e(TAG, "Verification failed", task.getException());
                onVerificationFailed();
            }
        });
    }

    /**
     * Checks if the user exists in the database and handles the appropriate flow:
     * - For existing users: Loads user data and navigates to home
     * - For new users: Navigates to profile creation
     * - For phone number updates: Returns to previous screen with updated number
     */
    private void checkUserExistsAndProceed() {
        String userId = authController.getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "User ID is null after verification");
            onVerificationFailed();
            return;
        }

        // Save the user ID and phone number to preferences immediately
        Log.d(TAG, "Saving user ID to preferences: " + userId);
        preferenceManager.putString(Constants.KEY_USER_ID, userId);
        preferenceManager.putString(Constants.KEY_PHONE_NUMBER, phoneNumber);

        // Verify data was saved correctly
        String savedUserId = preferenceManager.getString(Constants.KEY_USER_ID);
        Log.d(TAG, "Verified saved user ID from preferences: " + savedUserId);

        // Handle phone number update scenario
        if (isUpdating) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("phoneNumber", phoneNumber);
            setResult(RESULT_OK, resultIntent);
            finish();
            return;
        }
        // Check user existence and proceed accordingly
        userController.checkIfUserExists(userId,
                exists -> {
                    if (exists) {
                        // For existing users, save their data to preferences
                        FirebaseFirestore.getInstance()
                                .collection(Constants.KEY_COLLECTION_USERS)
                                .document(userId)
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        // Save the name if it exists
                                        String name = documentSnapshot.getString(Constants.KEY_NAME);
                                        if (name != null) {
                                            preferenceManager.putString(Constants.KEY_NAME, name);
                                        }
                                    }
                                    Log.d(TAG, "Navigating to Home with userId: " + userId);
                                    navigateToHome();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error fetching user data", e);
                                    navigateToHome(); // Still proceed even if additional data fetch fails
                                });
                    } else {
                        Log.d(TAG, "New user, navigating to Profile Creation with userId: " + userId);
                        navigateToProfileCreation();
                    }
                },
                e -> {
                    Log.e(TAG, "Error checking user existence", e);
                    Toast.makeText(this, "Error verifying user status", Toast.LENGTH_SHORT).show();
                    onVerificationFailed();
                });
    }

    /**
     * Navigates to the HomeActivity, clearing the activity stack to prevent
     * returning to the verification flow.
     */
    private void navigateToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Navigates to the ProfileCreationActivity for new users, clearing the activity
     * stack to prevent returning to the verification flow.
     */
    private void navigateToProfileCreation() {
        Intent intent = new Intent(this, ProfileCreationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Handles verification failure by showing an error message and resetting the input field.
     */
    private void onVerificationFailed() {
        Toast.makeText(this, "Invalid code. Please try again.", Toast.LENGTH_SHORT).show();
        codeInput.setText("");
    }

    /**
     * Handles the resend verification code request, implementing a cooldown period
     * to prevent abuse. Shows appropriate feedback to the user about the resend status.
     */
    private void onResendClick() {
        if (phoneNumber == null) {
            Toast.makeText(this, "Unable to resend code", Toast.LENGTH_SHORT).show();
            return;
        }

        // Enforce cooldown period between resend attempts
        long currentTime = System.currentTimeMillis() / 1000; // Convert to seconds
        long timeSinceLastResend = currentTime - lastResendTime;

        if (timeSinceLastResend < RESEND_COOLDOWN_SECONDS) {
            int remainingSeconds = (int)(RESEND_COOLDOWN_SECONDS - timeSinceLastResend);
            Toast.makeText(this,
                    "Please wait " + remainingSeconds + " seconds before requesting a new code",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Set up callbacks for the verification process
        PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                        Log.d(TAG, "Verification automatically completed");
                        authController.signInWithPhoneAuthCredential(credential, task -> {
                            if (task.isSuccessful()) {
                                checkUserExistsAndProceed();
                            } else {
                                VerificationActivity.this.onVerificationFailed();
                            }
                        });
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        Log.e(TAG, "Verification failed", e);
                        Toast.makeText(VerificationActivity.this,
                                "Failed to resend code", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCodeSent(String newVerificationId,
                                           PhoneAuthProvider.ForceResendingToken token) {
                        Log.d(TAG, "Code resent successfully");
                        verificationId = newVerificationId;
                        lastResendTime = System.currentTimeMillis() / 1000; // update last resend time
                        Toast.makeText(VerificationActivity.this,
                                "Verification code resent", Toast.LENGTH_SHORT).show();
                        codeInput.setText("");  // clear previous code
                    }
                };

        /// Show loading feedback and initiate verification
        Toast.makeText(this, "Resending verification code...", Toast.LENGTH_SHORT).show();
        authController.startPhoneNumberVerification(phoneNumber, this, callbacks);
    }
}