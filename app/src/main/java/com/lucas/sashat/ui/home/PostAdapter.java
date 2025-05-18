package com.lucas.sashat.ui.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.Query;
import com.lucas.sashat.MainActivity;
import com.lucas.sashat.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private List<Post> posts;
    private Context context;
    private Fragment fragment;

    public PostAdapter(List<Post> posts, Context context, Fragment fragment) {
        this.posts = posts;
        this.context = context;
        this.fragment = fragment;
    }


    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = posts.get(position);
        String postId = post.getPostId();

        holder.postText.setText(post.getText());
        holder.genre.setText("Género: " + post.getGenre());

        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            holder.postImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(post.getImageUrl())
                    .placeholder(R.drawable.round_back_white10_20)
                    .into(holder.postImage);
        } else {
            holder.postImage.setVisibility(View.GONE);
        }
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getUid().equals(post.getUserId())) {
            holder.optionsButton.setVisibility(View.VISIBLE);
            holder.optionsButton.setOnClickListener(v -> showOptionsDialog(post));
        } else {
            holder.optionsButton.setVisibility(View.GONE);
        }
        if (fragment instanceof HomeFragment && post.getUserId().equals(currentUserId)) {
            holder.optionsButton.setVisibility(View.VISIBLE);
            holder.optionsButton.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("postId", post.getPostId());
                args.putString("text", post.getText());
                args.putString("genre", post.getGenre());
                args.putString("imageUrl", post.getImageUrl());
                NavController navController = NavHostFragment.findNavController(fragment);
                navController.navigate(R.id.createPostFragment, args);
            });
        } else {
            holder.optionsButton.setVisibility(View.GONE);
        }

        db.collection("users")
                .document(post.getUserId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        String photoUrl = documentSnapshot.getString("photoUrl");

                        holder.username.setText(username != null ? username : "Usuario");

                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(context)
                                    .load(photoUrl)
                                    .transform(new CircleCrop()) // Aplicar la transformación circular aquí
                                    .into(holder.userProfileImage);
                        } else {
                            holder.userProfileImage.setImageResource(R.drawable.boy); // Imagen por defecto
                        }
                    } else {
                        holder.username.setText("Usuario");
                        holder.userProfileImage.setImageResource(R.drawable.boy);
                    }
                })
                .addOnFailureListener(e -> {
                    holder.username.setText("Usuario");
                    holder.userProfileImage.setImageResource(R.drawable.boy);
                });

        holder.likeButton.setOnClickListener(v -> {
            String likePath = "likes/" + postId + "/users/" + currentUserId;
            db.document(likePath).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    db.document(likePath).delete().addOnSuccessListener(unused -> {
                        holder.likeButton.setImageResource(R.drawable.ic_heart); // Ícono de no likeado
                    });
                } else {
                    HashMap<String, Object> likeData = new HashMap<>();
                    likeData.put("timestamp", new Timestamp(new Date()));
                    db.document(likePath).set(likeData).addOnSuccessListener(unused -> {
                        holder.likeButton.setImageResource(R.drawable.heart); // Ícono de like
                    });
                }
            });
        });

        db.collection("likes")
                .document(postId)
                .collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        holder.likeButton.setImageResource(R.drawable.heart);
                    }
                });

        holder.bookmarkButton.setOnClickListener(v -> {
            String bookmarkPath = "bookmarks/" + currentUserId + "/posts/" + postId;
            db.document(bookmarkPath).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    db.document(bookmarkPath).delete().addOnSuccessListener(unused -> {
                        holder.bookmarkButton.setImageResource(R.drawable.ic_bookmark); // Icono no guardado
                    });
                } else {
                    HashMap<String, Object> bookmarkData = new HashMap<>();
                    bookmarkData.put("timestamp", new Timestamp(new Date()));
                    db.document(bookmarkPath).set(bookmarkData).addOnSuccessListener(unused -> {
                        holder.bookmarkButton.setImageResource(R.drawable.savebutton); // Icono guardado
                    });
                }
            });
        });

        db.collection("saved")
                .document(currentUserId)
                .collection("posts")
                .document(postId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        holder.bookmarkButton.setImageResource(R.drawable.savebutton);
                    }
                });

        holder.shareButton.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, post.getText());
            context.startActivity(Intent.createChooser(shareIntent, "Compartir post"));
        });

        holder.bookComment.setOnClickListener(v -> {
            showCommentDialog(holder.itemView.getContext(), postId);
        });

        if (fragment instanceof SavedPostsFragment) {
            // Oculta el botón de opciones completamente
            holder.optionsButton.setVisibility(View.GONE);
        } else if (fragment instanceof HomeFragment && post.getUserId().equals(currentUserId)) {
            holder.optionsButton.setVisibility(View.VISIBLE);
            holder.optionsButton.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("postId", post.getPostId());
                args.putString("text", post.getText());
                args.putString("genre", post.getGenre());
                args.putString("imageUrl", post.getImageUrl());
                NavController navController = NavHostFragment.findNavController(fragment);
                navController.navigate(R.id.action_nav_home_to_createPostFragment, args);
            });
        } else {
            holder.optionsButton.setVisibility(View.GONE);
        }

    }
    private void showOptionsDialog(Post post) {
        // Evitar que se muestre desde fragments no permitidos
        if (!(fragment instanceof HomeFragment)) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Opciones")
                .setItems(new CharSequence[]{"Editar", "Eliminar"}, (dialog, which) -> {
                    if (which == 0) {
                        Bundle bundle = new Bundle();
                        bundle.putString("postId", post.getPostId());
                        bundle.putString("text", post.getText());
                        bundle.putString("genre", post.getGenre());
                        bundle.putString("imageUrl", post.getImageUrl());

                        NavController navController = Navigation.findNavController(((Activity) context), R.id.nav_host_fragment_activity_main);
                        navController.navigate(R.id.createPostFragment, bundle);
                    } else if (which == 1) {
                        deletePost(post.getPostId());
                    }
                })
                .show();
    }

    private void confirmDelete(Post post) {
        new AlertDialog.Builder(context)
                .setTitle("Confirmar eliminación")
                .setMessage("¿Estás seguro de que deseas eliminar esta publicación?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    FirebaseFirestore.getInstance().collection("posts")
                            .document(post.getPostId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                int index = posts.indexOf(post);
                                if (index != -1) {
                                    posts.remove(index);
                                    notifyItemRemoved(index);
                                }
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(context, "Error al eliminar", Toast.LENGTH_SHORT).show()
                            );
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deletePost(String postId) {
        FirebaseFirestore.getInstance().collection("posts").document(postId)
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(context, "Publicación eliminada", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(context, "Error al eliminar publicación", Toast.LENGTH_SHORT).show());
    }

    private void showCommentDialog(Context context, String postId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_comments, null);
        RecyclerView commentsRecyclerView = dialogView.findViewById(R.id.commentsRecyclerView);
        EditText commentInput = dialogView.findViewById(R.id.commentInput);

        List<Comment> commentList = new ArrayList<>();
        CommentAdapter commentAdapter = new CommentAdapter(commentList, context);
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        commentsRecyclerView.setAdapter(commentAdapter);

        db.collection("posts")
                .document(postId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    commentList.clear();
                    for (DocumentSnapshot doc : snapshot) {
                        Comment comment = doc.toObject(Comment.class);
                        commentList.add(comment);
                    }
                    commentAdapter.notifyDataSetChanged();
                });

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Comentarios");
        builder.setView(dialogView);
        builder.setPositiveButton("Enviar", (dialog, which) -> {
            String commentText = commentInput.getText().toString().trim();
            if (!commentText.isEmpty()) {
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                String username = "Usuario"; // Reemplazar con nombre real si lo deseas
                Timestamp timestamp = new Timestamp(new Date());

                Comment newComment = new Comment(userId, username, commentText, timestamp);
                db.collection("posts")
                        .document(postId)
                        .collection("comments")
                        .add(newComment)
                        .addOnSuccessListener(ref -> {
                            commentList.add(newComment);
                            commentAdapter.notifyItemInserted(commentList.size() - 1);
                            Toast.makeText(context, "Comentario enviado", Toast.LENGTH_SHORT).show();
                        });
            }
        });

        builder.setNegativeButton("Cerrar", (dialog, which) -> dialog.dismiss());
        builder.show();
    }


    @Override
    public int getItemCount() {
        return posts.size();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView postImage, userProfileImage;
        TextView username, postText, genre;
        ImageView likeButton,shareButton, bookComment, bookmarkButton;
        Button optionsButton;
        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            postImage = itemView.findViewById(R.id.postImage);
            userProfileImage = itemView.findViewById(R.id.userProfileImage);
            username = itemView.findViewById(R.id.username);
            postText = itemView.findViewById(R.id.postContentText);
            genre = itemView.findViewById(R.id.postGenre);
            optionsButton = itemView.findViewById(R.id.optionsButton);
            likeButton = itemView.findViewById(R.id.likeButton);
            shareButton = itemView.findViewById(R.id.shareButton);
            bookComment = itemView.findViewById(R.id.bookComment);
            bookmarkButton = itemView.findViewById(R.id.bookmarkButton);
        }
    }
}
