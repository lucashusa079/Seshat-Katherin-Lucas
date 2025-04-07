package com.lucas.sashat.ui.personal;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lucas.sashat.R;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int STORAGE_PERMISSION_CODE = 100;
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

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userId = auth.getCurrentUser().getUid();

        profileImage = view.findViewById(R.id.ivProfile);
        changeImage = view.findViewById(R.id.tvChangePhoto);
        uploadButton = view.findViewById(R.id.btnGuardar);
        etUsername = view.findViewById(R.id.etUsername);
        etDescription = view.findViewById(R.id.etDescripcion);

        loadUserData();

        profileImage.setOnClickListener(v -> requestStoragePermission());
        changeImage.setOnClickListener(v -> requestStoragePermission());
        uploadButton.setOnClickListener(v -> saveProfileChanges());

        return view;
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            openGallery(); // No se necesita permiso en Android 13+
        } else if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
        } else {
            openGallery();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.getData();
            getActivity().getContentResolver().takePersistableUriPermission(imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
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

        if (imageUri != null) {
            updates.put("photoUrl", imageUri.toString());
        }

        updateFirestoreData(updates);
    }

    private void updateFirestoreData(Map<String, Object> updates) {
        DocumentReference userRef = db.collection("users").document(userId);
        userRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();
                    navigateToPersonalFragment();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error al actualizar perfil", Toast.LENGTH_SHORT).show()
                );
    }

    private void navigateToPersonalFragment() {
        NavController navController = NavHostFragment.findNavController(ProfileFragment.this);
        navController.navigate(R.id.navigation_personal); // Usa directamente el ID del fragmento destino
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(getContext(), "Permiso denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
