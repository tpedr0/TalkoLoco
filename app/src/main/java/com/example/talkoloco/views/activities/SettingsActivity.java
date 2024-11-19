package com.example.talkoloco.views.activities;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import com.example.talkoloco.utils.ImageHandler;
import com.google.android.material.bottomsheet.BottomSheetDialog;

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
    private ArrayAdapter<String> statusAdapter;
    private static final String TAG = "SettingsActivity";

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

        // Initialize controllers
        navigationController = new NavigationController(this);
        authController = AuthController.getInstance();
        userController = UserController.getInstance();

        // Setup navigation
        navigationController.setupNavigation(binding.bottomNavigationView);

        // Setup features
        setupDeleteButton();
        setupNameEditing();
        setupStatusDropdown();

        // Setup profile picture click
        binding.profileIcon.setOnClickListener(v -> showProfilePictureOptions());

        // Load user data
        loadUserData();
    }

    private void setupStatusDropdown() {
        // Initialize adapter with all statuses
        statusAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                UserStatus.getAllStatuses()
        );
        binding.statusDropdown.setAdapter(statusAdapter);

        // Handle custom input
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

        // Handle item selection
        binding.statusDropdown.setOnItemClickListener((parent, view, position, id) -> {
            String selectedStatus = parent.getItemAtPosition(position).toString();
            saveNewStatus(selectedStatus);
        });
    }

    private void saveNewStatus(String newStatus) {
        String userId = authController.getCurrentUserId();
        if (userId != null && currentUser != null) {
            // Save to user status model
            UserStatus.saveStatus(newStatus);

            // Update adapter
            statusAdapter.clear();
            statusAdapter.addAll(UserStatus.getAllStatuses());
            statusAdapter.notifyDataSetChanged();

            // Save to user profile
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", newStatus);
            userController.updateFields(userId, updates,
                    aVoid -> {
                        Toast.makeText(this, "Status updated", Toast.LENGTH_SHORT).show();
                        hideKeyboard();
                    },
                    e -> Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void setupNameEditing() {
        // Make sure EditText is initially disabled but clickable
        binding.nameInput.setEnabled(false);
        binding.nameInput.setClickable(true);
        binding.nameInput.setFocusable(false);

        // Setup click listener
        binding.nameInput.setOnClickListener(v -> {
            binding.nameInput.setEnabled(true);
            binding.nameInput.setFocusableInTouchMode(true);
            binding.nameInput.setFocusable(true);
            binding.nameInput.requestFocus();
            binding.nameInput.setSelection(binding.nameInput.length());
            showKeyboard(binding.nameInput);
            isNameEditing = true;
        });

        binding.nameInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveNameChanges();
                return true;
            }
            return false;
        });

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

    private void showFullscreenImage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_fullscreen_image, null);

        if (currentUser != null && currentUser.getProfilePictureUrl() != null) {
            Bitmap profileBitmap = ImageHandler.decodeImage(currentUser.getProfilePictureUrl());
            if (profileBitmap != null) {
                binding.profileIcon.setImageBitmap(profileBitmap);
            }
        } else {
            binding.profileIcon.setImageResource(R.drawable.ic_profile_placeholder);
        }

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        }

        dialogView.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
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
                        }
                        binding.profileIcon.setAlpha(1.0f);
                    },
                    e -> {
                        Log.e(TAG, "Error loading user data", e);
                        Toast.makeText(this,
                                "Error loading user data", Toast.LENGTH_SHORT).show();
                        binding.profileIcon.setAlpha(1.0f);
                    }
            );
        }
    }

    private void updateUI(User user) {
        try {
            this.currentUser = user;

            // Set name if available
            if (user.getName() != null) {
                binding.nameInput.setText(user.getName());
            }
            binding.nameInput.setEnabled(false);
            binding.nameInput.setFocusable(false);
            binding.nameInput.setClickable(true);

            // Set phone number if available
            if (user.getPhoneNumber() != null) {
                binding.currentPhoneNumber.setText(user.getPhoneNumber());
            }

            // Set status if available
            if (user.getStatus() != null) {
                binding.statusDropdown.setText(user.getStatus(), false);
            } else {
                binding.statusDropdown.setText(UserStatus.getDefaultStatuses().get(0), false);
            }

            // Set profile picture if available
            if (user.getProfilePictureUrl() != null) {
                Bitmap profileBitmap = ImageHandler.decodeImage(user.getProfilePictureUrl());
                if (profileBitmap != null) {
                    binding.profileIcon.setImageBitmap(profileBitmap);
                }
            } else {
                binding.profileIcon.setImageResource(R.drawable.ic_profile_placeholder);
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

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(binding.nameInput.getWindowToken(), 0);
        }
    }
}

