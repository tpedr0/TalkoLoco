package com.example.talkoloco.views.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.example.talkoloco.R;
import com.example.talkoloco.controllers.NavigationController;
import com.example.talkoloco.databinding.ActivityHomeBinding ;


public class HomeActivity extends AppCompatActivity {
    private ActivityHomeBinding  binding;

    private NavigationController navigationController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        navigationController = new NavigationController(this);
        navigationController.setupNavigation(binding.bottomNavigationView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}