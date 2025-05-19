package com.lucas.sashat.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.lucas.sashat.R;
import com.squareup.picasso.Picasso;

import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
    private List<Comment> commentList;
    private Context context;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public CommentAdapter(List<Comment> commentList, Context context) {
        this.commentList = commentList;
        this.context = context;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        holder.commentText.setText(comment.getText());

        db.collection("users")
                .document(comment.getUserId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        String photoUrl = documentSnapshot.getString("photoUrl");

                        holder.username.setText(username != null ? username : "Usuario");

                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Picasso.get().load(photoUrl).into(holder.userProfileImage);
                        } else {
                            holder.userProfileImage.setImageResource(R.drawable.boy);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    holder.username.setText("Usuario");
                    holder.userProfileImage.setImageResource(R.drawable.boy);
                });
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView username, commentText;
        ImageView userProfileImage;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.commentUsername);
            commentText = itemView.findViewById(R.id.commentText);
            userProfileImage = itemView.findViewById(R.id.commentUserImage);
        }
    }
}
