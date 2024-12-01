package com.example.talkoloco.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.talkoloco.adapters.UsersAdapter;
import com.example.talkoloco.controllers.NavigationController;
import com.example.talkoloco.databinding.ActivityFriendsListBinding;
import com.example.talkoloco.listeners.UserListener;
import com.example.talkoloco.models.User;
import com.example.talkoloco.utils.Constants;
import com.example.talkoloco.utils.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FriendsListActivity extends AppCompatActivity implements UserListener {
    private ActivityFriendsListBinding binding;
    private NavigationController navigationController;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityFriendsListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(getApplicationContext());

        navigationController = new NavigationController(this);
        navigationController.setupNavigation(binding.bottomNavigationView);

        getUsers();
    }

    private void getUsers(){
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS).get()
                .addOnCompleteListener(task -> {
                    String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
                    if(task.isSuccessful() && task.getResult() !=null){
                        List<User> users = new ArrayList<>();
                        for(QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()){
                            if(currentUserId.equals(queryDocumentSnapshot.getId())){
                                continue;
                            }
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
    private void showErrorMessage(){
        Toast.makeText(this,
                "No users found :(",
                Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onUserClicked(User user) {
        Intent intent = new Intent(this, ViewProfileActivity.class);
        intent.putExtra(Constants.KEY_USER,user);
        startActivity(intent);
    }
}