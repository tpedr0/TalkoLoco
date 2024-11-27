package com.example.talkoloco.controllers;

import android.content.Context;
import android.util.Log;

import com.example.talkoloco.models.User;
import com.example.talkoloco.utils.Constants;
import com.example.talkoloco.utils.PreferenceManager;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import com.example.talkoloco.utils.Hash;
import java.util.HashMap;
import java.util.Map;

/**
 * the UserController class is a singleton controller that manages the user-related operations in the Firestore database.
 */
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
    /**
     * saves the user data to the Firestore database.
     *
     * @param user              the user object to be saved
     * @param onSuccessListener the listener for the successful save operation
     * @param onFailureListener the listener for the failed save operation
     */
    public void saveUser(User user, Context context, OnSuccessListener<Void> onSuccessListener,
                         OnFailureListener onFailureListener) {
        if (user.getUserId() == null) {
            onFailureListener.onFailure(new IllegalArgumentException("User ID cannot be null"));
            return;
        }

        // Hash the phone number for Firestore storage
        String plainPhoneNumber = user.getPhoneNumber_display();
        if (plainPhoneNumber != null) {
            user.setPhoneNumber_hash(Hash.hashPhoneNumber(plainPhoneNumber));

            // Save the plaintext phone number locally
            PreferenceManager preferenceManager = new PreferenceManager(context);
            preferenceManager.putString(Constants.KEY_PHONE_NUMBER, plainPhoneNumber);
        }

        // Prepare Firestore data
        Map<String, Object> userData = new HashMap<>();
        userData.put(Constants.KEY_USER_ID, user.getUserId());
        userData.put(Constants.KEY_PHONE_NUMBER, user.getPhoneNumber_hash()); // Store the hash
        userData.put(Constants.KEY_NAME, user.getName());
        userData.put(Constants.KEY_PROFILE_PICTURE, user.getProfilePictureUrl());
        userData.put(Constants.KEY_CREATED_AT, user.getCreatedAt());
        userData.put(Constants.KEY_LAST_LOGIN, user.getLastLoginAt());

        // Save to Firestore
        db.collection(Constants.KEY_COLLECTION_USERS)
                .document(user.getUserId())
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User saved successfully");
                    onSuccessListener.onSuccess(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving user", e);
                    onFailureListener.onFailure(e);
                });
    }


    public String getDisplayPhoneNumber(Context context) {
        PreferenceManager preferenceManager = new PreferenceManager(context);
        return preferenceManager.getString(Constants.KEY_PHONE_NUMBER);
    }

    /**
     * updates the user's name and profile picture URL in the Firestore database.
     *
     * @param userId                the ID of the user to be updated
     * @param name                  the new name of the user
     * @param profilePictureUrl     the new profile picture URL of the user
     * @param onSuccessListener     the listener for the successful update operation
     * @param onFailureListener     the listener for the failed update operation
     */
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

    /**
     * deletes the user data from the Firestore database.
     *
     * @param userId               the ID of the user to be deleted
     * @param onSuccessListener    the listener for the successful delete operation
     * @param onFailureListener    the listener for the failed delete operation
     */
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

    /**
     * checks if a user with the given ID exists in the Firestore database.
     *
     * @param userId               the ID of the user to be checked
     * @param onSuccessListener    the listener for the successful check operation
     * @param onFailureListener    the listener for the failed check operation
     */
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

}