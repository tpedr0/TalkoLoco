package com.example.talkoloco.views.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.talkoloco.R;
import com.example.talkoloco.databinding.ActivityCommunitiesBinding;

public class CommunitiesActivity extends AppCompatActivity {
    private ActivityCommunitiesBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCommunitiesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.chats) {
                startActivity(new Intent(this, MainActivity.class));
            } else if (id == R.id.communities) {
                startActivity(new Intent(this, CommunitiesActivity.class));
            } else if (id == R.id.settings) {
                startActivity(new Intent(this, ProfileCreationActivity.class));
            }
            return true;
        });
    }
}
