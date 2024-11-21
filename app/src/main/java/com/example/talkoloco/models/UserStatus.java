package com.example.talkoloco.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserStatus {
    // list of default status options
    private static final List<String> DEFAULT_STATUSES = Arrays.asList(
            "WhatsApp is inferior compared to this!",
            "Available",
            "Busy",
            "At work",
            "At school",
            "In a meeting",
            "Battery about to die",
            "Can't talk right now",
            "At the gym",
            "Sleeping",
            "Only emergency calls"
    );

    // store custom statuses added by user
    private static ArrayList<String> customStatuses = new ArrayList<>();

    // get list of default and custom statuses
    public static List<String> getAllStatuses() {
        ArrayList<String> allStatuses = new ArrayList<>(DEFAULT_STATUSES);
        allStatuses.addAll(customStatuses);
        return allStatuses;
    }

    // get just the default statuses
    public static List<String> getDefaultStatuses() {
        return new ArrayList<>(DEFAULT_STATUSES);
    }

    // add a new custom status
    public static void addCustomStatus(String status) {
        if (!customStatuses.contains(status) && !DEFAULT_STATUSES.contains(status)) {
            customStatuses.add(status);
        }
    }

    // check if a status is one of the defaults
    public static boolean isDefaultStatus(String status) {
        return DEFAULT_STATUSES.contains(status);
    }

    // clear all custom statuses
    public static void clearCustomStatuses() {
        customStatuses.clear();
    }

    // get list of custom statuses
    public static List<String> getCustomStatuses() {
        return new ArrayList<>(customStatuses);
    }

    // save a status (either adds to custom or leaves as default)
    public static void saveStatus(String status) {
        if (!isDefaultStatus(status)) {
            addCustomStatus(status);
        }
    }
}