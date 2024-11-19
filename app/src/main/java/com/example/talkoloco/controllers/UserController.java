package com.example.talkoloco.controllers;

import android.util.Log;

import com.example.talkoloco.models.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class UserController {
    private final FirebaseFirestore db;
    private static UserController instance;
    private static final String TAG = "UserController";
    private static final String USERS_COLLECTION = "users";

    private UserController() {
        db = FirebaseFirestore.getInstance();
    }

    public static UserController getInstance() {
        if (instance == null) {
            instance = new UserController();
        }
        return instance;
    }



    public void saveUser(User user, OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        if (user.getUserId() == null) {
            Log.e(TAG, "User ID is null");
            onFailureListener.onFailure(new IllegalArgumentException("User ID cannot be null"));
            return;
        }

        Log.d(TAG, "Attempting to save user: " + user.getUserId());

        db.collection(USERS_COLLECTION)
                .document(user.getUserId())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User saved successfully");
                    onSuccessListener.onSuccess(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving user", e);
                    onFailureListener.onFailure(e);
                });
    }

    public void updateUserProfile(String userId, String name, String profilePictureUrl,
                                  OnSuccessListener<Void> onSuccessListener,
                                  OnFailureListener onFailureListener) {
        Log.d(TAG, "Updating profile for user: " + userId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("profilePictureUrl", profilePictureUrl);  // Can be null to remove profile picture

        db.collection(USERS_COLLECTION)
                .document(userId)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User profile successfully updated");
                    onSuccessListener.onSuccess(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user profile", e);
                    onFailureListener.onFailure(e);
                });
    }

    public void updateFields(String userId, Map<String, Object> updates,
                             OnSuccessListener<Void> onSuccessListener,
                             OnFailureListener onFailureListener) {
        if (userId == null) {
            onFailureListener.onFailure(new IllegalArgumentException("User ID cannot be null"));
            return;
        }

        Log.d(TAG, "Updating fields for user: " + userId);

        db.collection(USERS_COLLECTION)
                .document(userId)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User fields successfully updated");
                    onSuccessListener.onSuccess(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user fields", e);
                    onFailureListener.onFailure(e);
                });
    }

    public void updateLastLogin(String userId, OnSuccessListener<Void> onSuccessListener,
                                OnFailureListener onFailureListener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastLoginAt", System.currentTimeMillis());

        db.collection(USERS_COLLECTION)
                .document(userId)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener);
    }

    public void getUserById(String userId, OnSuccessListener<User> onSuccessListener,
                            OnFailureListener onFailureListener) {
        Log.d(TAG, "Fetching user with ID: " + userId);

        db.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        Log.d(TAG, "User successfully fetched");
                        onSuccessListener.onSuccess(user);
                    } else {
                        Log.d(TAG, "No user found with ID: " + userId);
                        onFailureListener.onFailure(new Exception("User not found"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user", e);
                    onFailureListener.onFailure(e);
                });
    }

    public void checkIfUserExists(String userId, OnSuccessListener<Boolean> onSuccessListener,
                                  OnFailureListener onFailureListener) {
        Log.d(TAG, "Checking if user exists: " + userId);

        db.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    boolean exists = documentSnapshot.exists();
                    Log.d(TAG, "User exists: " + exists);
                    onSuccessListener.onSuccess(exists);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking if user exists", e);
                    onFailureListener.onFailure(e);
                });
    }

    public void deleteUser(String userId, OnSuccessListener<Void> onSuccessListener,
                           OnFailureListener onFailureListener) {
        Log.d(TAG, "Deleting user with ID: " + userId);

        db.collection(USERS_COLLECTION)
                .document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User successfully deleted");
                    onSuccessListener.onSuccess(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting user", e);
                    onFailureListener.onFailure(e);
                });
    }
}