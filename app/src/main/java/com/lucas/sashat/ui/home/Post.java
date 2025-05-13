package com.lucas.sashat.ui.home;

import com.google.firebase.firestore.Exclude;

import java.util.Date;

public class Post {
    private String genre;
    private String text;
    private String userId;
    private String username;
    private String userPhotoUrl;
    private String imageUrl;
    private String postId;
    private Date timestamp;

    public Post() {
        // Constructor vacío requerido para Firestore
    }
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getImageUrl() { return imageUrl; }

    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getGenre() {
        return genre;
    }

    public String getText() {
        return text;
    }

    public String getUserId() {
        return userId;
    }


    public String getUserProfileImage() {
        return userPhotoUrl;
    }

    public String getUsername() {
        return username != null ? username : "Usuario";
    }
    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getPostId() {
        return postId;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUserPhotoUrl(String userPhotoUrl) {
        this.userPhotoUrl = userPhotoUrl;
    }

    public void setUsername(String username) {
        this.username = username;
    }

}
