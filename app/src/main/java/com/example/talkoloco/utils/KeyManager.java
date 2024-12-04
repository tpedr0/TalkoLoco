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

/**
 * Manages encryption and key operations for secure message exchange.
 * Handles RSA key pair generation, AES key generation, and message encryption/decryption.
 */
public class KeyManager {
    private static final String TAG = "KeyManager";

    // Cryptographic constants
    private static final String RSA_ALIAS = "UserRSAKey";
    private static final String AES_TRANSFORMATION = "AES/CBC/PKCS5Padding";  // For encryption/decryption
    private static final String AES_ALGORITHM = "AES";  // For key generation
    private static final String RSA_ALGORITHM = "RSA/ECB/PKCS1Padding";
    private static final int RSA_KEY_SIZE = 2048;
    private static final int AES_KEY_SIZE = 128;

    private final Context context;
    private final PreferenceManager preferenceManager;

    /**
     * Initializes the KeyManager with application context for preferences access.
     *
     * @param context Application context for accessing preferences
     */
    public KeyManager(Context context) {
        this.context = context;
        this.preferenceManager = new PreferenceManager(context);
    }

    /**
     * Generates or retrieves RSA key pair for the user.
     * Keys are stored securely in preferences.
     *
     * @return Base64 encoded public key
     * @throws RuntimeException if key generation fails
     */
    public String generateUserKeys() {
        try {
            Log.d(TAG, "Starting key generation process");
            // Check for existing keys
            String existingPrivateKey = preferenceManager.getString("PRIVATE_KEY");
            String existingPublicKey = preferenceManager.getString(Constants.KEY_PUBLIC_KEY);
            Log.d(TAG, "Existing keys - Private: " + (existingPrivateKey != null) + ", Public: " + (existingPublicKey != null));

            // Return existing public key if both keys exist
            if (existingPrivateKey != null && existingPublicKey != null) {
                Log.d(TAG, "Using existing keys");
                return existingPublicKey;
            }

            // Generate new RSA key pair
            Log.d(TAG, "Generating new RSA key pair");
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(RSA_KEY_SIZE);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            // Save private key
            byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();
            String privateKeyString = Base64.encodeToString(privateKeyBytes, Base64.NO_WRAP);
            preferenceManager.putString("PRIVATE_KEY", privateKeyString);
            Log.d(TAG, "Stored new private key, length: " + privateKeyString.length());

            // Save and return public key
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

    /**
     * Retrieves the private key from preferences and converts it to a PrivateKey object.
     *
     * @return PrivateKey object for decryption
     * @throws RuntimeException if private key is missing or invalid
     */
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
     * Generates a new AES key for message encryption.
     *
     * @return SecretKey object for AES encryption
     * @throws RuntimeException if key generation fails
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
     * Encrypts a message using AES encryption with a random IV.
     *
     * @param message Message to encrypt
     * @param aesKey AES key for encryption
     * @return Base64 encoded string containing IV and encrypted message
     * @throws RuntimeException if encryption fails
     */
    public String encryptMessage(String message, SecretKey aesKey) {
        try {
            // Create and initialize IV
            byte[] iv = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // Initialize cipher for encryption
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec);

            // Perform encryption
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
     * Decrypts an AES encrypted message.
     *
     * @param encryptedMessage Base64 encoded string containing IV and encrypted message
     * @param aesKey AES key for decryption
     * @return Decrypted message string
     * @throws RuntimeException if decryption fails
     */
    public String decryptMessage(String encryptedMessage, SecretKey aesKey) {
        try {
            byte[] combined = Base64.decode(encryptedMessage, Base64.NO_WRAP);

            // Separate IV and encrypted data
            byte[] iv = new byte[16];
            byte[] encrypted = new byte[combined.length - 16];
            System.arraycopy(combined, 0, iv, 0, 16);
            System.arraycopy(combined, 16, encrypted, 0, encrypted.length);

            // Initialize cipher for decryption
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, ivSpec);

            // Perform decryption
            byte[] decryptedBytes = cipher.doFinal(encrypted);
            return new String(decryptedBytes);
        } catch (Exception e) {
            Log.e(TAG, "Error decrypting message", e);
            throw new RuntimeException("Failed to decrypt message", e);
        }
    }

    /**
     * Encrypts an AES key using RSA public key encryption for secure key exchange.
     *
     * @param aesKey AES key to encrypt
     * @param recipientPublicKeyString Recipient's public key as Base64 string
     * @return Encrypted AES key as Base64 string
     * @throws RuntimeException if encryption fails
     */
    public String encryptAESKey(SecretKey aesKey, String recipientPublicKeyString) {
        try {
            Log.d(TAG, "Encrypting AES key with recipient's public key");

            // Convert public key string to PublicKey object
            byte[] publicKeyBytes = Base64.decode(recipientPublicKeyString, Base64.NO_WRAP);
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            // Encrypt AES key with RSA
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedKey = cipher.doFinal(aesKey.getEncoded());
            return Base64.encodeToString(encryptedKey, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting AES key", e);
            throw new RuntimeException("Failed to encrypt AES key", e);
        }
    }

    /**
     * Decrypts an RSA encrypted AES key using the private key.
     *
     * @param encryptedKeyString Encrypted AES key as Base64 string
     * @return Decrypted AES key
     * @throws RuntimeException if decryption fails or private key is missing
     */
    public SecretKey decryptAESKey(String encryptedKeyString) {
        try {
            Log.d(TAG, "Decrypting AES key");

            // Retrieve and verify private key
            String privateKeyString = preferenceManager.getString("PRIVATE_KEY");
            if (privateKeyString == null) {
                throw new RuntimeException("Private key not found");
            }

            // Convert private key string to PrivateKey object
            byte[] privateKeyBytes = Base64.decode(privateKeyString, Base64.NO_WRAP);
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

            // Decrypt AES key
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