package com.lucas.sashat.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lucas.sashat.R;

import java.util.ArrayList;
import java.util.List;

public class SavedPostsFragment extends Fragment {
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private RecyclerView recyclerView;
    private PostAdapter adapter;
    private List<Post> savedPosts = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_saved_posts, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        recyclerView = view.findViewById(R.id.savedPostsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PostAdapter(savedPosts, getContext(), this);
        recyclerView.setAdapter(adapter);

        loadSavedPosts();

        return view;
    }

    private void loadSavedPosts() {
        String userId = auth.getCurrentUser().getUid();

        db.collection("bookmarks")
                .document(userId)
                .collection("posts")
                .get()
                .addOnSuccessListener(bookmarkSnapshots -> {
                    savedPosts.clear();
                    for (DocumentSnapshot bookmarkDoc : bookmarkSnapshots) {
                        String postId = bookmarkDoc.getId();
                        db.collection("posts")
                                .document(postId)
                                .get()
                                .addOnSuccessListener(postDoc -> {
                                    if (postDoc.exists()) {
                                        Post post = postDoc.toObject(Post.class);
                                        post.setPostId(postId); // Asegura que el ID esté seteado
                                        savedPosts.add(post);
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                    }
                });
    }
}
