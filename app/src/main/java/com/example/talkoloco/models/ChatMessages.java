package com.example.talkoloco.models;

import java.util.Date;

public class ChatMessages {
    // Informational objects of the chat
    public String senderId;
    public String receiverID;
    public String message;
    public String dateTime;
    public Date dateObject;

    // Empty constructor for Firebase
    public ChatMessages() {
        // Required for Firebase
    }

    // Full constructor
    public ChatMessages(String senderId, String receiverID, String message, String dateTime, Date dateObject) {
        this.senderId = senderId;
        this.receiverID = receiverID;
        this.message = message;
        this.dateTime = dateTime;
        this.dateObject = dateObject;
    }

    // Getters with null checks
    public String getSenderId() {
        return senderId != null ? senderId : "";
    }

    public String getReceiverID() {
        return receiverID != null ? receiverID : "";
    }

    public String getMessage() {
        return message != null ? message : "";
    }

    public String getDateTime() {
        return dateTime != null ? dateTime : "";
    }

   /* public Date getDateObject() {
        return dateObject != null ? dateObject : new Date();
    }

    */
}