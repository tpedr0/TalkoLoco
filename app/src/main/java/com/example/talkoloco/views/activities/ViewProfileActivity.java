package com.example.talkoloco.views.activities;

import static android.content.ContentValues.TAG;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.talkoloco.controllers.NavigationController;
import com.example.talkoloco.databinding.ActivityViewProfileBinding;
import com.example.talkoloco.models.User;
import com.example.talkoloco.utils.Constants;
import com.example.talkoloco.utils.ImageHandler;

public class ViewProfileActivity extends AppCompatActivity {

    ActivityViewProfileBinding binding;
    private NavigationController navigationController;
    private User viewedUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityViewProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //listen for backpress
        binding.backButton.setOnClickListener(v -> onBackPressed());

        //set up navigation bar
        navigationController = new NavigationController(this);

        navigationController.setupNavigation(binding.bottomNavigationView);

        viewedUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        if (viewedUser == null) {
            Log.e(TAG, "Receiver user is null");
            Toast.makeText(this, "Error: No user data received", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadReceiverDetails();
    }

    private void loadReceiverDetails() {
        viewedUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        //handle profile picture
        Bitmap profileBitmap = ImageHandler.decodeImage(viewedUser.getProfilePictureUrl());
        if (viewedUser != null && viewedUser.profilePictureUrl != null && !viewedUser.profilePictureUrl.isEmpty()) {
            binding.profileIcon.setImageBitmap(profileBitmap);
            binding.aboutOutput.setText(viewedUser.getStatus());
        }

        if (viewedUser != null) {
            binding.nameOutput.setText(viewedUser.getName());
        } else {
            Toast.makeText(this, "User details not found", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity if no user details are found
        }
    }


}
