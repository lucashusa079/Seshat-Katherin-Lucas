package com.lucas.sashat.ui.personal;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lucas.sashat.CloudinaryApi;
import com.lucas.sashat.CloudinaryResponse;
import com.lucas.sashat.R;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ProfileFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView profileImage;
    private TextView changeImage;
    private Uri imageUri;
    private Button uploadButton;
    private EditText etUsername, etDescription;
    private String userId;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Inicialización de Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userId = auth.getCurrentUser().getUid();

        // Referencias de UI
        profileImage = view.findViewById(R.id.ivProfile);
        changeImage = view.findViewById(R.id.tvChangePhoto);
        uploadButton = view.findViewById(R.id.btnGuardar);
        etUsername = view.findViewById(R.id.etUsername);
        etDescription = view.findViewById(R.id.etDescripcion);

        // Cargar datos actuales del usuario
        loadUserData();

        profileImage.setOnClickListener(v -> openGallery());
        changeImage.setOnClickListener(v -> openGallery());
        uploadButton.setOnClickListener(v -> saveProfileChanges());

        return view;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.getData();
            Glide.with(this).load(imageUri).into(profileImage);
        }
    }

    private void loadUserData() {
        db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String username = documentSnapshot.getString("username");
                String description = documentSnapshot.getString("description");
                String photoUrl = documentSnapshot.getString("photoUrl");

                etUsername.setText(username);
                etDescription.setText(description);

                if (photoUrl != null && !photoUrl.isEmpty()) {
                    Picasso.get().load(photoUrl).into(profileImage);
                }
            }
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Error al cargar datos", Toast.LENGTH_SHORT).show()
        );
    }

    private void saveProfileChanges() {
        String newUsername = etUsername.getText().toString().trim();
        String newDescription = etDescription.getText().toString().trim();

        if (newUsername.isEmpty() || newDescription.isEmpty()) {
            Toast.makeText(getContext(), "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("username", newUsername);
        updates.put("description", newDescription);

        // Actualizar foto solo si se seleccionó una nueva imagen
        if (imageUri != null) {
            uploadImageToCloudinary(imageUri, newUsername, newDescription);
        } else {
            updateFirestoreData(updates);
        }
    }

    private void uploadImageToCloudinary(Uri imageUri, String newUsername, String newDescription) {
        File file = new File(getRealPathFromURI(imageUri));
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
        RequestBody uploadPreset = RequestBody.create(MediaType.parse("text/plain"), "my_preset");

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.cloudinary.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        CloudinaryApi api = retrofit.create(CloudinaryApi.class);
        Call<CloudinaryResponse> call = api.uploadImage(body, uploadPreset);

        call.enqueue(new Callback<CloudinaryResponse>() {
            @Override
            public void onResponse(Call<CloudinaryResponse> call, Response<CloudinaryResponse> response) {
                if (response.isSuccessful()) {
                    String imageUrl = response.body().getSecure_url();
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("username", newUsername);
                    updates.put("description", newDescription);
                    updates.put("photoUrl", imageUrl);
                    updateFirestoreData(updates);
                }
            }

            @Override
            public void onFailure(Call<CloudinaryResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Error al subir la imagen", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateFirestoreData(Map<String, Object> updates) {
        DocumentReference userRef = db.collection("users").document(userId);
        userRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();
                    navigateToPersonalFragment();  // Método para navegar a PersonalFragment
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error al actualizar perfil", Toast.LENGTH_SHORT).show()
                );
    }

    private void navigateToPersonalFragment() {
        NavController navController = NavHostFragment.findNavController(ProfileFragment.this);
        navController.navigate(R.id.action_profileFragment_to_personalFragment);
    }

    private String getRealPathFromURI(Uri uri) {
        Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) {
            return uri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            String path = cursor.getString(index);
            cursor.close();
            return path;
        }
    }
}
