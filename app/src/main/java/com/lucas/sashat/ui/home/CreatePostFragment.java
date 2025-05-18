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

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lucas.sashat.R;

import java.io.File;
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

    public CreatePostFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_post, container, false);

        db = FirebaseFirestore.getInstance();

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

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
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

                if (selectedImageUri != null) {
                    File file = new File(getPath(requireContext(), selectedImageUri));
                    uploadImageToImgur(file, postId, post);
                } else {
                    savePostToFirestore(postId, post);
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


        return view;
    }
    private void deleteImageFromImgur(String imageUrl) {
        String deleteHash = extractDeleteHash(imageUrl);
        if (deleteHash == null) return;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.imgur.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ImgurApi imgurApi = retrofit.create(ImgurApi.class);

        Call<Void> call = imgurApi.deleteImage("Client-ID e0b28a1739f9afc", deleteHash);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    Log.e("Imgur", "Error al borrar imagen: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("Imgur", "Fallo al conectar con Imgur: " + t.getMessage());
            }
        });
    }

    // Extrae el ID de la imagen desde la URL (https://i.imgur.com/abc123.jpg => abc123)
    private String extractDeleteHash(String imageUrl) {
        try {
            Uri uri = Uri.parse(imageUrl);
            String filename = uri.getLastPathSegment(); // abc123.jpg
            if (filename != null && filename.contains(".")) {
                return filename.substring(0, filename.indexOf(".")); // abc123
            }
        } catch (Exception e) {
            Log.e("Imgur", "Error extrayendo ID de imagen", e);
        }
        return null;
    }

    private void loadPostData(String postId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("posts").document(postId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String text = documentSnapshot.getString("text");
                        String genre = documentSnapshot.getString("genre");
                        postEditText.setText(text);
                        int position = ((ArrayAdapter) genreSpinner.getAdapter()).getPosition(genre);
                        genreSpinner.setSelection(position);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error al cargar la publicación", Toast.LENGTH_SHORT).show());
    }

    private void uploadImageToImgur(File imageFile, String postId, Map<String, Object> post) {
        long fileSize = imageFile.length();
        if (fileSize > 10 * 1024 * 1024) {
            Toast.makeText(getContext(), "La imagen es demasiado grande (máx. 10 MB)", Toast.LENGTH_SHORT).show();
            return;
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.imgur.com/")
                .client(new OkHttpClient.Builder()
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ImgurApi imgurApi = retrofit.create(ImgurApi.class);

        RequestBody requestBody = RequestBody.create(MediaType.parse("image/*"), imageFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("image", imageFile.getName(), requestBody);

        Call<ImgurResponse> call = imgurApi.uploadImage("Client-ID e0b28a1739f9afc", body);

        call.enqueue(new Callback<ImgurResponse>() {
            @Override
            public void onResponse(Call<ImgurResponse> call, Response<ImgurResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String imageUrl = response.body().data.link;
                    post.put("imageUrl", imageUrl);
                    savePostToFirestore(postId, post);
                } else {
                    Toast.makeText(getContext(), "Error al subir imagen a Imgur", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ImgurResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Falló la subida: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void savePostToFirestore(String postId, Map<String, Object> post) {
        db.collection("posts").document(postId).set(post)
                .addOnSuccessListener(aVoid -> {
                    NavController navController = NavHostFragment.findNavController(CreatePostFragment.this);
                    navController.popBackStack();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error al publicar", Toast.LENGTH_SHORT).show());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 101);
    }

    private void openCamera() {
        Toast.makeText(getContext(), "Abrir cámara (a implementar)", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                previewImageView.setImageURI(selectedImageUri);
                previewImageView.setVisibility(View.VISIBLE);
                deleteImageButton.setVisibility(View.VISIBLE);
            }
        }
    }

    public static String getPath(Context context, Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return getPathForApi29AndAbove(context, uri);
        } else {
            return getPathForLegacy(context, uri);
        }
    }

    private static String getPathForApi29AndAbove(Context context, Uri uri) {
        String[] projection = {MediaStore.Images.Media.DISPLAY_NAME};
        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                return cursor.getString(columnIndex);
            }
        }
        return null;
    }

    private static String getPathForLegacy(Context context, Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                return cursor.getString(columnIndex);
            }
        }
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
    }
}
