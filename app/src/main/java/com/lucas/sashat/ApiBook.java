package com.lucas.sashat;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class ApiBook {
    @SerializedName("id") private String id;
    @SerializedName("volumeInfo") private VolumeInfo volumeInfo;

    public String getId() { return id; }
    public VolumeInfo getVolumeInfo() { return volumeInfo; }

    public static class VolumeInfo {
        @SerializedName("title") private String title;
        @SerializedName("authors") private List<String> authors;
        @SerializedName("imageLinks") private ImageLinks imageLinks;
        @SerializedName("categories") private List<String> categories;
        @SerializedName("description") private String description; // Añadido para el campo description

        public String getTitle() { return title; }
        public List<String> getAuthors() { return authors != null ? authors : new ArrayList<>(); }
        public ImageLinks getImageLinks() { return imageLinks; }
        public List<String> getCategories() { return categories != null ? categories : new ArrayList<>(); }
        public String getDescription() { return description != null ? description : ""; } // Getter para description
    }

    public static class ImageLinks {
        @SerializedName("smallThumbnail") private String smallThumbnail;
        @SerializedName("thumbnail") private String thumbnail;

        public String getSmallThumbnail() { return smallThumbnail; }
        public String getThumbnail() { return thumbnail; }
    }
}