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

import com.example.talkoloco.R;
import com.example.talkoloco.controllers.AuthController;
import com.example.talkoloco.controllers.UserController;
import com.example.talkoloco.databinding.ActivityProfileCreationBinding;
import com.example.talkoloco.models.User;
import com.example.talkoloco.utils.ImageHandler;
import com.example.talkoloco.utils.KeyManager;
import com.google.firebase.firestore.FirebaseFirestore;


import java.util.HashMap;

/**
 * Activity responsible for handling new user profile creation.
 * Manages user input for profile details including name and profile picture,
 * generates encryption keys, and saves the profile to Firebase.
 */
public class ProfileCreationActivity extends AppCompatActivity {
    private ActivityProfileCreationBinding binding;
    private AuthController authController;
    private UserController userController;
    private Uri selectedImageUri;

    // Launcher for handling image selection from gallery
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    // Update profile icon with selected image
                    selectedImageUri = result.getData().getData();
                    binding.profileIcon.setImageURI(selectedImageUri);
                }
            }
    );

    /**
     * Initializes the activity, sets up view binding, and configures initial UI state.
     * Also performs a test write to Firebase to verify connection.
     *
     * @param savedInstanceState If non-null, this activity is being re-initialized after previously being shut down
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileCreationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize controllers for auth and user management
        authController = AuthController.getInstance();
        userController = UserController.getInstance();

        initializeViews();
        setupNameInput();

        // Test Firebase connection with a write operation (used for testing)
        /* FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("test").add(new HashMap<String, Object>())
                .addOnSuccessListener(ref -> Log.d("Firebase", "Write successful"))
                .addOnFailureListener(e -> Log.e("Firebase", "Write failed", e));

         */
    }

    /**
     * Sets up initial view states and click listeners for UI components.
     * The done button is initially disabled until valid input is provided.
     */
    private void initializeViews() {
        // Disable done button until valid name is entered
        binding.doneButton.setEnabled(false);
        binding.doneButton.setOnClickListener(v -> onDoneClick());
        binding.profileIcon.setOnClickListener(v -> onProfileIconClick());
    }

    /**
     * Configures the name input field with a TextWatcher to enable/disable
     * the done button based on input validity.
     */
    private void setupNameInput() {
        binding.nameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Enable done button only if name field is not empty
                binding.doneButton.setEnabled(!s.toString().trim().isEmpty());
            }
        });
    }

    /**
     * Handles the profile creation completion process when the done button is clicked.
     * Creates a new user profile with the entered name, selected/default profile picture,
     * and generated encryption keys.
     */
    private void onDoneClick() {
        String name = binding.nameInput.getText().toString().trim();
        String userId = authController.getCurrentUserId();
        String phoneNumber = authController.getCurrentUser();

        if (userId != null && !name.isEmpty()) {
            // Create new user object with basic info
            User newUser = new User();
            newUser.setUserId(userId);
            newUser.setName(name);
            newUser.setPhoneNumber(phoneNumber);

            // Generate and store encryption keys for secure communication
            KeyManager keyManager = new KeyManager(this);
            String publicKey = keyManager.generateUserKeys();  // Generates and saves private key internally
            Log.d("KeyDebug", "Generated public key: " + publicKey);
            newUser.setPublicKey(publicKey);

            // Process and set profile picture
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
                    return; // Exit if image processing failed
                }
            } else {
                try {
                    // Use default profile picture if none selected
                    Uri defaultUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.default_pfp);
                    String encodedImage = ImageHandler.encodeImage(this, defaultUri);
                    if (ImageHandler.isImageSizeValid(encodedImage)) {
                        newUser.setProfilePictureUrl(encodedImage);
                    } else {
                        Toast.makeText(this, "Selected image is too large", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
                    return; // Exit if default image processing failed
                }
            }

            updateUserProfile(newUser);
            navigateToHome();
        }
    }

    /**
     * Saves the user profile to Firebase and handles success/failure scenarios.
     *
     * @param user The complete user profile to be saved
     */
    private void updateUserProfile(User user) {
        userController.saveUser(user, this, // Pass 'this' as the context
                aVoid -> {
                    // Profile update successful
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    navigateToHome();
                },
                e -> {
                    // Handle profile update failure
                    String errorMessage = (e != null && e.getMessage() != null)
                            ? e.getMessage()
                            : "An unknown error occurred";
                    Toast.makeText(this, "Error updating profile: " + errorMessage,
                            Toast.LENGTH_SHORT).show();
                }
        );
    }

    /**
     * Launches the image picker when the profile icon is clicked.
     * Sets up proper permissions for reading the selected image.
     */
    private void onProfileIconClick() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        imagePickerLauncher.launch(intent);
    }

    /**
     * Navigates to the HomeActivity and clears the activity stack.
     */
    private void navigateToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        // Clear activity stack to prevent going back to profile creation
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Cleanup when the activity is destroyed.
     * Ensures proper disposal of view binding.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null; // Prevent memory leaks by clearing view binding
    }
}