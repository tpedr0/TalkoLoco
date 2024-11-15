package com.example.talkoloco.views.activities;

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

            switch (item.getItemId()){
                case R.id.chats:
                    break;
                case R.id.communities:
                    break;
                case R.id.settings:
                    break;
            }

            return true;
        });
    }
}
