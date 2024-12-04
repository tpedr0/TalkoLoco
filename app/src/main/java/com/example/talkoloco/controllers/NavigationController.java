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

/**
 * Controller class responsible for managing bottom navigation across the application.
 * Handles navigation between main app sections and maintains navigation state.
 */
public class NavigationController {
    // Reference to the current activity for context
    private final Activity activity;

    /**
     * Initializes the navigation controller with the current activity.
     *
     * @param activity The activity that contains the navigation view
     */
    public NavigationController(Activity activity) {
        this.activity = activity;
    }

    /**
     * Configures the bottom navigation view with proper item selection handling
     * and initial state setup. Prevents unnecessary recreation of the current
     * activity when its navigation item is selected.
     *
     * Navigation flows:
     * - Chats: Main messaging interface (HomeActivity)
     * - Friends List: Contact management (FriendsListActivity)
     * - Settings: App configuration (SettingsActivity)
     *
     * @param bottomNavigationView The BottomNavigationView to be configured
     */
    public void setupNavigation(BottomNavigationView bottomNavigationView) {
        // Configure item selection listener
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            // Only navigate if we're not already on the selected screen
            if (id == R.id.chats && !(activity instanceof HomeActivity)) {
                activity.startActivity(new Intent(activity, HomeActivity.class));
            } else if (id == R.id.friendsList && !(activity instanceof FriendsListActivity)) {
                activity.startActivity(new Intent(activity, FriendsListActivity.class));
            } else if (id == R.id.settings && !(activity instanceof SettingsActivity)) {
                activity.startActivity(new Intent(activity, SettingsActivity.class));
            }
            return true; // Indicate the item selection was handled
        });

        // Set the initially selected item based on current activity
        if (activity instanceof HomeActivity) {
            bottomNavigationView.setSelectedItemId(R.id.chats);
        } else if (activity instanceof FriendsListActivity) {
            bottomNavigationView.setSelectedItemId(R.id.friendsList);
        } else if (activity instanceof SettingsActivity) {
            bottomNavigationView.setSelectedItemId(R.id.settings);
        }
    }
}