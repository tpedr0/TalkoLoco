package com.example.talkoloco.controllers;

import android.content.Context;
import android.util.Log;
import android.util.Base64;

import org.signal.libsignal.protocol.IdentityKey;
import org.signal.libsignal.protocol.IdentityKeyPair;
import org.signal.libsignal.protocol.InvalidKeyException;
import org.signal.libsignal.protocol.SessionBuilder;
import org.signal.libsignal.protocol.SessionCipher;
import org.signal.libsignal.protocol.SignalProtocolAddress;
import org.signal.libsignal.protocol.ecc.Curve;
import org.signal.libsignal.protocol.ecc.ECKeyPair;
import org.signal.libsignal.protocol.message.CiphertextMessage;
import org.signal.libsignal.protocol.message.PreKeySignalMessage;
import org.signal.libsignal.protocol.state.PreKeyBundle;
import org.signal.libsignal.protocol.state.PreKeyRecord;
import org.signal.libsignal.protocol.state.SignedPreKeyRecord;
import org.signal.libsignal.protocol.state.impl.InMemorySignalProtocolStore;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

public class SignalEncryptionController {
    private static final String TAG = "SignalEncryption";
    private static volatile SignalEncryptionController instance;
    private final InMemorySignalProtocolStore protocolStore;
    private final Map<String, SessionCipher> sessionCiphers;
    private final Context context;

    // Private constructor
    private SignalEncryptionController(Context context) {
        this.context = context.getApplicationContext();

        // Generate key pair
        ECKeyPair keyPair = Curve.generateKeyPair();
        IdentityKey identityKey = new IdentityKey(keyPair.getPublicKey());
        IdentityKeyPair identityKeyPair = new IdentityKeyPair(identityKey, keyPair.getPrivateKey());

        // Generate registration ID (1 to 16380 is the range used by Signal)
        int registrationId = new SecureRandom().nextInt(16380) + 1;

        // Initialize protocol store
        this.protocolStore = new InMemorySignalProtocolStore(identityKeyPair, registrationId);
        this.sessionCiphers = new HashMap<>();

        Log.d(TAG, "SignalEncryptionController initialized successfully");
    }

    // Thread-safe singleton implementation
    public static SignalEncryptionController getInstance(Context context) {
        if (instance == null) {
            synchronized (SignalEncryptionController.class) {
                if (instance == null) {
                    instance = new SignalEncryptionController(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public void initializeSession(String userId, PreKeyBundle preKeyBundle) {
        try {
            SignalProtocolAddress address = new SignalProtocolAddress(userId, 1);

            // Save the identity key before initializing the session
            IdentityKey theirIdentityKey = preKeyBundle.getIdentityKey();
            protocolStore.saveIdentity(address, theirIdentityKey);

            SessionBuilder sessionBuilder = new SessionBuilder(protocolStore, address);
            sessionBuilder.process(preKeyBundle);

            SessionCipher sessionCipher = new SessionCipher(protocolStore, address);
            sessionCiphers.put(userId, sessionCipher);

            Log.d(TAG, "Session initialized for user: " + userId);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing session", e);
            throw new RuntimeException("Failed to initialize session", e);
        }
    }

    public String encryptMessage(String message, String receiverId) {
        try {
            SessionCipher cipher = sessionCiphers.get(receiverId);
            if (cipher == null) {
                throw new IllegalStateException("No session established with recipient: " + receiverId);
            }

            CiphertextMessage encryptedMessage = cipher.encrypt(message.getBytes());
            return Base64.encodeToString(encryptedMessage.serialize(), Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting message", e);
            throw new RuntimeException("Failed to encrypt message", e);
        }
    }

    public String decryptMessage(String encryptedMessage, String senderId) {
        try {
            SessionCipher cipher = sessionCiphers.get(senderId);
            if (cipher == null) {
                throw new IllegalStateException("No session established with sender: " + senderId);
            }

            byte[] messageBytes = Base64.decode(encryptedMessage, Base64.DEFAULT);
            byte[] decryptedMessage = cipher.decrypt(new PreKeySignalMessage(messageBytes));
            return new String(decryptedMessage);
        } catch (Exception e) {
            Log.e(TAG, "Error decrypting message", e);
            throw new RuntimeException("Failed to decrypt message", e);
        }
    }

    public PreKeyBundle generatePreKeyBundle() {
        try {
            // Generate pre-keys
            PreKeyRecord preKey = generatePreKey(1);
            SignedPreKeyRecord signedPreKey = generateSignedPreKey(
                    protocolStore.getIdentityKeyPair(), 1);

            // Store pre-keys
            protocolStore.storePreKey(1, preKey);
            protocolStore.storeSignedPreKey(1, signedPreKey);

            // Create pre-key bundle
            PreKeyBundle preKeyBundle = new PreKeyBundle(
                    protocolStore.getLocalRegistrationId(),
                    1, // deviceId
                    1, // preKeyId
                    preKey.getKeyPair().getPublicKey(),
                    1, // signedPreKeyId
                    signedPreKey.getKeyPair().getPublicKey(),
                    signedPreKey.getSignature(),
                    protocolStore.getIdentityKeyPair().getPublicKey());

            // Add logging for debugging
            Log.d(TAG, "PreKeyBundle generated: " + preKeyBundle.toString());

            // Return the pre-key bundle
            return preKeyBundle;
        } catch (Exception e) {
            Log.e(TAG, "Error generating pre-key bundle", e);
            throw new RuntimeException("Failed to generate pre-key bundle", e);
        }
    }


    private PreKeyRecord generatePreKey(int id) throws InvalidKeyException {
        ECKeyPair keyPair = Curve.generateKeyPair();
        return new PreKeyRecord(id, keyPair);
    }

    private SignedPreKeyRecord generateSignedPreKey(IdentityKeyPair identityKeyPair, int id)
            throws InvalidKeyException {
        ECKeyPair keyPair = Curve.generateKeyPair();
        byte[] signature = Curve.calculateSignature(
                identityKeyPair.getPrivateKey(),
                keyPair.getPublicKey().serialize());
        return new SignedPreKeyRecord(id, System.currentTimeMillis(), keyPair, signature);
    }

    // Method to reset the singleton instance (useful for testing or user logout)
    public static void resetInstance() {
        instance = null;
    }
}