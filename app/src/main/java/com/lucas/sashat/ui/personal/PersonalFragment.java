package com.lucas.sashat.ui.personal;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lucas.sashat.R;

import java.util.HashMap;

public class PersonalFragment extends Fragment {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;
    private String viewedUserId;
    private TextView usernameTextView, bioTextView, followersTextView, followingTextView;
    private ImageView profileImageView;
    private Button followButton, editProfileButton, btnMenu;

    public PersonalFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_personal, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();
        btnMenu = view.findViewById(R.id.btnMenu);
        usernameTextView = view.findViewById(R.id.tvUsername);
        bioTextView = view.findViewById(R.id.tvDescripción);
        followersTextView = view.findViewById(R.id.tvNumSeguidores);
        profileImageView = view.findViewById(R.id.ivProfileImage);
        followButton = view.findViewById(R.id.btnFollow);
        editProfileButton = view.findViewById(R.id.btnEditar);
        followingTextView = view.findViewById(R.id.tvNumFollowed);

        viewedUserId = getArguments() != null ? getArguments().getString("userId") : currentUserId;

        loadUserProfile();
        setupFollowersListener();

        if (!viewedUserId.equals(currentUserId)) {
            checkFollowingStatus();
            followButton.setOnClickListener(v -> handleFollowAction());
            followButton.setVisibility(View.VISIBLE);
            editProfileButton.setVisibility(View.GONE);
        } else {
            editProfileButton.setVisibility(View.VISIBLE);
            followButton.setVisibility(View.GONE);
            editProfileButton.setOnClickListener(v -> {
                // Usar getNavController() para obtener el controlador de navegación
                NavController navController = NavHostFragment.findNavController(PersonalFragment.this);
                navController.navigate(R.id.action_personalFragment_to_profileFragment);
            });
        }
        btnMenu.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(PersonalFragment.this);
            navController.navigate(R.id.settingsFragment);
        });

        return view;
    }


    public void loadUserProfile() {
        if (getArguments() != null) {
            usernameTextView.setText(getArguments().getString("username"));
            bioTextView.setText(getArguments().getString("description"));
        }

        db.collection("users").document(viewedUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String imageUriString = documentSnapshot.getString("profileImageUri");
                        if (imageUriString != null && !imageUriString.isEmpty()) {
                            Glide.with(this).load(Uri.parse(imageUriString)).into(profileImageView);
                        }
                    }
                });

    }

    private void saveImageUriToFirestore(Uri imageUri) {
        FirebaseFirestore.getInstance().collection("users")
                .document(currentUserId)
                .update("profileImageUri", imageUri.toString())
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "URI de imagen guardada"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error al guardar URI", e));
    }

    private void checkFollowingStatus() {
        db.collection("users").document(currentUserId)
                .collection("following").document(viewedUserId)
                .get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        followButton.setText("Siguiendo");
                    } else {
                        followButton.setText("Seguir");
                    }
                });
    }

    private void handleFollowAction() {
        DocumentReference followingRef = db.collection("users").document(currentUserId)
                .collection("following").document(viewedUserId);
        DocumentReference followerRef = db.collection("users").document(viewedUserId)
                .collection("followers").document(currentUserId);

        followingRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Dejar de seguir
                followingRef.delete();
                followerRef.delete();
                updateFollowersCount(viewedUserId, -1);
                followButton.setText("Seguir");
            } else {
                // Seguir
                followingRef.set(new HashMap<>());
                followerRef.set(new HashMap<>());
                updateFollowersCount(viewedUserId, 1);
                followButton.setText("Siguiendo");
            }

            // 🔹 Recargar perfil para actualizar UI
            loadUserProfile();
        });
    }

    private void setupFollowersListener() {
        DocumentReference userRef = db.collection("users").document(viewedUserId);

        userRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                Log.e("Firestore", "Error al obtener seguidores", e);
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                // 🔹 Actualizar followers
                Long followersCount = documentSnapshot.contains("followersCount")
                        ? documentSnapshot.getLong("followersCount")
                        : 0;
                followersTextView.setText(String.valueOf(followersCount));

                // 🔹 Actualizar following
                Long followingCount = documentSnapshot.contains("followingCount")
                        ? documentSnapshot.getLong("followingCount")
                        : 0;
                followingTextView.setText(String.valueOf(followingCount));

                Log.d("Firestore", "Seguidores: " + followersCount + " - Siguiendo: " + followingCount);
            }
        });
    }


    private void updateFollowersCount(String userId, int delta) {
        DocumentReference userRef = db.collection("users").document(userId);
        userRef.update("followersCount", FieldValue.increment(delta))
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "FollowersCount actualizado en Firestore"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error actualizando followersCount", e));

        // 🔹 También actualizar "followingCount" del usuario actual
        if (userId.equals(viewedUserId)) {
            db.collection("users").document(currentUserId)
                    .update("followingCount", FieldValue.increment(delta))
                    .addOnSuccessListener(aVoid -> Log.d("Firestore", "FollowingCount actualizado en Firestore"))
                    .addOnFailureListener(e -> Log.e("Firestore", "Error actualizando followingCount", e));
        }
    }


}
