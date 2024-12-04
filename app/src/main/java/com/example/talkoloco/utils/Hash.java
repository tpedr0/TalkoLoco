package com.example.talkoloco.utils;

import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for handling phone number hashing operations.
 * Implements SHA-256 hashing for secure phone number storage and comparison.
 */
public class Hash {

    /**
     * Hashes a phone number using SHA-256 and encodes it as a Base64 string.
     * Used for secure storage and comparison of phone numbers.
     *
     * @param phoneNumber The phone number to hash
     * @return Base64 encoded SHA-256 hash of the phone number
     * @throws RuntimeException if hashing algorithm is not available
     */
    public static String hashPhoneNumber(String phoneNumber) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(phoneNumber.getBytes(StandardCharsets.UTF_8));
            return android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    /**
     * Debug utility to log original and hashed phone numbers.
     * Should only be used during development/testing.
     *
     * @param phoneNumber The phone number to hash and log
     */
    public static void debugPhoneHash(String phoneNumber) {
        String hash = hashPhoneNumber(phoneNumber);
        Log.d("Hash", "Original number: " + phoneNumber);
        Log.d("Hash", "Hashed value: " + hash);
    }

}
