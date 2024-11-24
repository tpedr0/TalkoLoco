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

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private User receiverUser;
    private Uri selectedImageUri;
    private List<ChatMessages> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;

    private FirebaseFirestore database;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    //send image
                }
            }
    );



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "ChatActivity onCreate started");

        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get user from intent
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
            return;
        }

        if (binding.messageInput.getText().toString().trim().isEmpty()) {
            Toast.makeText(ChatActivity.this,
                    "Message cannot be empty",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, currentUserId);
        message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
        message.put(Constants.KEY_MESSAGE, binding.messageInput.getText().toString());
        message.put(Constants.KEY_TIMESTAMP, new Date());

        Log.d("ChatDebug", "Message object: " + message.toString());

        database.collection(Constants.KEY_COLLECTION_CHAT)
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    Log.d("ChatDebug", "Message sent successfully");
                    binding.messageInput.setText(null);
                    // Optionally scroll to bottom of chat
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
                    ChatMessages chatMessage = new ChatMessages();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverID = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.dateTime = getReadableDateTime(
                            documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);

                    chatMessages.add(chatMessage);
                }
            }
            Collections.sort(chatMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
            if(count == 0){
                chatAdapter.notifyDataSetChanged();
            }else{
                chatAdapter.notifyItemRangeChanged(chatMessages.size(),chatMessages.size());

                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size()-1);
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
