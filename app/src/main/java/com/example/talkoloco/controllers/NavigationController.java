// NavigationController.java in controllers package
package com.example.talkoloco.controllers;

import android.app.Activity;
import android.content.Intent;
import com.example.talkoloco.R;
import com.example.talkoloco.views.activities.CommunitiesActivity;
import com.example.talkoloco.views.activities.FriendsListActivity;
import com.example.talkoloco.views.activities.HomeActivity;
import com.example.talkoloco.views.activities.SettingsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class NavigationController {
    private final Activity activity;

    public NavigationController(Activity activity) {
        this.activity = activity;
    }

    public void setupNavigation(BottomNavigationView bottomNavigationView) {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.chats && !(activity instanceof HomeActivity)) {
                activity.startActivity(new Intent(activity, HomeActivity.class));
            } else if (id == R.id.friendsList && !(activity instanceof FriendsListActivity)) {
                activity.startActivity(new Intent(activity, FriendsListActivity.class));
            } else if (id == R.id.settings && !(activity instanceof SettingsActivity)) {
                activity.startActivity(new Intent(activity, SettingsActivity.class));
            }
            return true;
        });

        // set selected item
        if (activity instanceof HomeActivity) {
            bottomNavigationView.setSelectedItemId(R.id.chats);
        } else if (activity instanceof FriendsListActivity) {
            bottomNavigationView.setSelectedItemId(R.id.friendsList);
        } else if (activity instanceof SettingsActivity) {
            bottomNavigationView.setSelectedItemId(R.id.settings);
        }
    }
}