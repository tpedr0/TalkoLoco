package com.example.talkoloco.controllers;

import android.util.Log;

import com.example.talkoloco.models.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

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
    public void saveUser(User user, OnSuccessListener<Void> onSuccessListener,
                         OnFailureListener onFailureListener) {
        if (user.getUserId() == null) {
            Log.e(TAG, "Cannot save user: userId is null");
            onFailureListener.onFailure(new IllegalArgumentException("User ID cannot be null"));
            return;
        }

        Log.d(TAG, "Saving user with ID: " + user.getUserId());

        db.collection(USERS_COLLECTION)
                .document(user.getUserId())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User successfully saved");
                    onSuccessListener.onSuccess(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving user", e);
                    onFailureListener.onFailure(e);
                });
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

        db.collection(USERS_COLLECTION)
                .document(userId)
                .update(
                        "name", name,
                        "profilePictureUrl", profilePictureUrl
                )
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
     * retrieves the user data from the Firestore database by the user ID.
     *
     * @param userId               the ID of the user to be retrieved
     * @param onSuccessListener    the listener for the successful retrieve operation
     * @param onFailureListener    the listener for the failed retrieve operation
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