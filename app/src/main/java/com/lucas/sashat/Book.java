package com.lucas.sashat;

import java.util.List;

public class Book {
    private String title;
    private String author;
    private List<String> genre;
    private String coverImage;

    // Constructor vacío necesario para Firestore
    public Book() { }

    public Book(String title, String author, List<String> genre, String coverImage) {
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.coverImage = coverImage;
    }

    // Getters y setters
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public List<String> getGenre() { return genre; }
    public String getCoverImage() { return coverImage; }
}
