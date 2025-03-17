package com.lucas.sashat.ui.personal;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
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
    private TextView usernameTextView, bioTextView, followersTextView;
    private ImageView profileImageView;
    private Button followButton, editProfileButton;

    public PersonalFragment() {
        // Constructor vacío requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_personal, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();

        usernameTextView = view.findViewById(R.id.tvUsername);
        bioTextView = view.findViewById(R.id.tvDescripción);
        followersTextView = view.findViewById(R.id.tvNumSeguidores);
        profileImageView = view.findViewById(R.id.ivProfileImage);
        followButton = view.findViewById(R.id.btnFollow);
        editProfileButton = view.findViewById(R.id.btnEditar);

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


        return view;
    }

    public void loadUserProfile() {
        db.collection("users").document(viewedUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        usernameTextView.setText(documentSnapshot.getString("username"));
                        bioTextView.setText(documentSnapshot.getString("bio"));
                        String imageUrl = documentSnapshot.getString("profileImageUrl");
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(this).load(imageUrl).into(profileImageView);
                        }
                    }
                });
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
        });
    }

    private void updateFollowersCount(String userId, int delta) {
        DocumentReference userRef = db.collection("users").document(userId);
        userRef.update("followersCount", FieldValue.increment(delta));
    }

    private void setupFollowersListener() {
        db.collection("users").document(viewedUserId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e == null && documentSnapshot != null && documentSnapshot.exists()) {
                        Long followersCount = documentSnapshot.getLong("followersCount");
                        followersTextView.setText(followersCount != null ? followersCount + "" : "0");
                    }
                });
    }
}
