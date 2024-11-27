package com.example.talkoloco.utils;

import android.util.Base64;
import android.util.Log;

import org.signal.libsignal.protocol.IdentityKey;
import org.signal.libsignal.protocol.ecc.Curve;
import org.signal.libsignal.protocol.ecc.ECPublicKey;
import org.signal.libsignal.protocol.state.PreKeyBundle;

import java.util.HashMap;
import java.util.Map;

public class PreKeyBundleUtil {
    private static final String TAG = "PreKeyBundleUtil";

    public static PreKeyBundle fromFirestore(Map<String, Object> data) {
        try {
            // Convert Long values to int
            int registrationId = ((Long) data.get("registrationId")).intValue();
            int deviceId = ((Long) data.get("deviceId")).intValue();
            int preKeyId = ((Long) data.get("preKeyId")).intValue();
            int signedPreKeyId = ((Long) data.get("signedPreKeyId")).intValue();

            // Decode the Base64 strings to byte arrays
            byte[] preKeyPublicBytes = Base64.decode((String) data.get("preKeyPublic"), Base64.DEFAULT);
            byte[] signedPreKeyPublicBytes = Base64.decode((String) data.get("signedPreKeyPublic"), Base64.DEFAULT);
            byte[] signedPreKeySignatureBytes = Base64.decode((String) data.get("signedPreKeySignature"), Base64.DEFAULT);
            byte[] identityKeyBytes = Base64.decode((String) data.get("identityKey"), Base64.DEFAULT);

            // Reconstruct the keys
            ECPublicKey preKeyPublic = Curve.decodePoint(preKeyPublicBytes, 0);
            ECPublicKey signedPreKeyPublic = Curve.decodePoint(signedPreKeyPublicBytes, 0);
            IdentityKey identityKey = new IdentityKey(identityKeyBytes, 0);

            // Create new PreKeyBundle
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
        } catch (Exception e) {
            Log.e(TAG, "Error creating PreKeyBundle from Firestore data", e);
            throw new RuntimeException("Failed to create PreKeyBundle", e);
        }
    }

    public static Map<String, Object> toFirestore(PreKeyBundle bundle) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("registrationId", bundle.getRegistrationId());
            data.put("deviceId", bundle.getDeviceId());
            data.put("preKeyId", bundle.getPreKeyId());
            data.put("signedPreKeyId", bundle.getSignedPreKeyId());

            // Convert keys to Base64 strings
            data.put("preKeyPublic", Base64.encodeToString(
                    bundle.getPreKey().serialize(),
                    Base64.DEFAULT));
            data.put("signedPreKeyPublic", Base64.encodeToString(
                    bundle.getSignedPreKey().serialize(),
                    Base64.DEFAULT));
            data.put("signedPreKeySignature", Base64.encodeToString(
                    bundle.getSignedPreKeySignature(),
                    Base64.DEFAULT));
            data.put("identityKey", Base64.encodeToString(
                    bundle.getIdentityKey().serialize(),
                    Base64.DEFAULT));

            return data;
        } catch (Exception e) {
            Log.e(TAG, "Error converting PreKeyBundle to Firestore data", e);
            throw new RuntimeException("Failed to convert PreKeyBundle", e);
        }
    }
}