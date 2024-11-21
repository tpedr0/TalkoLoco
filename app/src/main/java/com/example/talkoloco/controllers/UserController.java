package com.example.talkoloco.controllers;

import android.util.Log;

import com.example.talkoloco.models.User;
import com.example.talkoloco.utils.Constants;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

//signal  lib

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
            onFailureListener.onFailure(new IllegalArgumentException("User ID cannot be null"));
            return;
        }

        // Get phone number from Auth if not set
        if (user.getPhoneNumber() == null) {
            String phoneNumber = AuthController.getInstance().getCurrentUser();
            user.setPhoneNumber(phoneNumber);
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put(Constants.KEY_USER_ID, user.getUserId());
        userData.put(Constants.KEY_PHONE_NUMBER, user.getPhoneNumber());
        userData.put(Constants.KEY_NAME, user.getName());
        userData.put(Constants.KEY_PROFILE_PICTURE, user.getProfilePictureUrl());
        userData.put(Constants.KEY_CREATED_AT, user.getCreatedAt());
        userData.put(Constants.KEY_LAST_LOGIN, user.getLastLoginAt());

        db.collection(Constants.KEY_COLLECTION_USERS)
                .document(user.getUserId())
                .set(userData)
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
        updates.put(Constants.KEY_NAME, name);
        updates.put(Constants.KEY_PROFILE_PICTURE, profilePictureUrl);

        db.collection(Constants.KEY_COLLECTION_USERS)  // Use constant instead of USERS_COLLECTION
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

    public void updatePhoneNumber(String userId, String phoneNumber,
                                  OnSuccessListener<Void> onSuccessListener,
                                  OnFailureListener onFailureListener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_PHONE_NUMBER, phoneNumber);

        db.collection(Constants.KEY_COLLECTION_USERS)
                .document(userId)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Phone number updated successfully");
                    onSuccessListener.onSuccess(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating phone number", e);
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