package com.example.talkoloco.views.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.talkoloco.controllers.AuthController;
import com.example.talkoloco.controllers.UserController;
import com.example.talkoloco.databinding.ActivityProfileCreationBinding;
import com.example.talkoloco.models.User;
import com.example.talkoloco.utils.ImageHandler;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class ProfileCreationActivity extends AppCompatActivity {
    private ActivityProfileCreationBinding binding;
    private AuthController authController;
    private UserController userController;
    private Uri selectedImageUri;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    binding.profileIcon.setImageURI(selectedImageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileCreationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authController = AuthController.getInstance();
        userController = UserController.getInstance();

        initializeViews();
        setupNameInput();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("test").add(new HashMap<String, Object>())
                .addOnSuccessListener(ref -> Log.d("Firebase", "Write successful"))
                .addOnFailureListener(e -> Log.e("Firebase", "Write failed", e));
    }

    private void initializeViews() {
        binding.doneButton.setEnabled(false);
        binding.doneButton.setOnClickListener(v -> onDoneClick());
        binding.profileIcon.setOnClickListener(v -> onProfileIconClick());
    }


    private void setupNameInput() {
        binding.nameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                binding.doneButton.setEnabled(!s.toString().trim().isEmpty());
            }
        });
    }

    private void onDoneClick() {
        String name = binding.nameInput.getText().toString().trim();
        String userId = authController.getCurrentUserId();

        if (userId != null && !name.isEmpty()) {
            User newUser = new User();
            newUser.setUserId(userId);
            newUser.setName(name);

            if (selectedImageUri != null) {
                try {
                    String encodedImage = ImageHandler.encodeImage(this, selectedImageUri);
                    if (ImageHandler.isImageSizeValid(encodedImage)) {
                        newUser.setProfilePictureUrl(encodedImage);
                    } else {
                        Toast.makeText(this, "Selected image is too large", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            updateUserProfile(newUser);
            navigateToHome();
        }
    }

    private void updateUserProfile(User user) {
        userController.saveUser(user,
                aVoid -> {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    navigateToHome();
                },
                e -> Toast.makeText(this, "Error updating profile: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show()
        );
    }

    private void onProfileIconClick() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        imagePickerLauncher.launch(intent);
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}