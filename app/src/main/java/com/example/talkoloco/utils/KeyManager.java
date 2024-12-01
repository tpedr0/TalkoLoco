package com.example.talkoloco.utils;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class KeyManager {
    private static final String TAG = "KeyManager";
    private static final String RSA_ALIAS = "UserRSAKey";
    private static final String AES_TRANSFORMATION = "AES/CBC/PKCS5Padding";  // For encryption/decryption
    private static final String AES_ALGORITHM = "AES";  // For key generation
    private static final String RSA_ALGORITHM = "RSA/ECB/PKCS1Padding";
    private static final int RSA_KEY_SIZE = 2048;
    private static final int AES_KEY_SIZE = 128;

    private final Context context;
    private final PreferenceManager preferenceManager;

    public KeyManager(Context context) {
        this.context = context;
        this.preferenceManager = new PreferenceManager(context);
    }

    /**
     * Generates a new RSA key pair for the user
     * @return Base64 encoded public key
     */
    public String generateUserKeys() {
        try {
            Log.d(TAG, "Starting key generation process");
            String existingPrivateKey = preferenceManager.getString("PRIVATE_KEY");
            String existingPublicKey = preferenceManager.getString(Constants.KEY_PUBLIC_KEY);
            Log.d(TAG, "Existing keys - Private: " + (existingPrivateKey != null) + ", Public: " + (existingPublicKey != null));

            if (existingPrivateKey != null && existingPublicKey != null) {
                Log.d(TAG, "Using existing keys");
                return existingPublicKey;
            }

            Log.d(TAG, "Generating new RSA key pair");
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(RSA_KEY_SIZE);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            // Store the private key
            byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();
            String privateKeyString = Base64.encodeToString(privateKeyBytes, Base64.NO_WRAP);
            preferenceManager.putString("PRIVATE_KEY", privateKeyString);
            Log.d(TAG, "Stored new private key, length: " + privateKeyString.length());

            // Get public key string
            byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
            String publicKeyString = Base64.encodeToString(publicKeyBytes, Base64.NO_WRAP);
            preferenceManager.putString(Constants.KEY_PUBLIC_KEY, publicKeyString);
            Log.d(TAG, "Generated new public key, length: " + publicKeyString.length());

            return publicKeyString;
        } catch (Exception e) {
            Log.e(TAG, "Error generating RSA keys", e);
            throw new RuntimeException("Failed to generate RSA keys", e);
        }
    }

    private PrivateKey getPrivateKey() {
        try {
            String privateKeyString = preferenceManager.getString("PRIVATE_KEY");
            Log.d(TAG, "Retrieved private key from preferences: " + (privateKeyString != null));

            if (privateKeyString == null) {
                throw new RuntimeException("Private key not found");
            }

            byte[] privateKeyBytes = Base64.decode(privateKeyString, Base64.NO_WRAP);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            Log.e(TAG, "Error getting private key", e);
            throw new RuntimeException("Failed to get private key", e);
        }
    }

    /**
     * Generates a random AES key for message encryption
     */
    public SecretKey generateAESKey() {
        try {
            // Use just "AES" for key generation
            KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
            keyGen.init(AES_KEY_SIZE, new SecureRandom());
            return keyGen.generateKey();
        } catch (Exception e) {
            Log.e(TAG, "Error generating AES key", e);
            throw new RuntimeException("Failed to generate AES key", e);
        }
    }

    /**
     * Encrypts a message using AES
     */
    public String encryptMessage(String message, SecretKey aesKey) {
        try {
            // Generate IV
            byte[] iv = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // Initialize cipher with IV
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec);

            // Encrypt
            byte[] encryptedBytes = cipher.doFinal(message.getBytes());

            // Combine IV and encrypted data
            byte[] combined = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

            return Base64.encodeToString(combined, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting message", e);
            throw new RuntimeException("Failed to encrypt message", e);
        }
    }

    /**
     * Decrypts a message using AES
     */
    public String decryptMessage(String encryptedMessage, SecretKey aesKey) {
        try {
            byte[] combined = Base64.decode(encryptedMessage, Base64.NO_WRAP);

            // Extract IV and encrypted data
            byte[] iv = new byte[16];
            byte[] encrypted = new byte[combined.length - 16];
            System.arraycopy(combined, 0, iv, 0, 16);
            System.arraycopy(combined, 16, encrypted, 0, encrypted.length);

            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, ivSpec);

            byte[] decryptedBytes = cipher.doFinal(encrypted);
            return new String(decryptedBytes);
        } catch (Exception e) {
            Log.e(TAG, "Error decrypting message", e);
            throw new RuntimeException("Failed to decrypt message", e);
        }
    }
    /**
     * Encrypts the AES key using recipient's public key
     */
    public String encryptAESKey(SecretKey aesKey, String recipientPublicKeyString) {
        try {
            Log.d(TAG, "Encrypting AES key with recipient's public key");

            // Decode public key
            byte[] publicKeyBytes = Base64.decode(recipientPublicKeyString, Base64.NO_WRAP);
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            // Encrypt
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedKey = cipher.doFinal(aesKey.getEncoded());
            return Base64.encodeToString(encryptedKey, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting AES key", e);
            throw new RuntimeException("Failed to encrypt AES key", e);
        }
    }

    public SecretKey decryptAESKey(String encryptedKeyString) {
        try {
            Log.d(TAG, "Decrypting AES key");

            // Get stored private key
            String privateKeyString = preferenceManager.getString("PRIVATE_KEY");
            if (privateKeyString == null) {
                throw new RuntimeException("Private key not found");
            }

            // Decode private key
            byte[] privateKeyBytes = Base64.decode(privateKeyString, Base64.NO_WRAP);
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

            // Decrypt
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedKey = cipher.doFinal(Base64.decode(encryptedKeyString, Base64.NO_WRAP));
            return new SecretKeySpec(decryptedKey, "AES");
        } catch (Exception e) {
            Log.e(TAG, "Error decrypting AES key", e);
            throw new RuntimeException("Failed to decrypt AES key", e);
        }
    }
}