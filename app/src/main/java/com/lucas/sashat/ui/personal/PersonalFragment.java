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
    private TextView usernameTextView, bioTextView, followersCount, followingCount;
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
        followersCount = view.findViewById(R.id.tvNumSeguidores);
        profileImageView = view.findViewById(R.id.ivProfileImage);
        followButton = view.findViewById(R.id.btnFollow);
        editProfileButton = view.findViewById(R.id.btnEditar);
        followingCount = view.findViewById(R.id.tvNumFollowed);

        viewedUserId = getArguments() != null ? getArguments().getString("userId") : currentUserId;

        loadUserProfileRealtime();
        setupFollowersListener();
        loadFollowCounts();
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

    public void loadUserProfileRealtime() {
        DocumentReference userRef = db.collection("users").document(viewedUserId);

        userRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                Log.e("Firestore", "Error al obtener datos en tiempo real", e);
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                usernameTextView.setText(documentSnapshot.getString("username"));
                bioTextView.setText(documentSnapshot.getString("description"));
                followersCount.setText(String.valueOf(documentSnapshot.getLong("followersCount")));

                Glide.with(PersonalFragment.this)
                        .load(documentSnapshot.getString("photoUrl"))
                        .into(profileImageView);
            }
        });
    }

    public void setupFollowersListener() {
        // Configurar el listener para los seguidores (si es necesario)
    }

    private void loadFollowCounts() {
        // Cargar el número de seguidores
        db.collection("users").document(viewedUserId).collection("followers")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int followers = task.getResult().size();
                        followersCount.setText(String.valueOf(followers));
                    }
                });

        // Cargar el número de seguidos
        db.collection("users").document(viewedUserId).collection("following")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int following = task.getResult().size();
                        followingCount.setText(String.valueOf(following));
                    }
                });
    }

    public void checkFollowingStatus() {
        db.collection("users")
                .document(currentUserId)
                .collection("following")
                .document(viewedUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        followButton.setText("Dejar de seguir");
                    } else {
                        followButton.setText("Seguir");
                    }
                });
    }

    public void handleFollowAction() {
        DocumentReference userRef = db.collection("users").document(currentUserId);

        userRef.collection("following").document(viewedUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        unfollowUser();
                    } else {
                        followUser();
                    }
                });
    }

    public void followUser() {
        DocumentReference userRef = db.collection("users").document(currentUserId);
        userRef.collection("following").document(viewedUserId)
                .set(new HashMap<>())
                .addOnSuccessListener(aVoid -> {
                    followButton.setText("Dejar de seguir");
                    updateFollowersCount(viewedUserId, 1);
                });
    }

    public void unfollowUser() {
        DocumentReference userRef = db.collection("users").document(currentUserId);
        userRef.collection("following").document(viewedUserId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    followButton.setText("Seguir");
                    updateFollowersCount(viewedUserId, -1);
                });
    }

    public void updateFollowersCount(String userId, int change) {
        DocumentReference userRef = db.collection("users").document(userId);
        userRef.update("followersCount", FieldValue.increment(change));
    }
}
