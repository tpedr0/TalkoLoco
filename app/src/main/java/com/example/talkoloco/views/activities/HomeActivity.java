package com.example.talkoloco.views.activities;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.talkoloco.R;
import com.example.talkoloco.controllers.AuthController;
import com.example.talkoloco.controllers.NavigationController;
import com.example.talkoloco.controllers.UserController;
import com.example.talkoloco.databinding.ActivityHomeBinding;
import com.example.talkoloco.utils.PhoneNumberFormatter;

public class HomeActivity extends AppCompatActivity {
    private ActivityHomeBinding binding;
    private NavigationController navigationController;
    private AuthController authController;
    private UserController userController;
    private static final String TAG = "HomeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize controllers
        navigationController = new NavigationController(this);
        authController = AuthController.getInstance();
        userController = UserController.getInstance();

        // Setup navigation
        navigationController.setupNavigation(binding.bottomNavigationView);

        // Setup click listeners for new chat
        binding.addChatIcon.setOnClickListener(v -> showNewChatDialog());
        binding.startMessaging.setOnClickListener(v -> showNewChatDialog());
    }

    private void showNewChatDialog() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Material_Light_NoActionBar);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_new_chat, null);
        dialog.setContentView(dialogView);

        EditText phoneInput = dialogView.findViewById(R.id.phoneNumberInput);
        Button startChatButton = dialogView.findViewById(R.id.startChatButton);
        TextView cancelButton = dialogView.findViewById(R.id.cancelButton);

        // Disable the start chat button initially
        startChatButton.setEnabled(false);

        // Setup phone number formatting
        phoneInput.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting;
            private String lastFormatted = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;
                isFormatting = true;

                // get just the digits
                String input = s.toString();
                String digits = input.replaceAll("[^\\d]", "");

                // Ensure we start with country code
                if (!digits.startsWith("1")) {
                    digits = "1" + digits;
                }

                // Limit to exactly 11 digits (including country code)
                if (digits.length() > 11) {
                    digits = digits.substring(0, 11);
                }

                // Format the number
                StringBuilder formatted = new StringBuilder("+1 ");
                if (digits.length() > 1) {
                    String remaining = digits.substring(1); // Remove country code
                    if (remaining.length() > 0) {
                        formatted.append("(").append(remaining.substring(0, Math.min(3, remaining.length())));

                        if (remaining.length() > 3) {
                            formatted.append(") ").append(remaining.substring(3, Math.min(6, remaining.length())));

                            if (remaining.length() > 6) {
                                formatted.append("-").append(remaining.substring(6, Math.min(10, remaining.length())));
                            }
                        }
                    }
                }

                String formattedText = formatted.toString();
                if (!formattedText.equals(lastFormatted)) {
                    lastFormatted = formattedText;
                    s.replace(0, s.length(), formattedText);
                }

                // Enable button only if we have a complete number (exactly 11 digits)
                boolean isComplete = digits.length() == 11;
                startChatButton.setEnabled(isComplete);

                // Show error only if we have an incomplete number (more than just +1)
                if (digits.length() > 1 && !isComplete) {
                    phoneInput.setError("Enter a complete phone number");
                } else {
                    phoneInput.setError(null);
                }

                isFormatting = false;
            }
        });

        // Setup button clicks
        startChatButton.setOnClickListener(v -> {
            String phoneNumber = phoneInput.getText().toString();
            String digits = phoneNumber.replaceAll("[^\\d]", "");
            if (digits.length() == 11) {
                startNewChat(phoneNumber);
                dialog.dismiss();
            }
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        // Show dialog
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setWindowAnimations(R.style.DialogAnimation);
        }
        dialog.show();
    }

    private void startNewChat(String phoneNumber) {
        // Strip formatting before checking user existence
        String cleanPhoneNumber = PhoneNumberFormatter.stripFormatting(phoneNumber);

        userController.checkIfUserExists(cleanPhoneNumber,
                exists -> {
                    if (exists) {
                        // TODO: Start chat with user
                        Toast.makeText(HomeActivity.this,
                                "Starting chat with: " + phoneNumber, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(HomeActivity.this,
                                "No user found with this number", Toast.LENGTH_SHORT).show();
                    }
                },
                e -> Toast.makeText(HomeActivity.this,
                        "Error checking user: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if user is signed in
        if (!authController.isUserSignedIn()) {
            // Navigate to MainActivity if not signed in
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}