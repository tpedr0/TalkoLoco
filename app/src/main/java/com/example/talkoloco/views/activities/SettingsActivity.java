package com.example.talkoloco.views.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.talkoloco.R;
import com.example.talkoloco.databinding.ActivitySettingsBinding;

public class SettingsActivity extends AppCompatActivity {
    private ActivitySettingsBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.chats) {
                startActivity(new Intent(this, HomeActivity.class));
            } else if (id == R.id.communities) {
                startActivity(new Intent(this, CommunitiesActivity.class));
            } else if (id == R.id.settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            }
            return true;
        });
    }
}
