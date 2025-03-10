package com.lucas.sashat;

public class Book {
    private String title;
    private String author;
    private String genre;
    private int imageResId;

    public Book(String title, String author, String genre, int imageResId) {
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.imageResId = imageResId;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getGenre() {
        return genre;
    }

    public int getImageResId() {
        return imageResId;
    }
}
