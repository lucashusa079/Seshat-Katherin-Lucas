package com.lucas.sashat;

import com.google.firebase.Timestamp;
import java.util.List;

public class Book {
    private String title;
    private String author;
    private String imageUrl; // Puede ser un Uri local (content://) o una URL remota (https://)
    private String description;
    private String genre;
    private double averageRating;
    private int ratingsCount;
    private List<String> recommendations;
    private int recommendationsCount;
    private String addedBy;
    private String id;
    private Timestamp timestamp;
    private boolean isLocalImage; // Nuevo campo

    public Book() {
        this.averageRating = 0.0;
        this.ratingsCount = 0;
        this.recommendations = null;
        this.recommendationsCount = 0;
        this.isLocalImage = false;
    }

    // Getters
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getImageUrl() { return imageUrl; }
    public String getDescription() { return description; }
    public String getGenre() { return genre; }
    public double getAverageRating() { return averageRating; }
    public int getRatingsCount() { return ratingsCount; }
    public List<String> getRecommendations() { return recommendations; }
    public int getRecommendationsCount() { return recommendationsCount; }
    public String getAddedBy() { return addedBy; }
    public String getId() { return id; }
    public Timestamp getTimestamp() { return timestamp; }
    public boolean isLocalImage() { return isLocalImage; }

    // Setters
    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setDescription(String description) { this.description = description; }
    public void setGenre(String genre) { this.genre = genre; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    public void setRatingsCount(int ratingsCount) { this.ratingsCount = ratingsCount; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    public void setRecommendationsCount(int recommendationsCount) { this.recommendationsCount = recommendationsCount; }
    public void setAddedBy(String addedBy) { this.addedBy = addedBy; }
    public void setId(String id) { this.id = id; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public void setLocalImage(boolean localImage) { isLocalImage = localImage; }
}