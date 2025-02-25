package com.lucas.sashat;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface GoogleBooksApiService {
    @GET("volumes")
    Call<BookResponse> searchBooks(
            @Query("q") String query,
            @Query("key") String apiKey
    );
}

