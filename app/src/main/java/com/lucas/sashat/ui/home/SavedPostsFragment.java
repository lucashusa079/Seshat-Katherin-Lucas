package com.lucas.sashat.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lucas.sashat.R;

import java.util.ArrayList;
import java.util.List;

public class SavedPostsFragment extends Fragment {

    private RecyclerView recyclerView;
    private PostAdapter adapter;
    private List<Post> savedPosts = new ArrayList<>();
    private FirebaseFirestore db;
    private String userId;
    private TextView emptyMessage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_saved_posts, container, false);

        recyclerView = view.findViewById(R.id.savedPostsRecyclerView);
        emptyMessage = view.findViewById(R.id.emptyMessage);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PostAdapter(savedPosts, getContext(), this); // Pasa this como fragment también
        recyclerView.setAdapter(adapter);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();
        ImageButton buttonSettings = view.findViewById(R.id.buttonSettings);
        buttonSettings.setOnClickListener(v -> {
            // Navegar a SettingsFragment usando Navigation Component
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.settingsFragment);
        });

        loadSavedPosts();

        return view;
    }

    private void loadSavedPosts() {
        db.collection("saved")
                .document(userId)
                .collection("posts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    savedPosts.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Post post = doc.toObject(Post.class);
                        if (post != null) {
                            post.setBookmarked(true);
                            savedPosts.add(post);
                        }
                    }
                    adapter.notifyDataSetChanged();

                    if (savedPosts.isEmpty()) {
                        emptyMessage.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        emptyMessage.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                });
    }
}

