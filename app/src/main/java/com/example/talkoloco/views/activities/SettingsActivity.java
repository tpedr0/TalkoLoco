package com.example.talkoloco.views.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.talkoloco.R;
import com.example.talkoloco.controllers.AuthController;
import com.example.talkoloco.controllers.NavigationController;
import com.example.talkoloco.controllers.UserController;
import com.example.talkoloco.databinding.ActivitySettingsBinding;
import com.example.talkoloco.models.User;
import com.example.talkoloco.models.UserStatus;
import com.example.talkoloco.utils.Constants;
import com.example.talkoloco.utils.ImageHandler;
import com.example.talkoloco.utils.PreferenceManager;
import com.example.talkoloco.utils.ThemeManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {
    private ActivitySettingsBinding binding;
    private NavigationController navigationController;
    private AuthController authController;
    private UserController userController;
    private Uri selectedImageUri;
    private User currentUser;
    private boolean isNameEditing = false;
    private boolean isPhoneEditing = false;
    private String originalPhoneNumber;
    private ArrayAdapter<String> statusAdapter;
    private static final String TAG = "SettingsActivity";
    private static final int VERIFY_PHONE_REQUEST = 100;
    private static final int DRAWABLE_RIGHT = 2;
    private ThemeManager themeManager;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    updateProfilePicture(selectedImageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        themeManager = ThemeManager.getInstance(this);

        // initialize controllers
        navigationController = new NavigationController(this);
        authController = AuthController.getInstance();
        userController = UserController.getInstance();

        // setup nav
        navigationController.setupNavigation(binding.bottomNavigationView);


        // setup features
        setupDeleteButton();
        setupNameEditing();
        setupPhoneEditing();
        setupStatusDropdown();
        setupDarkMode();
        setupSignOutButton();


        // setup profile picture click
        binding.profileIcon.setOnClickListener(v -> showProfilePictureOptions());

        // load user data
        loadUserData();
    }

    private void setupDarkMode() {
        // update button icon based on current mode
        binding.darkModeButton.setImageResource(
                themeManager.isDarkMode() ? R.drawable.ic_light_mode : R.drawable.ic_dark_mode
        );

        // sdd click listener for toggling dark mode
        binding.darkModeButton.setOnClickListener(v -> {
            boolean newDarkMode = !themeManager.isDarkMode(); // Toggle the current mode
            themeManager.setDarkMode(newDarkMode);           // Save preference
            themeManager.apply();                            // Apply theme globally
            recreate();                                      // Restart the activity to reflect changes
        });
    }


    private void setupStatusDropdown() {
        // initialize adapter with all statuses
        statusAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                UserStatus.getAllStatuses()
        );
        binding.statusDropdown.setAdapter(statusAdapter);

        // handle custom input
        binding.statusDropdown.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String newStatus = binding.statusDropdown.getText().toString().trim();
                if (!newStatus.isEmpty()) {
                    saveNewStatus(newStatus);
                    return true;
                }
            }
            return false;
        });

        // handle item selection
        binding.statusDropdown.setOnItemClickListener((parent, view, position, id) -> {
            String selectedStatus = parent.getItemAtPosition(position).toString();
            saveNewStatus(selectedStatus);
        });
    }


    private void setupNameEditing() {
        binding.nameInput.setEnabled(false);
        binding.nameInput.setFocusable(false);

        //add touch listener to the edit icon
        binding.editName.setOnClickListener(v -> {
            if (!isNameEditing) {
                //enable editing for nameInput
                binding.nameInput.setEnabled(true);
                binding.nameInput.setFocusableInTouchMode(true);
                binding.nameInput.requestFocus();
                binding.nameInput.setSelection(binding.nameInput.length()); // Move cursor to end
                showKeyboard(binding.nameInput);
                isNameEditing = true;
            }
        });

        //save changes
        binding.nameInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveNameChanges();
                return true;
            }
            return false;
        });

        //save changes when the input loses focus
        binding.nameInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && isNameEditing) {
                saveNameChanges();
            }
        });
    }

    private void saveNameChanges() {
        String newName = binding.nameInput.getText().toString().trim();
        if (!newName.isEmpty() && currentUser != null) {
            String userId = authController.getCurrentUserId();
            if (userId != null) {
                userController.updateUserProfile(
                        userId,
                        newName,
                        currentUser.getProfilePictureUrl(),
                        aVoid -> {
                            Toast.makeText(this, "Name updated successfully", Toast.LENGTH_SHORT).show();
                            binding.nameInput.setEnabled(false);
                            binding.nameInput.setFocusable(false);
                            hideKeyboard();
                            isNameEditing = false;
                            currentUser.setName(newName);
                        },
                        e -> Toast.makeText(this, "Failed to update name", Toast.LENGTH_SHORT).show()
                );
            }
        } else {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            binding.nameInput.setText(currentUser.getName());
        }
        binding.nameInput.setEnabled(false);
        binding.nameInput.setFocusable(false);
        hideKeyboard();
        isNameEditing = false;
    }

    private void setupPhoneEditing() {
        //disable editing for the phone number field
        binding.currentPhoneNumber.setEnabled(false);
        binding.currentPhoneNumber.setFocusable(false);

        //add click listener to the edit icon (was switched from touch listener)
        binding.editNumber.setOnClickListener(v -> {
            if (!isPhoneEditing) {
                //save original phone number
                originalPhoneNumber = binding.currentPhoneNumber.getText().toString();

                //enable editing for the phone number field
                binding.currentPhoneNumber.setEnabled(true);
                binding.currentPhoneNumber.setFocusableInTouchMode(true);
                binding.currentPhoneNumber.requestFocus();
                binding.currentPhoneNumber.setSelection(binding.currentPhoneNumber.length()); // Move cursor to end
                showKeyboard(binding.currentPhoneNumber);

                isPhoneEditing = true;
            }
        });

        //save the phone number
        binding.currentPhoneNumber.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                verifyAndSavePhoneNumber();
                return true;
            }
            return false;
        });

        //save the phone number when field loses focus
        binding.currentPhoneNumber.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && isPhoneEditing) {
                verifyAndSavePhoneNumber();
            }
        });
    }

    private void verifyAndSavePhoneNumber() {
        String newPhoneNumber = binding.currentPhoneNumber.getText().toString().trim();

        if (!newPhoneNumber.equals(originalPhoneNumber)) {
            // Show verification dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Verify New Phone Number")
                    .setMessage("To change your phone number, you'll need to verify the new number. Would you like to proceed?")
                    .setPositiveButton("Verify", (dialog, which) -> {
                        startPhoneVerification(newPhoneNumber);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        // Revert to original number
                        binding.currentPhoneNumber.setText(originalPhoneNumber);
                        resetPhoneEditState();
                    })
                    .setCancelable(false);

            AlertDialog dialog = builder.create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
            }
            dialog.show();
        } else {
            resetPhoneEditState();
        }
    }

    private void startPhoneVerification(String newPhoneNumber) {
        // Format phone number if needed
        String formattedPhoneNumber = formatPhoneNumber(newPhoneNumber);

        // Start verification process using AuthController
        authController.startPhoneNumberVerification(
                formattedPhoneNumber,
                this,
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                        // Usually won't be called since we're using manual code verification
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        Log.e(TAG, "Phone verification failed", e);
                        Toast.makeText(SettingsActivity.this,
                                "Verification failed. Please try again.",
                                Toast.LENGTH_SHORT).show();
                        binding.currentPhoneNumber.setText(originalPhoneNumber);
                        resetPhoneEditState();
                    }

                    @Override
                    public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                        // Launch VerificationActivity with the necessary data
                        Intent intent = new Intent(SettingsActivity.this, VerificationActivity.class);
                        intent.putExtra("verificationId", verificationId);
                        intent.putExtra("phoneNumber", formattedPhoneNumber);
                        intent.putExtra("isUpdating", true); // Flag to indicate this is a number update
                        startActivityForResult(intent, VERIFY_PHONE_REQUEST);
                    }
                }
        );
    }

    private void updateUserPhoneNumber(String newPhoneNumber) {
        String userId = authController.getCurrentUserId();
        if (userId != null && currentUser != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put(Constants.KEY_PHONE_NUMBER, newPhoneNumber);

            userController.updateFields(userId, updates,
                    aVoid -> {
                        Toast.makeText(this, "Phone number updated successfully", Toast.LENGTH_SHORT).show();
                        currentUser.setPhoneNumber(newPhoneNumber);
                    },
                    e -> {
                        Toast.makeText(this, "Failed to update phone number", Toast.LENGTH_SHORT).show();
                        binding.currentPhoneNumber.setText(originalPhoneNumber);
                        Log.e(TAG, "Failed to update phone number", e);
                    }
            );
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VERIFY_PHONE_REQUEST) {
            if (resultCode == RESULT_OK) {
                // Phone verification was successful, update the user's phone number
                String newPhoneNumber = binding.currentPhoneNumber.getText().toString().trim();
                updateUserPhoneNumber(newPhoneNumber);
            } else {
                // Verification was cancelled or failed
                binding.currentPhoneNumber.setText(originalPhoneNumber);
                Toast.makeText(this, "Phone number update cancelled", Toast.LENGTH_SHORT).show();
            }
            resetPhoneEditState();
        }
    }

    private void updatePhoneNumber(String newPhoneNumber) {
        String userId = authController.getCurrentUserId();
        if (userId != null) {
            userController.updatePhoneNumber(userId, newPhoneNumber,
                    aVoid -> {
                        Toast.makeText(this, "Phone number updated successfully", Toast.LENGTH_SHORT).show();
                        currentUser.setPhoneNumber(newPhoneNumber);
                        binding.currentPhoneNumber.setText(newPhoneNumber);
                    },
                    e -> {
                        Toast.makeText(this, "Failed to update phone number", Toast.LENGTH_SHORT).show();
                        binding.currentPhoneNumber.setText(originalPhoneNumber);
                        Log.e(TAG, "Failed to update phone number", e);
                    }
            );
        }
    }

    private String formatPhoneNumber(String phoneNumber) {
        // Remove any non-digit characters
        String digitsOnly = phoneNumber.replaceAll("[^0-9]", "");

        // Ensure number starts with country code
        if (!digitsOnly.startsWith("+")) {
            // Add your default country code here (e.g., +1 for US)
            digitsOnly = "+1" + digitsOnly;
        }

        return digitsOnly;
    }

    private void resetPhoneEditState() {
        binding.currentPhoneNumber.setEnabled(false);
        binding.currentPhoneNumber.setFocusable(false);
        binding.currentPhoneNumber.setFocusableInTouchMode(false);
        hideKeyboard();
        isPhoneEditing = false;
    }

    private void showProfilePictureOptions() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.activity_pfp_options, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        View viewPhotoOption = bottomSheetView.findViewById(R.id.viewPhotoOption);
        View changePhotoOption = bottomSheetView.findViewById(R.id.changePhotoOption);
        View removePhotoOption = bottomSheetView.findViewById(R.id.removePhotoOption);

        viewPhotoOption.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            showFullscreenImage();
        });

        changePhotoOption.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            openImagePicker();
        });

        if (currentUser != null && currentUser.getProfilePictureUrl() != null) {
            removePhotoOption.setVisibility(View.VISIBLE);
            removePhotoOption.setOnClickListener(v -> {
                bottomSheetDialog.dismiss();
                showRemovePhotoConfirmation();
            });
        } else {
            removePhotoOption.setVisibility(View.GONE);
        }

        bottomSheetDialog.show();
    }

    private void showFullscreenImage() {
        if (currentUser == null || currentUser.getProfilePictureUrl() == null) {
            Toast.makeText(this, "No profile picture available", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_fullscreen_image, null);

        // find the ImageView in the dialog layout
        android.widget.ImageView fullscreenImageView = dialogView.findViewById(R.id.fullscreenImageView);

        // decode and set the image
        Bitmap profileBitmap = ImageHandler.decodeImage(currentUser.getProfilePictureUrl());
        if (profileBitmap != null) {
            fullscreenImageView.setImageBitmap(profileBitmap);
        } else {
            fullscreenImageView.setImageResource(R.drawable.ic_profile_placeholder);
        }

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
            // make the dialog full screen
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }

        // dismiss dialog when clicking anywhere on the image
        dialogView.setOnClickListener(v -> dialog.dismiss());

        // set up close button
        View closeButton = dialogView.findViewById(R.id.closeButton);
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        imagePickerLauncher.launch(intent);
    }

    private void updateProfilePicture(Uri imageUri) {
        try {
            String encodedImage = ImageHandler.encodeImage(this, imageUri);
            if (ImageHandler.isImageSizeValid(encodedImage)) {
                String userId = authController.getCurrentUserId();
                if (userId != null && currentUser != null) {
                    userController.updateUserProfile(
                            userId,
                            currentUser.getName(),
                            encodedImage,
                            aVoid -> {
                                Toast.makeText(this, "Profile picture updated", Toast.LENGTH_SHORT).show();
                                loadUserData();
                            },
                            e -> Toast.makeText(this, "Failed to update profile picture", Toast.LENGTH_SHORT).show()
                    );
                }
            } else {
                Toast.makeText(this, "Selected image is too large", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
        }
    }

    private void showRemovePhotoConfirmation() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Remove Profile Picture")
                .setMessage("Are you sure you want to remove your profile picture?")
                .setPositiveButton("REMOVE", (dialog1, which) -> removeProfilePicture())
                .setNegativeButton("CANCEL", null)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        }

        dialog.show();
    }

    private void removeProfilePicture() {
        String userId = authController.getCurrentUserId();
        if (userId != null && currentUser != null) {
            userController.updateUserProfile(
                    userId,
                    currentUser.getName(),
                    null,
                    aVoid -> {
                        Toast.makeText(this, "Profile picture removed", Toast.LENGTH_SHORT).show();
                        binding.profileIcon.setImageResource(R.drawable.ic_profile_placeholder);
                        loadUserData();
                    },
                    e -> Toast.makeText(this, "Failed to remove profile picture", Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void setupDeleteButton() {
        binding.deleteAccount.setOnClickListener(v -> showDeleteAccountConfirmation());
    }

    private void showDeleteAccountConfirmation() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("DELETE", (dialog1, which) -> deleteAccount())
                .setNegativeButton("CANCEL", null)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        }

        dialog.show();
    }

    private void deleteAccount() {
        String userId = authController.getCurrentUserId();
        if (userId != null) {
            userController.deleteUser(userId,
                    aVoid -> {
                        authController.signOut();
                        startActivity(new Intent(this, MainActivity.class));
                        finishAffinity();
                    },
                    e -> Toast.makeText(this,
                            "Failed to delete account", Toast.LENGTH_SHORT).show());
        }
    }

    private void loadUserData() {
        binding.profileIcon.setAlpha(0.5f);
        String userId = authController.getCurrentUserId();
        if (userId != null) {
            userController.getUserById(userId,
                    user -> {
                        if (user != null) {
                            updateUI(user);
                            // Get locally stored display number
                            String displayNumber = userController.getDisplayPhoneNumber(this);
                            binding.currentPhoneNumber.setText(displayNumber != null ?
                                    displayNumber : "No phone number available");
                        }
                        binding.profileIcon.setAlpha(1.0f);
                    },
                    e -> {
                        Log.e(TAG, "Error loading user data", e);
                        Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show();
                        binding.profileIcon.setAlpha(1.0f);
                    }
            );
        }

        // Retrieve phone number from SharedPreferences
        PreferenceManager preferenceManager = new PreferenceManager(this);
        String displayPhoneNumber = preferenceManager.getString(Constants.KEY_PHONE_NUMBER);

        // Update the phone number field in the UI
        if (displayPhoneNumber != null) {
            binding.currentPhoneNumber.setText(displayPhoneNumber);
        } else {
            binding.currentPhoneNumber.setText("No phone number available");
        }
    }

    private void updateUI(User user) {
        try {
            this.currentUser = user;

            // sets name if available
            if (user.getName() != null) {
                binding.nameInput.setText(user.getName());
                Log.d(TAG, "Setting name: " + user.getName());
            }
            binding.nameInput.setEnabled(false);
            binding.nameInput.setFocusable(false);
            binding.nameInput.setClickable(true);

            // sets phone number if available
            Log.d(TAG, "Phone number from user object: " + user.getPhoneNumber());
            if (user.getPhoneNumber() != null) {
                binding.currentPhoneNumber.setText(user.getPhoneNumber());
            } else {
                // if not in Firestore, try getting it from Auth
                String authPhoneNumber = authController.getCurrentUser();
                Log.d(TAG, "Phone number from Auth: " + authPhoneNumber);
                if (authPhoneNumber != null) {
                    binding.currentPhoneNumber.setText(authPhoneNumber);
                    // save the phone number to Firestore
                    Map<String, Object> updates = new HashMap<>();
                    updates.put(Constants.KEY_PHONE_NUMBER, authPhoneNumber);
                    userController.updateFields(user.getUserId(), updates,
                            aVoid -> Log.d(TAG, "Phone number synced to Firestore"),
                            e -> Log.e(TAG, "Failed to sync phone number", e)
                    );
                }
            }
            binding.currentPhoneNumber.setEnabled(false);
            binding.currentPhoneNumber.setFocusable(false);
            binding.currentPhoneNumber.setClickable(true);

            // sets status if available
            Log.d(TAG, "Status from user object: " + user.getStatus());
            if (user.getStatus() != null) {
                binding.statusDropdown.setText(user.getStatus(), false);
            } else {
                binding.statusDropdown.setText(UserStatus.getDefaultStatuses().get(0), false);
            }

            // sets profile picture if available
            if (user.getProfilePictureUrl() != null) {
                Bitmap profileBitmap = ImageHandler.decodeImage(user.getProfilePictureUrl());
                if (profileBitmap != null) {
                    binding.profileIcon.setImageBitmap(profileBitmap);
                    Log.d(TAG, "Set profile picture");
                } else {
                    binding.profileIcon.setImageResource(R.drawable.ic_profile_placeholder);
                    Log.d(TAG, "Failed to decode profile picture, using placeholder");
                }
            } else {
                binding.profileIcon.setImageResource(R.drawable.ic_profile_placeholder);
                Log.d(TAG, "No profile picture URL, using placeholder");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error updating UI", e);
            Toast.makeText(this, "Error updating display", Toast.LENGTH_SHORT).show();
        }
    }

    private void showKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }


    private void saveNewStatus(String newStatus) {
        String userId = authController.getCurrentUserId();
        if (userId != null && currentUser != null) {
            // save to user status model
            UserStatus.saveStatus(newStatus);

            // update adapter
            statusAdapter.clear();
            statusAdapter.addAll(UserStatus.getAllStatuses());
            statusAdapter.notifyDataSetChanged();

            // save to user profile
            Map<String, Object> updates = new HashMap<>();
            updates.put(Constants.KEY_STATUS, newStatus);

            userController.updateFields(userId, updates,
                    aVoid -> {
                        currentUser.setStatus(newStatus); // Update local user object
                        Toast.makeText(this, "Status updated", Toast.LENGTH_SHORT).show();
                        hideKeyboard();
                    },
                    e -> Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(binding.nameInput.getWindowToken(), 0);
        }
    }

    private void setupSignOutButton() {
        Button signOutButton = binding.signOutButton;
        signOutButton.setOnClickListener(v -> {
            authController.signOut();
            showLogoutToast();
            startActivity(new Intent(SettingsActivity.this, MainActivity.class));
            finish();
        });
    }

    private void showLogoutToast() {
        Toast.makeText(SettingsActivity.this, "Successfully logged out", Toast.LENGTH_SHORT).show();
    }
}