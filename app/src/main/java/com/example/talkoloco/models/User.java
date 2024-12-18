package com.example.talkoloco.models;

import java.io.Serializable;

public class User implements Serializable {
    private String phoneNumber;

    //changed from private to public so it can be accessed from the ChatActivity class
    public String name;
    public String profilePictureUrl;
    private String userId;
    private long createdAt;
    private long lastLoginAt;
    private String status;
    private String phoneNumber_display;
    private String phoneNumber_hash;
    private String publicKey;

    public String image, token, id;


    /**
     * constructs a new User instance with the required empty constructor for Firebase.
     * this constructor sets the creation and last login timestamps.
     */
    public User() {
        this.createdAt = System.currentTimeMillis();
        this.lastLoginAt = System.currentTimeMillis();
    }

    /**
     * constructs a new User instance with the provided phone number.
     * this constructor calls the empty constructor to set the timestamps.
     *
     * @param phoneNumber the phone number of the user
     */
    public User(String phoneNumber) {
        this();  // Call empty constructor to set timestamps
        this.phoneNumber = phoneNumber;
    }

    /**
     * Returns the phone number of the user.
     *
     * @return the phone number of the user
     */
    public String getPhoneNumber() {
        return phoneNumber_display;
    }

    /**
     * Sets the phone number of the user.
     *
     * @param phoneNumber the new phone number of the user
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber_display = phoneNumber;
    }

    /**
     * Returns the name of the user.
     *
     * @return the name of the user
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the user.
     *
     * @param name the new name of the user
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the profile picture URL of the user.
     *
     * @return the profile picture URL of the user
     */
    public String getProfilePictureUrl(){
        return profilePictureUrl;
    }

    /**
     * Sets the profile picture URL of the user.
     *
     * @param profilePictureUrl the new profile picture URL of the user
     */
    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    /**
     * Returns the user ID of the user.
     *
     * @return the user ID of the user
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the user ID of the user.
     *
     * @param userId the new user ID of the user
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Returns the creation timestamp of the user.
     *
     * @return the creation timestamp of the user
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation timestamp of the user.
     *
     * @param createdAt the new creation timestamp of the user
     */
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Returns the last login timestamp of the user.
     *
     * @return the last login timestamp of the user
     */
    public long getLastLoginAt() {
        return lastLoginAt;
    }

    /**
     * Sets the last login timestamp of the user.
     *
     * @param lastLoginAt the new last login timestamp of the user
     */
    public void setLastLoginAt(long lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    /**
     * Updates the last login timestamp of the user to the current time.
     */
    public void updateLastLogin() {
        this.lastLoginAt = System.currentTimeMillis();
    }

    /**
     * Checks if the user's profile is complete, i.e., the name is not null and not empty.
     *
     * @return true if the user's profile is complete, false otherwise
     */
    public boolean isProfileComplete() {
        return name != null && !name.trim().isEmpty();
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public String getPhoneNumber_display() {
        return phoneNumber_display;
    }

    public void setPhoneNumber_display(String phoneNumber_display) {
        this.phoneNumber_display = phoneNumber_display;
    }

    public String getPhoneNumber_hash() {
        return phoneNumber_hash;
    }

    public void setPhoneNumber_hash(String phoneNumber_hash) {
        this.phoneNumber_hash = phoneNumber_hash;
    }

    /**
     * Returns the user's public key for message encryption.
     *
     * @return the public key of the user
     */
    public String getPublicKey() {
        return publicKey;
    }

    /**
     * Sets the user's public key.
     *
     * @param publicKey the public key for message encryption
     */
    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
}

