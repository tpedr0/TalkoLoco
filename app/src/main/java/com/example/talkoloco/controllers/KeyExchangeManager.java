package com.example.talkoloco.controllers;

import android.util.Base64;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import org.signal.libsignal.protocol.IdentityKey;
import org.signal.libsignal.protocol.InvalidKeyException;
import org.signal.libsignal.protocol.ecc.Curve;
import org.signal.libsignal.protocol.ecc.ECPublicKey;
import org.signal.libsignal.protocol.state.PreKeyBundle;

import java.util.HashMap;
import java.util.Map;

public class KeyExchangeManager {
    private static final String TAG = "KeyExchangeManager";
    private static final String KEY_BUNDLES_COLLECTION = "key_bundles";
    private final FirebaseFirestore db;

    // FireStore data keys
    private static final String KEY_REGISTRATION_ID = "registrationId";
    private static final String KEY_DEVICE_ID = "deviceId";
    private static final String KEY_PREKEY_ID = "preKeyId";
    private static final String KEY_PREKEY_PUBLIC = "preKeyPublic";
    private static final String KEY_SIGNED_PREKEY_ID = "signedPreKeyId";
    private static final String KEY_SIGNED_PREKEY_PUBLIC = "signedPreKeyPublic";
    private static final String KEY_SIGNED_PREKEY_SIGNATURE = "signedPreKeySignature";
    private static final String KEY_IDENTITY_KEY = "identityKey";

    public KeyExchangeManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void storePreKeyBundle(String userId, PreKeyBundle preKeyBundle,
                                  OnSuccessListener<Void> onSuccessListener,
                                  OnFailureListener onFailureListener) {
        try {
            Map<String, Object> bundleData = new HashMap<>();
            bundleData.put(KEY_REGISTRATION_ID, preKeyBundle.getRegistrationId());
            bundleData.put(KEY_DEVICE_ID, preKeyBundle.getDeviceId());
            bundleData.put(KEY_PREKEY_ID, preKeyBundle.getPreKeyId());
            bundleData.put(KEY_PREKEY_PUBLIC, Base64.encodeToString(
                    preKeyBundle.getPreKey().serialize(),
                    Base64.DEFAULT));
            bundleData.put(KEY_SIGNED_PREKEY_ID, preKeyBundle.getSignedPreKeyId());
            bundleData.put(KEY_SIGNED_PREKEY_PUBLIC, Base64.encodeToString(
                    preKeyBundle.getSignedPreKey().serialize(),
                    Base64.DEFAULT));
            bundleData.put(KEY_SIGNED_PREKEY_SIGNATURE, Base64.encodeToString(
                    preKeyBundle.getSignedPreKeySignature(),
                    Base64.DEFAULT));
            bundleData.put(KEY_IDENTITY_KEY, Base64.encodeToString(
                    preKeyBundle.getIdentityKey().serialize(),
                    Base64.DEFAULT));

            db.collection(KEY_BUNDLES_COLLECTION)
                    .document(userId)
                    .set(bundleData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "PreKeyBundle stored for user: " + userId);
                        onSuccessListener.onSuccess(aVoid);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error storing PreKeyBundle", e);
                        onFailureListener.onFailure(e);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error preparing PreKeyBundle data", e);
            onFailureListener.onFailure(e);
        }
    }

    public void retrievePreKeyBundle(String userId,
                                     OnSuccessListener<PreKeyBundle> onSuccessListener,
                                     OnFailureListener onFailureListener) {
        db.collection(KEY_BUNDLES_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    try {
                        if (document.exists()) {
                            Map<String, Object> data = document.getData();
                            if (data == null) {
                                throw new Exception("No data found in document");
                            }

                            PreKeyBundle preKeyBundle = reconstructPreKeyBundle(data);
                            onSuccessListener.onSuccess(preKeyBundle);
                        } else {
                            onFailureListener.onFailure(
                                    new Exception("No PreKeyBundle found for user: " + userId));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reconstructing PreKeyBundle", e);
                        onFailureListener.onFailure(e);
                    }
                })
                .addOnFailureListener(onFailureListener);
    }

    private PreKeyBundle reconstructPreKeyBundle(Map<String, Object> data) throws Exception {
        try {
            // Extract the values from Firestore data
            int registrationId = ((Long) data.get(KEY_REGISTRATION_ID)).intValue();
            int deviceId = ((Long) data.get(KEY_DEVICE_ID)).intValue();
            int preKeyId = ((Long) data.get(KEY_PREKEY_ID)).intValue();
            int signedPreKeyId = ((Long) data.get(KEY_SIGNED_PREKEY_ID)).intValue();

            // Decode the Base64 strings back to byte arrays
            byte[] preKeyPublicBytes = Base64.decode(
                    (String) data.get(KEY_PREKEY_PUBLIC),
                    Base64.DEFAULT);
            byte[] signedPreKeyPublicBytes = Base64.decode(
                    (String) data.get(KEY_SIGNED_PREKEY_PUBLIC),
                    Base64.DEFAULT);
            byte[] signedPreKeySignatureBytes = Base64.decode(
                    (String) data.get(KEY_SIGNED_PREKEY_SIGNATURE),
                    Base64.DEFAULT);
            byte[] identityKeyBytes = Base64.decode(
                    (String) data.get(KEY_IDENTITY_KEY),
                    Base64.DEFAULT);

            // Reconstruct the keys from bytes
            ECPublicKey preKeyPublic = Curve.decodePoint(preKeyPublicBytes, 0);
            ECPublicKey signedPreKeyPublic = Curve.decodePoint(signedPreKeyPublicBytes, 0);
            IdentityKey identityKey = new IdentityKey(identityKeyBytes, 0);

            // Create and return the PreKeyBundle
            return new PreKeyBundle(
                    registrationId,
                    deviceId,
                    preKeyId,
                    preKeyPublic,
                    signedPreKeyId,
                    signedPreKeyPublic,
                    signedPreKeySignatureBytes,
                    identityKey
            );
        } catch (InvalidKeyException e) {
            Log.e(TAG, "Error reconstructing keys", e);
            throw new Exception("Failed to reconstruct PreKeyBundle: Invalid key", e);
        } catch (Exception e) {
            Log.e(TAG, "Error rebuilding PreKeyBundle", e);
            throw new Exception("Failed to reconstruct PreKeyBundle", e);
        }
    }

    public void deletePreKeyBundle(String userId,
                                   OnSuccessListener<Void> onSuccessListener,
                                   OnFailureListener onFailureListener) {
        db.collection(KEY_BUNDLES_COLLECTION)
                .document(userId)
                .delete()
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener);
    }
}