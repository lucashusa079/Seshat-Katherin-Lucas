package com.lucas.sashat;

import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public interface CloudinaryApi {

    @Multipart
    @POST("v1_1/djacrzf1q/image/upload")
    Call<CloudinaryResponse> uploadImage(
            @Part MultipartBody.Part file,
            @Part("upload_preset") RequestBody uploadPreset
    );
}
