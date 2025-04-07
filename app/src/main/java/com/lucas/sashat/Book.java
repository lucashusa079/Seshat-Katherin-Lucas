package com.lucas.sashat;

import java.util.List;

public class Book {
    private String title;
    private String author;
    private String coverImage;
    private String description;
    private List<String> genre;
    private double averageRating;
    private int ratingsCount;
    private List<String> recommendations;
    private int recommendationsCount;

    public Book() {}

    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getCoverImage() { return coverImage; }
    public String getDescription() { return description; }
    public List<String> getGenre() { return genre; }
    public double getAverageRating() { return averageRating; }
    public int getRatingsCount() { return ratingsCount; }
    public List<String> getRecommendations() { return recommendations; }
    public int getRecommendationsCount() { return recommendationsCount; }

    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
    public void setDescription(String description) { this.description = description; }
    public void setGenre(List<String> genre) { this.genre = genre; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    public void setRatingsCount(int ratingsCount) { this.ratingsCount = ratingsCount; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    public void setRecommendationsCount(int recommendationsCount) { this.recommendationsCount = recommendationsCount; }
}