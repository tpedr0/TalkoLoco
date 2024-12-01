package com.example.talkoloco.controllers;

import android.content.Context;
import android.util.Log;

import com.example.talkoloco.models.User;
import com.example.talkoloco.utils.Constants;
import com.example.talkoloco.utils.PreferenceManager;
import com.example.talkoloco.utils.KeyManager;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import com.example.talkoloco.utils.Hash;
import java.util.HashMap;
import java.util.Map;

/**
 * The UserController class is a singleton controller that manages the user-related operations in the Firestore database.
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
     * @param context          the application context
     * @param onSuccessListener the listener for the successful save operation
     * @param onFailureListener the listener for the failed save operation
     */
    public void saveUser(User user, Context context, OnSuccessListener<Void> onSuccessListener,
                         OnFailureListener onFailureListener) {
        if (user.getUserId() == null) {
            onFailureListener.onFailure(new IllegalArgumentException("User ID cannot be null"));
            return;
        }

        // Initialize PreferenceManager
        PreferenceManager preferenceManager = new PreferenceManager(context);

        // Generate encryption keys if they don't exist
        KeyManager keyManager = new KeyManager(context);
        String publicKey = preferenceManager.getString(Constants.KEY_PUBLIC_KEY);
        if (publicKey == null) {
            publicKey = keyManager.generateUserKeys();
        }
        user.setPublicKey(publicKey);

        // Hash the phone number for Firestore storage
        String plainPhoneNumber = user.getPhoneNumber_display();
        if (plainPhoneNumber != null) {
            user.setPhoneNumber_hash(Hash.hashPhoneNumber(plainPhoneNumber));

            // Save the plaintext phone number locally
            preferenceManager.putString(Constants.KEY_PHONE_NUMBER, plainPhoneNumber);
        }

        // Prepare Firestore data
        Map<String, Object> userData = new HashMap<>();
        userData.put(Constants.KEY_USER_ID, user.getUserId());
        userData.put(Constants.KEY_PHONE_NUMBER, user.getPhoneNumber_hash());
        userData.put(Constants.KEY_NAME, user.getName());
        userData.put(Constants.KEY_PROFILE_PICTURE, user.getProfilePictureUrl());
        userData.put(Constants.KEY_CREATED_AT, user.getCreatedAt());
        userData.put(Constants.KEY_LAST_LOGIN, user.getLastLoginAt());
        userData.put(Constants.KEY_PUBLIC_KEY, user.getPublicKey());

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

    /**
     * Gets the display phone number from local storage.
     *
     * @param context The application context
     * @return The display phone number
     */
    public String getDisplayPhoneNumber(Context context) {
        PreferenceManager preferenceManager = new PreferenceManager(context);
        return preferenceManager.getString(Constants.KEY_PHONE_NUMBER);
    }

    /**
     * Updates the user's name and profile picture URL in the Firestore database.
     *
     * @param userId            the ID of the user to be updated
     * @param name              the new name of the user
     * @param profilePictureUrl the new profile picture URL of the user
     * @param onSuccessListener the listener for the successful update operation
     * @param onFailureListener the listener for the failed update operation
     */
    public void updateUserProfile(String userId, String name, String profilePictureUrl,
                                  OnSuccessListener<Void> onSuccessListener,
                                  OnFailureListener onFailureListener) {
        Log.d(TAG, "Updating profile for user: " + userId);

        Map<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_NAME, name);
        updates.put(Constants.KEY_PROFILE_PICTURE, profilePictureUrl);

        db.collection(Constants.KEY_COLLECTION_USERS)
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

    /**
     * Updates the user's phone number in the Firestore database.
     *
     * @param userId            the ID of the user to be updated
     * @param phoneNumber       the new phone number
     * @param onSuccessListener the listener for the successful update operation
     * @param onFailureListener the listener for the failed update operation
     */
    public void updatePhoneNumber(String userId, String phoneNumber,
                                  OnSuccessListener<Void> onSuccessListener,
                                  OnFailureListener onFailureListener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_PHONE_NUMBER, Hash.hashPhoneNumber(phoneNumber));

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

    /**
     * Updates specified fields for a user in the Firestore database.
     *
     * @param userId            the ID of the user to be updated
     * @param updates           map of fields to update and their new values
     * @param onSuccessListener the listener for the successful update operation
     * @param onFailureListener the listener for the failed update operation
     */
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

    /**
     * Updates the last login timestamp for a user.
     *
     * @param userId            the ID of the user
     * @param onSuccessListener the listener for the successful update operation
     * @param onFailureListener the listener for the failed update operation
     */
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

    /**
     * Retrieves a user by their ID from the Firestore database.
     *
     * @param userId            the ID of the user to retrieve
     * @param onSuccessListener the listener for the successful retrieval operation
     * @param onFailureListener the listener for the failed retrieval operation
     */
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
     * Deletes a user from the Firestore database.
     *
     * @param userId            the ID of the user to be deleted
     * @param onSuccessListener the listener for the successful delete operation
     * @param onFailureListener the listener for the failed delete operation
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
     * Checks if a user with the given ID exists in the Firestore database.
     *
     * @param userId            the ID of the user to be checked
     * @param onSuccessListener the listener for the successful check operation
     * @param onFailureListener the listener for the failed check operation
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

    /**
     * Finds a user by their phone number in the Firestore database.
     *
     * @param phoneNumber       the phone number to search for
     * @param onSuccessListener the listener for the successful retrieval operation
     * @param onFailureListener the listener for the failed retrieval operation
     */
    public void getUserByPhoneNumber(String phoneNumber, OnSuccessListener<User> onSuccessListener,
                                     OnFailureListener onFailureListener) {
        Log.d(TAG, "Looking up user by phone number");

        // Hash the phone number before querying
        String hashedPhoneNumber = Hash.hashPhoneNumber(phoneNumber);

        db.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_PHONE_NUMBER, hashedPhoneNumber)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        User user = querySnapshot.getDocuments().get(0).toObject(User.class);
                        if (user != null) {
                            // Set the user ID from the document ID
                            user.setUserId(querySnapshot.getDocuments().get(0).getId());
                            Log.d(TAG, "User found by phone number");
                            onSuccessListener.onSuccess(user);
                        } else {
                            Log.d(TAG, "User document exists but couldn't be converted to User object");
                            onFailureListener.onFailure(new Exception("User not found"));
                        }
                    } else {
                        Log.d(TAG, "No user found with phone number: " + phoneNumber);
                        onFailureListener.onFailure(new Exception("User not found"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding user by phone number", e);
                    onFailureListener.onFailure(e);
                });
    }

    public void doesPhoneNumberExist(String phoneNumber, OnSuccessListener<Boolean> onSuccess, OnFailureListener onFailure) {
        FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_PHONE_NUMBER, phoneNumber)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    onSuccess.onSuccess(!querySnapshot.isEmpty());
                })
                .addOnFailureListener(onFailure);
    }

    public void createDummyUser(String phoneNumber, OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> dummyUser = new HashMap<>();
        dummyUser.put("phoneNumber", phoneNumber);
        dummyUser.put("name", "Dummy User");

        db.collection("dummy_users")
                .add(dummyUser)
                .addOnSuccessListener(documentReference -> onSuccessListener.onSuccess(null))
                .addOnFailureListener(onFailureListener);
    }
}