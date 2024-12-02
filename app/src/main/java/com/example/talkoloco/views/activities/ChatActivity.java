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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.crypto.SecretKey;
import com.example.talkoloco.utils.KeyManager;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private User receiverUser;
    private Uri selectedImageUri;
    private List<ChatMessages> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private KeyManager keyManager;
    private FirebaseFirestore database;

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

        // Initialize the PreferenceManager first
        preferenceManager = new PreferenceManager(getApplicationContext());

        // Initialize KeyManager right after PreferenceManager
        keyManager = new KeyManager(getApplicationContext());

        // Initialize database
        database = FirebaseFirestore.getInstance();


        // Debug log to check if user ID exists in preferences
        String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
        Log.d(TAG, "Current user ID from preferences: " + currentUserId);

        // If current user ID is missing, try to recover it
        if (currentUserId == null || currentUserId.isEmpty()) {
            retrieveCurrentUserFromFirestore();
        }

        receiverUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        if (receiverUser == null) {
            Log.e(TAG, "Receiver user is null");
            Toast.makeText(this, "Error: No user data received", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Received user: " + receiverUser.name + ", ID: " + receiverUser.id);

        loadReceiverDetails();
        setListeners();
        init();
        listenMessages();
    }

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
            // Ensure keyManager is initialized
            if (keyManager == null) {
                keyManager = new KeyManager(getApplicationContext());
            }

            // Validate encryption keys
            String privateKey = preferenceManager.getString("PRIVATE_KEY");
            String publicKey = preferenceManager.getString(Constants.KEY_PUBLIC_KEY);

            if (privateKey == null || publicKey == null) {
                // Generate new keys if missing
                publicKey = keyManager.generateUserKeys();
                Log.d(TAG, "Generated new encryption keys");
            }

            chatMessages = new ArrayList<>();

            // Safely handle the receiver's profile picture
            Bitmap receiverBitmap = ImageHandler.decodeImage(receiverUser.getProfilePictureUrl());
            if (receiverUser != null && receiverUser.profilePictureUrl != null && !receiverUser.profilePictureUrl.isEmpty()) {
                binding.profilePic.setImageBitmap(receiverBitmap);
            }

            chatAdapter = new ChatAdapter(
                    chatMessages,
                    receiverBitmap,
                    preferenceManager.getString(Constants.KEY_USER_ID)
            );

            if (binding != null && binding.chatRecyclerView != null) {
                binding.chatRecyclerView.setAdapter(chatAdapter);
            } else {
                throw new IllegalStateException("Binding or RecyclerView is null");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error initializing chat: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing chat: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }
    private boolean validateEncryptionSetup() {
        if (keyManager == null) {
            Log.e(TAG, "KeyManager is null");
            return false;
        }

        String privateKey = preferenceManager.getString("PRIVATE_KEY");
        String publicKey = preferenceManager.getString(Constants.KEY_PUBLIC_KEY);

        if (privateKey == null || publicKey == null) {
            Log.e(TAG, "Missing encryption keys");
            return false;
        }

        if (receiverUser == null || receiverUser.getPublicKey() == null) {
            Log.e(TAG, "Missing receiver's public key");
            return false;
        }

        return true;
    }



    private void sendMessages() {
        if (!validateEncryptionSetup()) {
            Toast.makeText(this, "Cannot send message: Encryption not properly initialized",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
        String messageText = binding.messageInput.getText().toString().trim();

        if (messageText.trim().isEmpty()) {
            Toast.makeText(this, "Cannot send empty message", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Generate a new AES key for this message
            SecretKey aesKey = keyManager.generateAESKey();

            // Encrypt the message with AES
            String encryptedMessage = keyManager.encryptMessage(messageText, aesKey);

            // Encrypt AES key for recipient
            String recipientEncryptedKey = keyManager.encryptAESKey(aesKey, receiverUser.getPublicKey());

            // Encrypt AES key for yourself (using your own public key)
            String senderEncryptedKey = keyManager.encryptAESKey(aesKey,
                    preferenceManager.getString(Constants.KEY_PUBLIC_KEY));

            HashMap<String, Object> message = new HashMap<>();
            message.put(Constants.KEY_SENDER_ID, currentUserId);
            message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
            message.put(Constants.KEY_ENCRYPTED_MESSAGE, encryptedMessage);
            message.put(Constants.KEY_ENCRYPTED_AES_KEY_RECIPIENT, recipientEncryptedKey);
            message.put(Constants.KEY_ENCRYPTED_AES_KEY_SENDER, senderEncryptedKey);
            message.put(Constants.KEY_MESSAGE_TYPE, Constants.MESSAGE_TYPE_TEXT);
            message.put(Constants.KEY_TIMESTAMP, new Date());

            database.collection(Constants.KEY_COLLECTION_CHAT)
                    .add(message)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Message sent successfully");
                        binding.messageInput.setText(null);
                        if (chatMessages != null && !chatMessages.isEmpty()) {
                            binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error sending message", e);
                        Toast.makeText(ChatActivity.this,
                                "Error sending message: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error in encryption process", e);
            Toast.makeText(ChatActivity.this,
                    "Error encrypting message: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
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
        }
        try {
            String encodedImage = ImageHandler.encodeImage(this, selectedImageUri);
            if (ImageHandler.isImageSizeValid(encodedImage)) {
                HashMap<String, Object> message = new HashMap<>();
                message.put(Constants.KEY_SENDER_ID, currentUserId);
                message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
                message.put(Constants.KEY_MESSAGE, encodedImage);  // Store encoded image directly
                message.put(Constants.KEY_MESSAGE_TYPE, Constants.MESSAGE_TYPE_IMAGE);  // Mark as image
                message.put(Constants.KEY_TIMESTAMP, new Date());

                database.collection(Constants.KEY_COLLECTION_CHAT).add(message)
                        .addOnSuccessListener(documentReference -> {
                            binding.messageInput.setText(null);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to send image", Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(this, "Selected image is too large", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
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
        if (error != null) return;

        if (value != null) {
            int count = chatMessages.size();
            String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);

            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    ChatMessages chatMessage = new ChatMessages();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverID = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);

                    String messageType = documentChange.getDocument().getString(Constants.KEY_MESSAGE_TYPE);

                    if (messageType != null && messageType.equals(Constants.MESSAGE_TYPE_IMAGE)) {
                        // Handle image message - no encryption
                        chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    } else {
                        // Handle text message - with encryption
                        String encryptedMessage = documentChange.getDocument().getString(Constants.KEY_ENCRYPTED_MESSAGE);
                        try {
                            String encryptedAESKey;
                            if (chatMessage.senderId.equals(currentUserId)) {
                                // We're the sender, use sender's key
                                Log.d(TAG, "Decrypting as sender");
                                encryptedAESKey = documentChange.getDocument().getString(Constants.KEY_ENCRYPTED_AES_KEY_SENDER);
                            } else {
                                // We're the receiver, use recipient's key
                                Log.d(TAG, "Decrypting as receiver");
                                encryptedAESKey = documentChange.getDocument().getString(Constants.KEY_ENCRYPTED_AES_KEY_RECIPIENT);
                            }

                            SecretKey aesKey = keyManager.decryptAESKey(encryptedAESKey);
                            chatMessage.message = keyManager.decryptMessage(encryptedMessage, aesKey);
                        } catch (Exception e) {
                            Log.e(TAG, "Error decrypting message", e);
                            chatMessage.message = "[Error: Could not decrypt message]";
                        }
                    }

                    chatMessage.dateTime = getReadableDateTime(
                            documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);
                }
            }
            Collections.sort(chatMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
            if (count == 0) {
                chatAdapter.notifyDataSetChanged();
            } else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
    };

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

        binding.profilePic.setOnClickListener(v -> viewProfile(receiverUser));
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

    private void viewProfile(User user){
        Intent intent = new Intent(this, ViewProfileActivity.class);
        intent.putExtra(Constants.KEY_USER,user);
        startActivity(intent);
    }
}