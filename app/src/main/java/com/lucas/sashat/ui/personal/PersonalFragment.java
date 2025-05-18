package com.lucas.sashat.ui.personal;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lucas.sashat.BookListPagerAdapter;
import com.lucas.sashat.R;

import java.util.HashMap;

public class PersonalFragment extends Fragment {
    private static final String TAG = "PersonalFragment";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;
    private String viewedUserId;
    private TextView usernameTextView, bioTextView, followersCount, followingCount;
    private ImageView profileImageView, btnMenu;
    private Button followButton, editProfileButton;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    public PersonalFragment() {
        // Constructor vacío requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_personal, container, false);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        // Inicializar vistas
        btnMenu = view.findViewById(R.id.btnMenu);
        usernameTextView = view.findViewById(R.id.tvUsername);
        bioTextView = view.findViewById(R.id.tvDescripcion);
        followersCount = view.findViewById(R.id.tvNumSeguidores);
        profileImageView = view.findViewById(R.id.ivProfileImage);
        followButton = view.findViewById(R.id.btnFollow);
        editProfileButton = view.findViewById(R.id.btnEditar);
        followingCount = view.findViewById(R.id.tvNumFollowed);
        viewPager = view.findViewById(R.id.viewPager);
        tabLayout = view.findViewById(R.id.tabLayout);

        // Obtener viewedUserId desde argumentos o usar currentUserId
        viewedUserId = getArguments() != null ? getArguments().getString("userId") : currentUserId;

        // Configurar perfil y botones
        if (currentUserId == null) {
            Log.e(TAG, "Usuario no autenticado");
            Toast.makeText(getContext(), "Por favor, inicia sesión", Toast.LENGTH_SHORT).show();
            return view;
        }

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
                NavController navController = NavHostFragment.findNavController(PersonalFragment.this);
                navController.navigate(R.id.action_personalFragment_to_profileFragment);
            });
        }

        btnMenu.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(PersonalFragment.this);
            navController.navigate(R.id.settingsFragment);
        });

        // Configurar ViewPager2 para las listas de libros
        BookListPagerAdapter pagerAdapter = new BookListPagerAdapter(requireActivity(), viewedUserId);
        viewPager.setAdapter(pagerAdapter);

        // Vincular TabLayout con ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(R.string.read);
                    break;
                case 1:
                    tab.setText(R.string.reading);
                    break;
                case 2:
                    tab.setText(R.string.pending_read);
                    break;
            }
        }).attach();

        return view;
    }

    private void loadUserProfileRealtime() {
        DocumentReference userRef = db.collection("users").document(viewedUserId);

        userRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Error al obtener datos en tiempo real", e);
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                usernameTextView.setText(documentSnapshot.getString("username"));
                bioTextView.setText(documentSnapshot.getString("description"));
                followersCount.setText(String.valueOf(documentSnapshot.getLong("followersCount")));

                Glide.with(PersonalFragment.this)
                        .load(documentSnapshot.getString("photoUrl"))
                        .placeholder(R.drawable.ic_book_placeholder)
                        .error(R.drawable.ic_book_placeholder)
                        .into(profileImageView);
            } else {
                Log.d(TAG, "Documento de usuario no encontrado para: " + viewedUserId);
            }
        });
    }

    private void setupFollowersListener() {
        // Implementar si necesitas un listener en tiempo real para seguidores
    }

    private void loadFollowCounts() {
        // Cargar el número de seguidores
        db.collection("users").document(viewedUserId).collection("followers")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int followers = task.getResult().size();
                        followersCount.setText(String.valueOf(followers));
                    } else {
                        Log.e(TAG, "Error al cargar seguidores", task.getException());
                    }
                });

        // Cargar el número de seguidos
        db.collection("users").document(viewedUserId).collection("following")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int following = task.getResult().size();
                        followingCount.setText(String.valueOf(following));
                    } else {
                        Log.e(TAG, "Error al cargar seguidos", task.getException());
                    }
                });
    }

    private void checkFollowingStatus() {
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
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error al verificar estado de seguimiento", e));
    }

    private void handleFollowAction() {
        DocumentReference userRef = db.collection("users").document(currentUserId);

        userRef.collection("following").document(viewedUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        unfollowUser();
                    } else {
                        followUser();
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error al manejar acción de seguimiento", e));
    }

    private void followUser() {
        DocumentReference userRef = db.collection("users").document(currentUserId);
        userRef.collection("following").document(viewedUserId)
                .set(new HashMap<>())
                .addOnSuccessListener(aVoid -> {
                    followButton.setText("Dejar de seguir");
                    updateFollowersCount(viewedUserId, 1);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error al seguir usuario", e));
    }

    private void unfollowUser() {
        DocumentReference userRef = db.collection("users").document(currentUserId);
        userRef.collection("following").document(viewedUserId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    followButton.setText("Seguir");
                    updateFollowersCount(viewedUserId, -1);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error al dejar de seguir usuario", e));
    }

    private void updateFollowersCount(String userId, int change) {
        DocumentReference userRef = db.collection("users").document(userId);
        userRef.update("followersCount", FieldValue.increment(change))
                .addOnFailureListener(e -> Log.e(TAG, "Error al actualizar contador de seguidores", e));
    }
}