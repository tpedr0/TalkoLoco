package com.example.talkoloco.utils;

public class Constants {
    // Existing collections
    public static final String KEY_COLLECTION_USERS = "users";
    public static final String KEY_COLLECTION_CHAT = "chats";

    // Updated user fields for hashed phone numbers
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_NAME = "name";
    public static final String KEY_PHONE_NUMBER = "phoneNumber";  // Changed to match hashed field
    public static final String KEY_PHONE_NUMBER_DISPLAY = "phoneNumber_display";  // Added for display
    public static final String KEY_PROFILE_PICTURE = "profilePictureUrl";
    public static final String KEY_STATUS = "status";
    public static final String KEY_FCM_TOKEN = "fcmToken";
    public static final String KEY_CREATED_AT = "createdAt";
    public static final String KEY_LAST_LOGIN = "lastLoginAt";

    // Existing chat fields remain the same
    public static final String KEY_USER = "user";
    public static final String KEY_SENDER_ID = "senderId";
    public static final String KEY_RECEIVER_ID = "receiverId";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_IMAGE = "profilePictureUrl";

    // Existing preferences remain the same
    public static final String KEY_PREFERENCE_NAME = "talkolocoPrefs";
    public static final String KEY_IS_SIGNED_IN = "isSignedIn";
}