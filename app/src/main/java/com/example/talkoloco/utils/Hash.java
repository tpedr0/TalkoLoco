package com.example.talkoloco.utils;

import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hash {

    public static String hashPhoneNumber(String phoneNumber) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(phoneNumber.getBytes(StandardCharsets.UTF_8));
            return android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }
    public static void debugPhoneHash(String phoneNumber) {
        String hash = hashPhoneNumber(phoneNumber);
        Log.d("Hash", "Original number: " + phoneNumber);
        Log.d("Hash", "Hashed value: " + hash);
    }

}
