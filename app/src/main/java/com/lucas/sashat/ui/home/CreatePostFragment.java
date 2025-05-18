package com.lucas.sashat.ui.home;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.load.resource.bitmap.CircleCrop;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lucas.sashat.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CreatePostFragment extends Fragment {

    private EditText postEditText;
    private ImageButton cameraButton, galleryButton, closeButton, deleteImageButton;
    private ImageView profileImageView, previewImageView;
    private TextView usernameTextView;
    private Spinner genreSpinner;
    private Button publishButton;
    private String postId = null;
    private Uri selectedImageUri;
    private FirebaseFirestore db;
    private Button deletePostButton;
    private String currentImageUrl;
    private FirebaseAuth auth;

    public CreatePostFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_post, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        postEditText = view.findViewById(R.id.postEditText);
        cameraButton = view.findViewById(R.id.cameraButton);
        galleryButton = view.findViewById(R.id.galleryButton);
        closeButton = view.findViewById(R.id.closeButton);
        profileImageView = view.findViewById(R.id.profileImageView);
        usernameTextView = view.findViewById(R.id.usernameTextView);
        genreSpinner = view.findViewById(R.id.genreSpinner);
        publishButton = view.findViewById(R.id.publishButton);
        previewImageView = view.findViewById(R.id.previewImageView);
        deleteImageButton = view.findViewById(R.id.deleteImageButton);
        deletePostButton = view.findViewById(R.id.deletePostButton);
        deletePostButton.setVisibility(View.GONE);
        previewImageView.setVisibility(View.GONE);
        deleteImageButton.setVisibility(View.GONE);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.genres, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genreSpinner.setAdapter(adapter);

        if (getArguments() != null) {
            postId = getArguments().getString("postId");
            String text = getArguments().getString("text");
            String genre = getArguments().getString("genre");
            String imageUrl = getArguments().getString("imageUrl");

            if (imageUrl != null) {
                previewImageView.setVisibility(View.VISIBLE);
                deleteImageButton.setVisibility(View.VISIBLE);
                Glide.with(this).load(imageUrl).into(previewImageView);
                currentImageUrl = imageUrl;
            }
            if (postId != null) {
                db.collection("posts").document(postId).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String userId = documentSnapshot.getString("userId");
                                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                if (currentUser != null && currentUser.getUid().equals(userId)) {
                                    deletePostButton.setVisibility(View.VISIBLE);
                                }
                            }
                        });
            }
            if (text != null) {
                postEditText.setText(text);
            }
            if (genre != null) {
                int position = adapter.getPosition(genre);
                genreSpinner.setSelection(position);
            }

            if (postId != null) {
                loadPostData(postId);
            }
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            usernameTextView.setText(username);
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Error al cargar usuario", Toast.LENGTH_SHORT).show());
        }

        SharedPreferences prefs = requireContext().getSharedPreferences("perfil", Context.MODE_PRIVATE);
        String profileImagePath = prefs.getString("profileImagePath", null);
        if (profileImagePath != null) {
            Glide.with(this).load(Uri.parse(profileImagePath)).into(profileImageView);
        }

        cameraButton.setOnClickListener(v -> openCamera());
        galleryButton.setOnClickListener(v -> openGallery());

        deleteImageButton.setOnClickListener(v -> {
            selectedImageUri = null;
            previewImageView.setImageDrawable(null);
            previewImageView.setVisibility(View.GONE);
            deleteImageButton.setVisibility(View.GONE);

            if (postId != null) {
                db.collection("posts").document(postId)
                        .update("imageUrl", null)
                        .addOnSuccessListener(unused -> Log.d("CreatePost", "Imagen eliminada del post"))
                        .addOnFailureListener(e -> Log.e("CreatePost", "Error al eliminar imagen", e));
            }
        });

        closeButton.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(CreatePostFragment.this);
            navController.popBackStack();
        });

        publishButton.setOnClickListener(v -> {
            String postText = postEditText.getText().toString().trim();
            String selectedGenre = genreSpinner.getSelectedItem().toString();

            if (postText.isEmpty()) {
                Toast.makeText(getContext(), "Por favor escribe algo para publicar.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedGenre.equals("Selecciona un género") || selectedGenre.isEmpty()) {
                Toast.makeText(getContext(), "Por favor selecciona un género literario.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentUser != null) {
                String userId = currentUser.getUid();
                if (postId == null) {
                    postId = UUID.randomUUID().toString(); // Solo generamos uno nuevo si es creación
                }

                Map<String, Object> post = new HashMap<>();
                post.put("userId", userId);
                post.put("text", postText);
                post.put("genre", selectedGenre);
                post.put("timestamp", Timestamp.now());

                publishButton.setEnabled(false);
                publishButton.setText("Publicando...");

                if (selectedImageUri != null) {
                    new Thread(() -> {
                        try {
                            File file = getFileFromUri(requireContext(), selectedImageUri);
                            requireActivity().runOnUiThread(() -> uploadImageToImgur(file, postId, post));
                        } catch (IOException e) {
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
                                publishButton.setEnabled(true);
                                publishButton.setText("Publicar");
                            });
                        }
                    }).start();
                } else {
                    savePostToFirestore(postId, post);
                    publishButton.setEnabled(true);
                    publishButton.setText("Publicar");
                }
            }
        });

        deletePostButton.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(getContext())
                    .setTitle("¿Eliminar publicación?")
                    .setMessage("¿Estás seguro de que deseas eliminar esta publicación? Esta acción no se puede deshacer.")
                    .setPositiveButton("Eliminar", (dialog, which) -> {
                        if (postId != null) {
                            db.collection("posts").document(postId)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        if (currentImageUrl != null) {
                                            deleteImageFromImgur(currentImageUrl);
                                        }
                                        Toast.makeText(getContext(), "Post eliminado", Toast.LENGTH_SHORT).show();
                                        NavHostFragment.findNavController(CreatePostFragment.this).popBackStack();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(getContext(), "Error al eliminar", Toast.LENGTH_SHORT).show()
                                    );
                        }
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        loadUserProfileImage();
        return view;
    }

    private void loadUserProfileImage() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String imagePath = documentSnapshot.getString("profileImagePath");
                            if (imagePath != null) {
                                Glide.with(this).load(Uri.parse(imagePath)).into(profileImageView);
                            }
                        }
                    });
        }
    }

    private void loadPostData(String postId) {
        db.collection("posts").document(postId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String text = documentSnapshot.getString("text");
                        String genre = documentSnapshot.getString("genre");
                        String imageUrl = documentSnapshot.getString("imageUrl");

                        if (text != null) {
                            postEditText.setText(text);
                        }
                        if (genre != null) {
                            ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) genreSpinner.getAdapter();
                            int position = adapter.getPosition(genre);
                            genreSpinner.setSelection(position);
                        }
                        if (imageUrl != null) {
                            previewImageView.setVisibility(View.VISIBLE);
                            deleteImageButton.setVisibility(View.VISIBLE);
                            Glide.with(this).load(imageUrl).into(previewImageView);
                            currentImageUrl = imageUrl;
                        }
                    }
                });
    }

    private File getFileFromUri(Context context, Uri uri) throws IOException {
        File tempFile = File.createTempFile("upload", ".jpg", context.getCacheDir());
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
        }
        return tempFile;
    }

    private void uploadImageToImgur(File imageFile, String postId, Map<String, Object> post) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.imgur.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        ImgurApi apiService = retrofit.create(ImgurApi.class);

        RequestBody requestFile = RequestBody.create(imageFile, MediaType.parse("image/*"));
        MultipartBody.Part body = MultipartBody.Part.createFormData("image", imageFile.getName(), requestFile);

        // Cambia "Client-ID TU_CLIENT_ID" por tu Client-ID real
        Call<ImgurResponse> call = apiService.uploadImage("Client-ID eadd0498ebb4045", body);

        call.enqueue(new Callback<ImgurResponse>() {
            @Override
            public void onResponse(Call<ImgurResponse> call, Response<ImgurResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    String imageUrl = response.body().getData().getLink();
                    Log.d("ImgurUpload", "Imagen subida con éxito: " + imageUrl);
                    post.put("imageUrl", imageUrl);
                    savePostToFirestore(postId, post);
                } else {
                    String errorBodyStr = "No body";
                    try {
                        if (response.errorBody() != null) {
                            errorBodyStr = response.errorBody().string();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.e("ImgurUpload", "Error al subir imagen. Código: " + response.code() + "\nError body:\n" + errorBodyStr);
                    Toast.makeText(getContext(), "Error al subir imagen: " + response.code(), Toast.LENGTH_LONG).show();

                    publishButton.setEnabled(true);
                    publishButton.setText("Publicar");
                }
            }

            @Override
            public void onFailure(Call<ImgurResponse> call, Throwable t) {
                Log.e("ImgurUpload", "Fallo en la subida", t);
                Toast.makeText(getContext(), "Fallo en la conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();

                publishButton.setEnabled(true);
                publishButton.setText("Publicar");
            }
        });
    }

    private void savePostToFirestore(String postId, Map<String, Object> post) {
        db.collection("posts").document(postId).set(post)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(), "Publicación exitosa", Toast.LENGTH_SHORT).show();
                    publishButton.setEnabled(true);
                    publishButton.setText("Publicar");
                    NavHostFragment.findNavController(CreatePostFragment.this).popBackStack();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al guardar publicación", Toast.LENGTH_SHORT).show();
                    publishButton.setEnabled(true);
                    publishButton.setText("Publicar");
                });
    }

    private void deleteImageFromImgur(String imageUrl) {
        // Aquí implementa tu lógica para eliminar la imagen en Imgur, si la tienes
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 100);
    }

    private void openCamera() {
        // Aquí implementa abrir cámara si quieres
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == 100) {
                selectedImageUri = data.getData();
                previewImageView.setVisibility(View.VISIBLE);
                deleteImageButton.setVisibility(View.VISIBLE);
                Glide.with(this).load(selectedImageUri).into(previewImageView);
            }
        }
    }
}
