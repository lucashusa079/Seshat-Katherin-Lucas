package com.lucas.sashat.ui.home;

import com.google.firebase.Timestamp;

public class Comment {
    private String userId;
    private String username;
    private String text;
    private Timestamp timestamp;

    public Comment() {} // Requerido por Firestore

    public Comment(String userId, String username, String text, Timestamp timestamp) {
        this.userId = userId;
        this.username = username;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getText() { return text; }
    public Timestamp getTimestamp() { return timestamp; }
}
