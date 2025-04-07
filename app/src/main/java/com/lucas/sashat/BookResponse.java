package com.lucas.sashat;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

// BookResponse.java
public class BookResponse {
    @SerializedName("items") private List<ApiBook> items;
    public List<ApiBook> getItems() { return items != null ? items : new ArrayList<>(); }
}
