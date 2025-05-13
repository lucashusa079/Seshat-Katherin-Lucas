package com.lucas.sashat.ui.home;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.Query;
import com.lucas.sashat.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private List<Post> posts;
    private Context context;

    public PostAdapter(List<Post> posts, Context context) {
        this.posts = posts;
        this.context = context;
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

        // Cargar imagen del contenido del post
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            holder.postImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(post.getImageUrl())
                    .placeholder(R.drawable.round_back_white10_20)
                    .into(holder.postImage);
        } else {
            holder.postImage.setVisibility(View.GONE);
        }

        // 🔥 Obtener y mostrar nombre de usuario y foto de perfil desde Firestore
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

        // Manejar clics en el botón de like
        holder.likeButton.setOnClickListener(v -> {
            String likePath = "likes/" + postId + "/users/" + currentUserId;
            db.document(likePath).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Ya le dio like → quitarlo
                    db.document(likePath).delete().addOnSuccessListener(unused -> {
                        holder.likeButton.setImageResource(R.drawable.ic_heart); // Ícono de no likeado
                        Toast.makeText(context, "Like eliminado", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    // No le dio like → agregarlo
                    HashMap<String, Object> likeData = new HashMap<>();
                    likeData.put("timestamp", new Timestamp(new Date()));
                    db.document(likePath).set(likeData).addOnSuccessListener(unused -> {
                        holder.likeButton.setImageResource(R.drawable.heart); // Ícono de like
                        Toast.makeText(context, "Te gustó este post", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        // ❤️ Like
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

        // Manejar clics en el botón de guardar (bookmark)
        holder.bookmarkButton.setOnClickListener(v -> {
            String bookmarkPath = "bookmarks/" + currentUserId + "/posts/" + postId;
            db.document(bookmarkPath).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Ya está guardado → quitar
                    db.document(bookmarkPath).delete().addOnSuccessListener(unused -> {
                        holder.bookmarkButton.setImageResource(R.drawable.ic_bookmark); // Icono no guardado
                        Toast.makeText(context, "Post eliminado de guardados", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    // No está guardado → guardar
                    HashMap<String, Object> bookmarkData = new HashMap<>();
                    bookmarkData.put("timestamp", new Timestamp(new Date()));
                    db.document(bookmarkPath).set(bookmarkData).addOnSuccessListener(unused -> {
                        holder.bookmarkButton.setImageResource(R.drawable.savebutton); // Icono guardado
                        Toast.makeText(context, "Post guardado", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        // 🔖 Bookmark
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

        // 📤 Compartir
        holder.shareButton.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, post.getText());
            context.startActivity(Intent.createChooser(shareIntent, "Compartir post"));
        });

        // 💬 Comentarios
        holder.bookComment.setOnClickListener(v -> {
            showCommentDialog(holder.itemView.getContext(), postId);
        });
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

        // Cargar comentarios existentes
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

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            postImage = itemView.findViewById(R.id.postImage);
            userProfileImage = itemView.findViewById(R.id.userProfileImage);
            username = itemView.findViewById(R.id.username);
            postText = itemView.findViewById(R.id.postContentText);
            genre = itemView.findViewById(R.id.postGenre);

            likeButton = itemView.findViewById(R.id.likeButton);
            shareButton = itemView.findViewById(R.id.shareButton);
            bookComment = itemView.findViewById(R.id.bookComment);
            bookmarkButton = itemView.findViewById(R.id.bookmarkButton);
        }
    }

}
