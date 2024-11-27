package com.example.talkoloco.views.activities;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.talkoloco.adapters.ChatAdapter;
import com.example.talkoloco.controllers.SignalEncryptionController;
import com.example.talkoloco.controllers.KeyExchangeManager;
import com.example.talkoloco.databinding.ActivityChatBinding;
import com.example.talkoloco.models.ChatMessages;
import com.example.talkoloco.models.User;
import com.example.talkoloco.utils.Constants;
import com.example.talkoloco.utils.ImageHandler;
import com.example.talkoloco.utils.PreferenceManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.signal.libsignal.protocol.state.PreKeyBundle;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private User receiverUser;
    private Uri selectedImageUri;
    private List<ChatMessages> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;

    // Encryption-related fields
    private SignalEncryptionController encryptionController;
    private KeyExchangeManager keyExchangeManager;
    private boolean isEncryptionReady = false;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    sendImage();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(getApplicationContext());
        receiverUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);

        String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
        Log.d(TAG, "Current user ID from preferences: " + currentUserId);

        if (currentUserId == null || currentUserId.isEmpty()) {
            retrieveCurrentUserFromFirestore();
        } else {
            initializeEncryption();
        }

        if (receiverUser == null) {
            Log.e(TAG, "Receiver user is null");
            Toast.makeText(this, "Error: No user data received", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadReceiverDetails();
        setListeners();
        init();
        listenMessages();
    }

    private void initializeEncryption() {
        try {
            String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
            if (currentUserId == null || currentUserId.isEmpty()) {
                Log.e(TAG, "Current user ID is null or empty");
                return;
            }

            Log.d(TAG, "Starting encryption initialization with currentUserId: " + currentUserId +
                    " and receiverId: " + receiverUser.id);
            encryptionController = SignalEncryptionController.getInstance(this);
            keyExchangeManager = new KeyExchangeManager();

            // First check if receiver's bundle already exists
            keyExchangeManager.retrievePreKeyBundle(
                    receiverUser.id,
                    receiverBundle -> {
                        Log.d(TAG, "Found existing receiver bundle, handling it");
                        handleExistingReceiverBundle(currentUserId, receiverBundle);
                    },
                    e -> {
                        Log.d(TAG, "No existing receiver bundle found, starting new setup");
                        handleNewEncryptionSetup(currentUserId);
                    }
            );

        } catch (Exception e) {
            Log.e(TAG, "Error in encryption initialization", e);
            e.printStackTrace();
        }
    }



    private void handleExistingReceiverBundle(String currentUserId, PreKeyBundle receiverBundle) {
        try {
            Log.d(TAG, "Handling existing receiver bundle for currentUserId: " + currentUserId);
            // Generate our bundle
            PreKeyBundle myPreKeyBundle = encryptionController.generatePreKeyBundle();
            Log.d(TAG, "Generated our PreKeyBundle successfully");

            // Store our bundle first
            keyExchangeManager.storePreKeyBundle(
                    currentUserId,
                    myPreKeyBundle,
                    aVoid -> {
                        try {
                            Log.d(TAG, "Successfully stored our PreKeyBundle");
                            // Initialize session with receiver's bundle
                            encryptionController.initializeSession(receiverUser.id, receiverBundle);
                            isEncryptionReady = true;
                            Log.d(TAG, "Encryption session initialized successfully");
                            runOnUiThread(() ->
                                    Toast.makeText(ChatActivity.this,
                                            "Secure chat established",
                                            Toast.LENGTH_SHORT).show()
                            );
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to initialize session", e);
                            e.printStackTrace();
                        }
                    },
                    e -> {
                        Log.e(TAG, "Failed to store our PreKeyBundle", e);
                        e.printStackTrace();
                    }
            );
        } catch (Exception e) {
            Log.e(TAG, "Error in handleExistingReceiverBundle", e);
            e.printStackTrace();
        }
    }

    private void handleNewEncryptionSetup(String currentUserId) {
        try {
            Log.d(TAG, "Starting new encryption setup for currentUserId: " + currentUserId);
            // Generate and store our PreKeyBundle
            PreKeyBundle myPreKeyBundle = encryptionController.generatePreKeyBundle();
            Log.d(TAG, "Generated new PreKeyBundle successfully");

            keyExchangeManager.storePreKeyBundle(
                    currentUserId,
                    myPreKeyBundle,
                    aVoid -> {
                        Log.d(TAG, "Successfully stored our PreKeyBundle. Waiting for receiver...");
                        // Start listening for receiver's bundle
                        startListeningForReceiverBundle();
                    },
                    e -> {
                        Log.e(TAG, "Failed to store our PreKeyBundle", e);
                        e.printStackTrace();
                    }
            );
        } catch (Exception e) {
            Log.e(TAG, "Error in handleNewEncryptionSetup", e);
            e.printStackTrace();
        }
    }

    private boolean verifyEncryptionStatus() {
        try {
            if (!isEncryptionReady) {
                Log.d(TAG, "Encryption not ready yet");
                return false;
            }

            // Test if we can actually encrypt/decrypt
            String testMessage = "test";
            Log.d(TAG, "Testing encryption with message: " + testMessage);

            String encrypted = encryptionController.encryptMessage(testMessage, receiverUser.id);
            Log.d(TAG, "Test message encrypted successfully");

            String decrypted = encryptionController.decryptMessage(encrypted, receiverUser.id);
            Log.d(TAG, "Test message decrypted successfully: " + decrypted);

            if (testMessage.equals(decrypted)) {
                Log.d(TAG, "Encryption test successful");
                return true;
            } else {
                Log.e(TAG, "Encryption test failed - decryption mismatch");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Encryption test failed with exception", e);
            e.printStackTrace();
            return false;
        }
    }

    private void startListeningForReceiverBundle() {
        Log.d(TAG, "Starting to listen for receiver's bundle");
        FirebaseFirestore.getInstance()
                .collection("key_bundles")
                .document(receiverUser.id)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening for receiver bundle", e);
                        return;
                    }

                    if (snapshot != null && snapshot.exists() && !isEncryptionReady) {
                        Log.d(TAG, "Receiver's bundle found in listener");
                        keyExchangeManager.retrievePreKeyBundle(
                                receiverUser.id,
                                receiverBundle -> {
                                    try {
                                        Log.d(TAG, "Retrieved receiver bundle, initializing session");
                                        encryptionController.initializeSession(receiverUser.id, receiverBundle);
                                        isEncryptionReady = true;

                                        // Verify the session immediately after initialization
                                        if (verifyEncryptionStatus()) {
                                            Log.d(TAG, "Session verification successful");
                                            runOnUiThread(() ->
                                                    Toast.makeText(ChatActivity.this,
                                                            "Secure chat established",
                                                            Toast.LENGTH_SHORT).show()
                                            );
                                        } else {
                                            Log.e(TAG, "Session verification failed after initialization");
                                            isEncryptionReady = false;
                                        }
                                    } catch (Exception ex) {
                                        Log.e(TAG, "Failed to initialize session", ex);
                                        isEncryptionReady = false;
                                        ex.printStackTrace();
                                    }
                                },
                                error -> Log.e(TAG, "Failed to retrieve receiver's bundle", error)
                        );
                    }
                });
    }

    // Your existing methods remain the same
    private void retrieveCurrentUserFromFirestore() {
        // Get the current user's phone number
        String phoneNumber = preferenceManager.getString(Constants.KEY_PHONE_NUMBER);

        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            FirebaseFirestore.getInstance()
                    .collection(Constants.KEY_COLLECTION_USERS)
                    .whereEqualTo(Constants.KEY_PHONE_NUMBER, phoneNumber)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            // Get the first document (user)
                            String userId = querySnapshot.getDocuments().get(0).getId();
                            Log.d(TAG, "Retrieved user ID from Firebase: " + userId);

                            // Save it to preferences
                            preferenceManager.putString(Constants.KEY_USER_ID, userId);

                            // Restart the chat initialization
                            init();
                            listenMessages();
                        } else {
                            Log.e(TAG, "No user found with phone number: " + phoneNumber);
                            Toast.makeText(ChatActivity.this,
                                    "Could not find user details. Please try logging in again.",
                                    Toast.LENGTH_LONG).show();
                            redirectToLogin();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error retrieving user details", e);
                        Toast.makeText(ChatActivity.this,
                                "Error retrieving user details: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        redirectToLogin();
                    });
        } else {
            Log.e(TAG, "No phone number found in preferences");
            redirectToLogin();
        }
    }

    private void redirectToLogin() {
        // Clear preferences
        preferenceManager.clear();

        // Redirect to your main activity (phone number input)
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void init() {
        try {
            preferenceManager = new PreferenceManager(getApplicationContext());
            chatMessages = new ArrayList<>();

            // Safely handle the receiver's profile picture
            Bitmap receiverBitmap = null;
            if (receiverUser != null && receiverUser.profilePictureUrl != null && !receiverUser.profilePictureUrl.isEmpty()) {
                receiverBitmap = getBitmapFromEncodedString(receiverUser.profilePictureUrl);
            }

            chatAdapter = new ChatAdapter(
                    chatMessages,
                    receiverBitmap,  // This can now be null
                    preferenceManager.getString(Constants.KEY_USER_ID)
            );

            if (binding != null && binding.chatRecyclerView != null) {
                binding.chatRecyclerView.setAdapter(chatAdapter);
            } else {
                throw new IllegalStateException("Binding or RecyclerView is null");
            }

            database = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing chat: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish(); // Safely close the activity
        }
    }

    private void sendMessages() {
        String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
        Log.d("ChatDebug", "Current User ID from preferences: " + currentUserId);
        Log.d("ChatDebug", "Receiver User ID: " + receiverUser.id);

        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e("ChatDebug", "Current user ID is null or empty!");
            handleMissingUserId();
            return;
        }

        String messageText = binding.messageInput.getText().toString().trim();
        if (messageText.isEmpty()) {
            Toast.makeText(ChatActivity.this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Encrypt message if encryption is ready
            String messageToSend = messageText;
            if (isEncryptionReady) {
                if (!verifyEncryptionStatus()) {
                    Log.e(TAG, "Encryption verification failed - falling back to unencrypted");
                    isEncryptionReady = false;
                } else {
                    messageToSend = encryptionController.encryptMessage(messageText, receiverUser.id);
                    Log.d(TAG, "Message encrypted successfully: " + messageToSend.substring(0, Math.min(messageToSend.length(), 20)) + "...");
                }
            } else {
                Log.d(TAG, "Sending unencrypted message (encryption not ready)");
            }

            HashMap<String, Object> message = new HashMap<>();
            message.put(Constants.KEY_SENDER_ID, currentUserId);
            message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
            message.put(Constants.KEY_MESSAGE, messageToSend);
            message.put(Constants.KEY_TIMESTAMP, new Date());
            message.put("isEncrypted", isEncryptionReady);

            database.collection(Constants.KEY_COLLECTION_CHAT)
                    .add(message)
                    .addOnSuccessListener(documentReference -> {
                        Log.d("ChatDebug", "Message sent successfully with ID: " + documentReference.getId());
                        binding.messageInput.setText(null);
                        if (chatMessages != null && !chatMessages.isEmpty()) {
                            binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ChatDebug", "Failed to send message", e);
                        Toast.makeText(ChatActivity.this,
                                "Error sending message: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });

            // Log encryption status after sending
            Log.d("ChatDebug", "Message sent with encryption status: " + isEncryptionReady);

        } catch (Exception e) {
            Log.e("ChatDebug", "Error sending/encrypting message", e);
            e.printStackTrace(); // Add full stack trace
            Toast.makeText(this, "Error sending message", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleMissingUserId() {
        // Try to get the current user ID from Firebase Auth
        FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String userId = querySnapshot.getDocuments().get(0).getId();
                        Log.d("ChatDebug", "Retrieved user ID from Firebase: " + userId);
                        // Save it to preferences
                        preferenceManager.putString(Constants.KEY_USER_ID, userId);
                        // Try sending the message again now that we have the ID
                        sendMessages();
                    } else {
                        Toast.makeText(ChatActivity.this,
                                "Could not find user details",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ChatActivity.this,
                            "Error retrieving user details: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void sendImage(){
        String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
        Log.d("ChatDebug", "Current User ID from preferences: " + currentUserId);
        Log.d("ChatDebug", "Receiver User ID: " + receiverUser.id);
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e("ChatDebug", "Current user ID is null or empty!");
            // Try to get the current user ID from Firebase Auth
            FirebaseFirestore.getInstance()
                    .collection(Constants.KEY_COLLECTION_USERS)
                    .whereEqualTo(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            String userId = querySnapshot.getDocuments().get(0).getId();
                            Log.d("ChatDebug", "Retrieved user ID from Firebase: " + userId);
                            // Save it to preferences
                            preferenceManager.putString(Constants.KEY_USER_ID, userId);
                            // Try sending the message again now that we have the ID
                            sendImage();
                        } else {
                            Toast.makeText(ChatActivity.this,
                                    "Could not find user details",
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ChatActivity.this,
                                "Error retrieving user details: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
            return;
        } else {
            try {
                String encodedImage = ImageHandler.encodeImage(this, selectedImageUri);
                if (ImageHandler.isImageSizeValid(encodedImage)) {
                    HashMap<String, Object> message = new HashMap<>();

                    message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                    message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);

                    message.put(Constants.KEY_MESSAGE, encodedImage);
                    message.put(Constants.KEY_TIMESTAMP, new Date());

                    database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
                } else {
                    Toast.makeText(this, "Selected image is too large", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
            }

        }
    }

    private void listenMessages() {
        String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
        if (currentUserId == null || receiverUser == null || receiverUser.id == null) {
            return;
        }

        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo("senderId", currentUserId)
                .whereEqualTo("receiverId", receiverUser.id)
                .addSnapshotListener(eventListener);

        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("senderId", receiverUser.id)
                .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }

        if (value != null) {
            int count = chatMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    try {
                        ChatMessages chatMessage = new ChatMessages();
                        chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                        chatMessage.receiverID = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);

                        // Get the message and decrypt if it's encrypted
                        String messageText = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                        Boolean isEncrypted = documentChange.getDocument().getBoolean("isEncrypted");

                        if (isEncrypted != null && isEncrypted && isEncryptionReady) {
                            try {
                                messageText = encryptionController.decryptMessage(messageText, chatMessage.senderId);
                                Log.d("ChatDebug", "Message decrypted successfully");
                            } catch (Exception e) {
                                Log.e("ChatDebug", "Error decrypting message", e);
                                messageText = "Unable to decrypt message";
                            }
                        }

                        chatMessage.message = messageText;
                        chatMessage.dateTime = getReadableDateTime(
                                documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                        chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                        chatMessages.add(chatMessage);
                    } catch (Exception e) {
                        Log.e("ChatDebug", "Error processing message", e);
                    }
                }
            }
            Collections.sort(chatMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
            if (count == 0) {
                chatAdapter.notifyDataSetChanged();
            } else {
                chatAdapter.notifyItemRangeChanged(chatMessages.size(), chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
    };

    // Rest of your existing methods remain the same
    private Bitmap getBitmapFromEncodedString(String encodedImage) {
        if (encodedImage == null || encodedImage.isEmpty()) {
            return null; // Return null if the image string is invalid
        }
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private void loadReceiverDetails() {
        receiverUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        if (receiverUser != null) {
            binding.contact.setText(receiverUser.name);
        } else {
            Toast.makeText(this, "User details not found", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity if no user details are found
        }
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        //chat
        binding.sendMessage.setOnClickListener(v-> sendMessages());

        binding.attachments.setOnClickListener(v -> openImagePicker());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        imagePickerLauncher.launch(intent);
    }

    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a",
                Locale.getDefault()).format(date);
    }
}