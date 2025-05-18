package com.lucas.sashat.ui.home;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ImgurApi {
    @Multipart
    @POST("3/image")
    Call<ImgurResponse> uploadImage(
            @Header("Authorization") String auth,
            @Part MultipartBody.Part image
    );
    @DELETE("3/image/{deleteHash}")
    Call<Void> deleteImage(
            @Header("Authorization") String auth,
            @Path("deleteHash") String deleteHash
    );
}
