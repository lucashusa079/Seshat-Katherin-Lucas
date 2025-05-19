package com.lucas.sashat;

import com.google.firebase.Timestamp;

import org.parceler.Parcel;

@Parcel
public class Book {
    private String title;
    private String author;
    private String coverImage; // Cambiado de imageUrl a imageUri para coincidir con Firebase
    private String notes; // Reemplaza description para las notas del usuario
    private String genre;
    private double rating; // Para la puntuación del usuario (0.0 a 5.0)
    private String addedBy;
    private String id;
    private Timestamp timestamp;
    private Timestamp startDate; // Fecha de inicio
    private Timestamp endDate; // Fecha de fin
    private boolean isLocalImage;

    public Book() {
        this.rating = 0.0;
        this.isLocalImage = false;
    }

    // Getters
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getCoverImage() { return coverImage; }
    public String getNotes() { return notes; }
    public String getGenre() { return genre; }
    public double getRating() { return rating; }
    public String getAddedBy() { return addedBy; }
    public String getId() { return id; }
    public Timestamp getTimestamp() { return timestamp; }
    public Timestamp getStartDate() { return startDate; }
    public Timestamp getEndDate() { return endDate; }
    public boolean isLocalImage() { return isLocalImage; }

    // Setters
    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setGenre(String genre) { this.genre = genre; }
    public void setRating(double rating) { this.rating = rating; }
    public void setAddedBy(String addedBy) { this.addedBy = addedBy; }
    public void setId(String id) { this.id = id; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public void setStartDate(Timestamp startDate) { this.startDate = startDate; }
    public void setEndDate(Timestamp endDate) { this.endDate = endDate; }
    public void setLocalImage(boolean localImage) { this.isLocalImage = localImage; }
}