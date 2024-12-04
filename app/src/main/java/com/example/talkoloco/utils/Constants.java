package com.example.talkoloco.utils;

/**
 * Constants class containing all static final values used throughout the application.
 * Centralizes configuration values and string constants for consistency and maintainability.
 */
public class Constants {

    // firebase collections
    public static final String KEY_COLLECTION_USERS = "users";
    public static final String KEY_COLLECTION_CHAT = "chats";

    // user fields
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_NAME = "name";
    public static final String KEY_PHONE_NUMBER = "phoneNumber";
    public static final String KEY_PROFILE_PICTURE = "profilePictureUrl";
    public static final String KEY_STATUS = "status";
    public static final String KEY_FCM_TOKEN = "fcmToken";
    public static final String KEY_CREATED_AT = "createdAt";
    public static final String KEY_LAST_LOGIN = "lastLoginAt";

    // chat fields
    public static final String KEY_USER = "user";
    public static final String KEY_SENDER_ID = "senderId";
    public static final String KEY_RECEIVER_ID = "receiverId";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_IMAGE = "profilePictureUrl";


    // shared preferences
    public static final String KEY_PREFERENCE_NAME = "talkolocoPrefs";
    public static final String KEY_IS_SIGNED_IN = "isSignedIn";

    // New constants for encryption
    public static final String KEY_PUBLIC_KEY = "publicKey";
    public static final String KEY_ENCRYPTED_MESSAGE = "encryptedMessage";
    public static final String KEY_ENCRYPTED_AES_KEY = "encryptedAESKey";
    public static final String KEY_ENCRYPTED_AES_KEY_RECIPIENT = "encryptedAESKeyRecipient";
    public static final String KEY_ENCRYPTED_AES_KEY_SENDER = "encryptedAESKeySender";
    public static final String KEY_MESSAGE_TYPE = "messageType";
    public static final String MESSAGE_TYPE_TEXT = "text";
    public static final String MESSAGE_TYPE_IMAGE = "image";





}