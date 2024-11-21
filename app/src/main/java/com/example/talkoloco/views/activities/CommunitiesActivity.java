package com.example.talkoloco.views.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.talkoloco.R;
import com.example.talkoloco.controllers.NavigationController;
import com.example.talkoloco.databinding.ActivityCommunitiesBinding;

public class CommunitiesActivity extends AppCompatActivity {
    private ActivityCommunitiesBinding binding;
    private NavigationController navigationController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCommunitiesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        navigationController = new NavigationController(this);
        navigationController.setupNavigation(binding.bottomNavigationView);
    }
}