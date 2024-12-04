package com.example.talkoloco.views.activities;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.talkoloco.R;
import com.example.talkoloco.controllers.AuthController;
import com.example.talkoloco.controllers.NavigationController;
import com.example.talkoloco.controllers.UserController;
import com.example.talkoloco.adapters.UsersAdapter;
import com.example.talkoloco.databinding.ActivityHomeBinding;
import com.example.talkoloco.models.User;
import com.example.talkoloco.listeners.UserListener;
import com.example.talkoloco.utils.Constants;
import com.example.talkoloco.utils.Hash;
import com.example.talkoloco.utils.PreferenceManager;
import com.example.talkoloco.utils.PhoneNumberFormatter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Main home screen of the application that displays user contacts and provides
 * chat initialization functionality. Handles user authentication state,
 * displays existing chats, and provides interface for starting new conversations.
 */
public class HomeActivity extends AppCompatActivity implements UserListener {

    private ActivityHomeBinding binding;
    private NavigationController navigationController;
    private AuthController authController;
    private UserController userController;
    private PreferenceManager preferenceManager;
    private static final String TAG = "HomeActivity";

    /**
     * Initializes the activity, sets up view binding, and configures the UI components.
     * Handles navigation setup and user authentication checks.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        preferenceManager = new PreferenceManager(getApplicationContext());
        setContentView(binding.getRoot());


        // Initialize controllers for navigation, auth, and user management
        navigationController = new NavigationController(this);
        authController = AuthController.getInstance();
        userController = UserController.getInstance();

        // Setup bottom navigation and chat initialization buttons
        navigationController.setupNavigation(binding.bottomNavigationView);
        binding.addChatIcon.setOnClickListener(v -> showNewChatDialog());
        binding.startMessaging.setOnClickListener(v -> showNewChatDialog());

        // Load existing users
        getUsers();
    }

    /**
     * Displays a dialog for initiating a new chat by entering a phone number.
     * Handles phone number formatting and validation in real-time.
     */
    private void showNewChatDialog() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Material_Light_NoActionBar);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_new_chat, null);
        dialog.setContentView(dialogView);

        EditText phoneInput = dialogView.findViewById(R.id.phoneNumberInput);
        Button startChatButton = dialogView.findViewById(R.id.startChatButton);
        TextView cancelButton = dialogView.findViewById(R.id.cancelButton);

        // Initially disable chat button until valid number is entered
        startChatButton.setEnabled(false);

        // Configure phone number formatting and validation
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

                // Extract only digits from input
                String input = s.toString();
                String digits = input.replaceAll("[^\\d]", "");

                // Ensure country code presence
                if (!digits.startsWith("1")) {
                    digits = "1" + digits;
                }

                // Limit to valid phone number length
                if (digits.length() > 11) {
                    digits = digits.substring(0, 11);
                }

                // Format the phone number with proper separators
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

                // Update input field if format has changed
                String formattedText = formatted.toString();
                if (!formattedText.equals(lastFormatted)) {
                    lastFormatted = formattedText;
                    s.replace(0, s.length(), formattedText);
                }

                // Enable button only for complete phone numbers
                boolean isComplete = digits.length() == 11;
                startChatButton.setEnabled(isComplete);

                // Show error for incomplete numbers
                if (digits.length() > 1 && !isComplete) {
                    phoneInput.setError("Enter a complete phone number");
                } else {
                    phoneInput.setError(null);
                }

                isFormatting = false;
            }
        });

        // Setup dialog button actions
        startChatButton.setOnClickListener(v -> {
            String phoneNumber = phoneInput.getText().toString();
            String digits = phoneNumber.replaceAll("[^\\d]", "");
            if (digits.length() == 11) {
                startNewChat(phoneNumber);
                dialog.dismiss();
            }
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        // Configure and show dialog
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setWindowAnimations(R.style.DialogAnimation);
        }
        dialog.show();
        startChatButton.setOnClickListener(v -> {
            String phoneNumber = phoneInput.getText().toString();
            String digits = phoneNumber.replaceAll("[^\\d]", "");
            if (digits.length() == 11) {
                startNewChat(phoneNumber);
                dialog.dismiss();
            }
        });
    }

    /**
     * Verifies if a user exists with the given phone number and initiates chat if found.
     *
     * @param phoneNumber The formatted phone number to check
     */
    private void checkIfUserExistsByPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        // Query user by phone number using UserController
        userController.getUserByPhoneNumber(phoneNumber,
                new OnSuccessListener<User>() {
                    @Override
                    public void onSuccess(User user) {
                        // Create chat user object with necessary information
                        User chatUser = new User();
                        chatUser.name = user.getName();
                        chatUser.id = user.getUserId();
                        chatUser.profilePictureUrl = user.getProfilePictureUrl();
                        chatUser.setPublicKey(user.getPublicKey()); // Required for encrypted communication

                        // Navigate to chat
                        startNewChatWithUser(chatUser);
                    }
                },
                new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(HomeActivity.this,
                                "No user found with this phone number",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /**
     * Initiates the chat creation process by cleaning the phone number
     * and checking user existence.
     *
     * @param phoneNumber The raw phone number input
     */
    private void startNewChat(String phoneNumber) {
        // Strip formatting from the phone number
        String cleanPhoneNumber = PhoneNumberFormatter.stripFormatting(phoneNumber);

        // Check if the user exists by phone number
        checkIfUserExistsByPhoneNumber(cleanPhoneNumber);
    }

    /**
     * Launches the ChatActivity with the selected user's information.
     *
     * @param user The User object containing chat participant details
     */
    private void startNewChatWithUser(User user) {
        Log.d(TAG, "Starting chat with user. Public key: " + user.getPublicKey());
        if (user == null) {
            Toast.makeText(this, "Invalid user data", Toast.LENGTH_SHORT).show();
            return;
        }
        // Navigate to ChatActivity with the user details
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user); // Pass user object to ChatActivity
        startActivity(intent);
    }

    /**
     * Verifies user authentication state when activity resumes.
     * Redirects to MainActivity if user is not signed in.
     */
    @Override
    protected void onResume() {
        super.onResume();
        // check if user is signed in
        if (!authController.isUserSignedIn()) {
            // navigate to MainActivity if not signed in
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    /**
     * Cleans up resources when activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null; // Prevent memory leaks
    }

    /**
     * Retrieves and displays the list of available users from Firebase.
     * Filters out the current user from the list.
     */
    private void getUsers(){
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS).get()
                .addOnCompleteListener(task -> {
                    String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
                    if(task.isSuccessful() && task.getResult() !=null){
                        List<User> users = new ArrayList<>();
                        // Process each user document
                        for(QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()){
                            // Skip current user
                            if(currentUserId.equals(queryDocumentSnapshot.getId())){
                                continue;
                            }
                            // Create user object from document data
                            User user = new User();
                            user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                            user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                            user.profilePictureUrl = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                            user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                            user.setPublicKey(queryDocumentSnapshot.getString(Constants.KEY_PUBLIC_KEY));
                            user.id = queryDocumentSnapshot.getId();
                            user.setStatus(queryDocumentSnapshot.getString(Constants.KEY_STATUS));
                            users.add(user);
                        }
                        // Update UI based on results
                        if(!users.isEmpty()){
                            UsersAdapter usersAdapter = new UsersAdapter(users,this,binding.getRoot().getContext());
                            binding.userRecycleView.setAdapter(usersAdapter);
                            binding.userRecycleView.setVisibility(View.VISIBLE);
                        } else {
                            showErrorMessage();
                        }
                    }else {
                        showErrorMessage();
                    }
                });
    }

    /**
     * Displays an error message when no users are found.
     */
    private void showErrorMessage(){
        Toast.makeText(this,
                "No users found :(",
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Callback implementation for user selection from the list.
     * Launches ChatActivity with the selected user's information.
     *
     * @param user The selected User object
     */
    @Override
    public void onUserClicked(User user) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(Constants.KEY_USER,user);
        startActivity(intent);
    }
}