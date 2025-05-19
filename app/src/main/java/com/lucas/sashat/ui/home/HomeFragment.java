package com.lucas.sashat.ui.home;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.lucas.sashat.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomeFragment extends Fragment {

    private FirebaseFirestore db;
    private RecyclerView postsRecyclerView;
    private PostAdapter postAdapter;
    private List<Post> postList = new ArrayList<>();
    private ChipGroup genreChipGroup;
    private Set<String> selectedGenres = new HashSet<>();

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();

        postsRecyclerView = view.findViewById(R.id.postsRecyclerView);
        genreChipGroup = view.findViewById(R.id.genreChipGroup);

        postAdapter = new PostAdapter(postList, getContext(), this);
        postsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        postsRecyclerView.setAdapter(postAdapter);

        setupGenreChips();

        if (selectedGenres.isEmpty()) {
            loadAllPosts();
        } else {
            filterPostsByGenres(new ArrayList<>(selectedGenres));
        }

        return view;
    }

    private void setupGenreChips() {
        String[] genres = getResources().getStringArray(R.array.genres);

        Set<String> savedGenres = getContext()
                .getSharedPreferences("genre_prefs", Context.MODE_PRIVATE)
                .getStringSet("selected_genres", new HashSet<>());

        selectedGenres.clear();
        selectedGenres.addAll(savedGenres);

        for (String genre : genres) {
            Chip chip = new Chip(getContext());
            chip.setText(genre);
            chip.setCheckable(true);
            chip.setTextColor(getResources().getColor(R.color.textPublicacion));
            chip.setChipBackgroundColorResource(R.color.colorPublicacionFondo);
            chip.setChecked(savedGenres.contains(genre));

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedGenres.add(genre);
                } else {
                    selectedGenres.remove(genre);
                }

                saveSelectedGenres();

                if (selectedGenres.isEmpty()) {
                    loadAllPosts();
                } else {
                    filterPostsByGenres(new ArrayList<>(selectedGenres));
                }
            });

            genreChipGroup.addView(chip);
        }
    }

    private void saveSelectedGenres() {
        getContext()
                .getSharedPreferences("genre_prefs", Context.MODE_PRIVATE)
                .edit()
                .putStringSet("selected_genres", selectedGenres)
                .apply();
    }

    private void loadAllPosts() {
        db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    postList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Post post = doc.toObject(Post.class);
                        if (post != null) {
                            post.setPostId(doc.getId());
                            postList.add(post);
                        }
                    }
                    postAdapter.notifyDataSetChanged();
                });
    }

    private void filterPostsByGenres(List<String> genres) {
        postList.clear();
        postAdapter.notifyDataSetChanged();

        List<List<String>> genreChunks = new ArrayList<>();
        for (int i = 0; i < genres.size(); i += 10) {
            genreChunks.add(genres.subList(i, Math.min(i + 10, genres.size())));
        }

        final int[] completedQueries = {0};

        for (List<String> chunk : genreChunks) {
            db.collection("posts")
                    .whereIn("genre", chunk)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        for (DocumentSnapshot doc : snapshot) {
                            Post post = doc.toObject(Post.class);
                            if (post != null) {
                                post.setPostId(doc.getId());
                                if (!postList.contains(post)) {
                                    postList.add(post);
                                }
                            }
                        }

                        completedQueries[0]++;
                        if (completedQueries[0] == genreChunks.size()) {
                            postAdapter.notifyDataSetChanged();
                        }
                    })
                    .addOnFailureListener(e -> {
                        completedQueries[0]++;
                        if (completedQueries[0] == genreChunks.size()) {
                            postAdapter.notifyDataSetChanged();
                        }
                    });
        }
    }


    private void showCommentDialog(Context context, String postId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.comentarios);

        final EditText input = new EditText(context);
        input.setHint(R.string.escribe_un_comentario);
        builder.setView(input);

        builder.setPositiveButton(R.string.enviar, (dialog, which) -> {
            String commentText = input.getText().toString().trim();
            if (!commentText.isEmpty()) {
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                String username = "Usuario"; // Reemplaza esto con el username real
                Timestamp timestamp = new Timestamp(new Date());

                Comment comment = new Comment(userId, username, commentText, timestamp);

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("posts")
                        .document(postId)
                        .collection("comments")
                        .add(comment)
                        .addOnSuccessListener(docRef -> Toast.makeText(context, "Comentario enviado", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(context, "Error al comentar", Toast.LENGTH_SHORT).show());
            }
        });

        builder.setNegativeButton(R.string.cancelar, (dialog, which) -> dialog.cancel());

        builder.show();
    }

}

