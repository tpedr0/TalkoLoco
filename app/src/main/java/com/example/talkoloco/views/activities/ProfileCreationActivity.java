package com.example.talkoloco.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.talkoloco.controllers.AuthController;
import com.example.talkoloco.controllers.UserController;
import com.example.talkoloco.databinding.ActivityProfileCreationBinding;
import com.example.talkoloco.models.User;
import com.example.talkoloco.views.activities.HomeActivity;

public class ProfileCreationActivity extends AppCompatActivity {
    private ActivityProfileCreationBinding binding;
    private AuthController authController;
    private UserController userController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileCreationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authController = AuthController.getInstance();
        userController = UserController.getInstance();

        initializeViews();
        setupNameInput();
    }

    private void initializeViews() {
        // now uses binding instead of findViewById
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
            // creates new user directly
            User newUser = new User();
            newUser.setUserId(userId);
            newUser.setName(name);

            updateUserProfile(newUser);
        }

        Intent intent = new Intent(ProfileCreationActivity.this, HomeActivity.class);
        // clears the back stack so user can't go back to auth flow
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

    }

    private void updateUserProfile(User user) {
        userController.saveUser(user,
                aVoid -> {
                    // Change this part
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    // Navigate to Home instead of just finishing
                },
                e -> Toast.makeText(this,
                        "Error updating profile", Toast.LENGTH_SHORT).show()
        );
    }

    private void onProfileIconClick() {
        Toast.makeText(this, "Profile picture selection coming soon", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}