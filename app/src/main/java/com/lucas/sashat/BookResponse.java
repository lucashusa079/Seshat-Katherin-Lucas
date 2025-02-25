package com.lucas.sashat;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class BookResponse {
    @SerializedName("items")
    private List<BookItem> items;

    public List<BookItem> getItems() {
        return items;
    }

    public static class BookItem {
        @SerializedName("id")
        private String id;

        @SerializedName("volumeInfo")
        private VolumeInfo volumeInfo;

        public String getId() {
            return id;
        }

        public VolumeInfo getVolumeInfo() {
            return volumeInfo;
        }
    }

    public static class VolumeInfo {
        @SerializedName("title")
        private String title;

        @SerializedName("authors")
        private List<String> authors;

        @SerializedName("imageLinks")
        private ImageLinks imageLinks;

        public String getTitle() {
            return title;
        }

        public List<String> getAuthors() {
            return authors;
        }

        public ImageLinks getImageLinks() {
            return imageLinks;
        }
    }

    public static class ImageLinks {
        @SerializedName("thumbnail")
        private String thumbnail;

        public String getThumbnail() {
            return thumbnail;
        }
    }
}

