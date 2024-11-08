package com.example.talkoloco.views.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.talkoloco.R;
import com.example.talkoloco.controllers.AuthController;
import com.example.talkoloco.controllers.UserController;
import com.example.talkoloco.models.User;

/**
 * the ProfileCreation class handles the user profile creation process.
 */
public class ProfileCreation extends AppCompatActivity {
    private EditText nameInput;
    private Button doneButton;
    private AuthController authController;
    private UserController userController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitivity_profile_creation);

        authController = AuthController.getInstance();
        userController = UserController.getInstance();

        initializeViews();
        setupNameInput();
    }

    /**
     * initializes the views and sets up the name input field.
     */
    private void initializeViews() {
        nameInput = findViewById(R.id.nameInput);
        doneButton = findViewById(R.id.doneButton);
        ImageView profileIcon = findViewById(R.id.profileIcon);

        // initially disable done button
        doneButton.setEnabled(false);

        // set up click listeners
        doneButton.setOnClickListener(v -> onDoneClick());
        profileIcon.setOnClickListener(v -> onProfileIconClick());
    }


    /**
     * sets up the name input field, enabling the "Done" button when the name is not empty.
     */
    private void setupNameInput() {
        nameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Enable done button only if name is not empty
                doneButton.setEnabled(!s.toString().trim().isEmpty());
            }
        });
    }

    /**
     * handles the click event of the "Done" button, which updates the user profile and navigates to the main app screen.
     */
    private void onDoneClick() {
        String name = nameInput.getText().toString().trim();
        String userId = authController.getCurrentUserId();

        if (userId != null && !name.isEmpty()) {
            userController.getUserById(userId,
                    user -> {
                        if (user != null) {
                            user.setName(name);
                            updateUserProfile(user);
                        }
                    },
                    e -> Toast.makeText(ProfileCreation.this,
                            "Error fetching user data", Toast.LENGTH_SHORT).show()
            );
        }
    }

    /**
     * updates the user's profile in the Firestore database.
     *
     * @param user the updated user object
     */
    private void updateUserProfile(User user) {
        userController.saveUser(user,
                aVoid -> {
                    // navigate to main app screen or finish
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                },
                e -> Toast.makeText(this,
                        "Error updating profile", Toast.LENGTH_SHORT).show()
        );
    }

    /**
     * handles the click event of the profile icon, which is a placeholder for future profile picture selection functionality.
     */
    private void onProfileIconClick() {
        // Implement profile picture selection
        Toast.makeText(this, "Profile picture selection coming soon", Toast.LENGTH_SHORT).show();
    }
}