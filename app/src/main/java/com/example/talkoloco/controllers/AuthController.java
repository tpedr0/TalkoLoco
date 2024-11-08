package com.example.talkoloco.controllers;

import android.app.Activity;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

/**
 * the AuthController class is a singleton controller that manages the Firebase Authentication-related operations.
 */
public class AuthController {
    private final FirebaseAuth mAuth;
    private static AuthController instance;
    private static final String TAG = "AuthController";

    private AuthController() {
        mAuth = FirebaseAuth.getInstance();
    }

    public static AuthController getInstance() {
        if (instance == null) {
            instance = new AuthController();
        }
        return instance;
    }

    /**
     * initiates the phone number verification process using the Firebase Authentication API.
     *
     * @param phoneNumber the phone number to be verified
     * @param activity    the current activity
     * @param callbacks   the callbacks for the verification process
     */
    public void startPhoneNumberVerification(String phoneNumber, Activity activity,
                                             PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks) {
        Log.d(TAG, "Starting phone verification for: " + phoneNumber);

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }


    /**
     * signs in the user with the provided phone auth credential.
     *
     * @param credential             the phone auth credential
     * @param onCompleteListener the listener for the sign-in result
     */
    public void signInWithPhoneAuthCredential(PhoneAuthCredential credential,
                                              OnCompleteListener<AuthResult> onCompleteListener) {
        Log.d(TAG, "Attempting to sign in with phone credential");
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(onCompleteListener);
    }

    /**
     * verifies the phone number with the provided code.
     *
     * @param verificationId         the verification ID
     * @param code                   the verification code
     * @param onCompleteListener the listener for the verification result
     */
    public void verifyPhoneNumberWithCode(String verificationId, String code,
                                          OnCompleteListener<AuthResult> onCompleteListener) {
        Log.d(TAG, "Verifying phone number with code");
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential, onCompleteListener);
    }

    /**
     * retrieves the current user's ID, if the user is signed in.
     *
     * @return the current user's ID, or null if the user is not signed in
     */
    public String getCurrentUserId() {
        return mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
    }

    /**
     * checks if the user is currently signed in.
     *
     * @return true if the user is signed in, false otherwise
     */
    public boolean isUserSignedIn() {
        return mAuth.getCurrentUser() != null;
    }

    /**
     * signs out the current user.
     */
    public void signOut() {
        mAuth.signOut();
    }
}