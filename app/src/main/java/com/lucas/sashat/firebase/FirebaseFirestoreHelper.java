package com.lucas.sashat.firebase;

import android.content.Context;
import com.lucas.sashat.R;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.*;
import java.util.HashMap;
import java.util.Map;

public class FirebaseFirestoreHelper {
    private Context context;

    public FirebaseFirestoreHelper(Context context, FirebaseFirestore db) {
        this.context = context;
        this.db = db;
    }
    private final FirebaseFirestore db;

    public FirebaseFirestoreHelper() {
        this.db = FirebaseFirestore.getInstance();
    }

    // --------------------
    // USUARIOS
    // --------------------

    public void addUser(String userId, String email, String username, String photoUrl, String description) {
        Map<String, Object> user = new HashMap<>();
        user.put("email", email);
        user.put("username", username);
        user.put("photoUrl", photoUrl); // La URL de la imagen
        user.put("description", description);
        user.put("followers", 0);
        user.put("following", 0);

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> System.out.println(context.getResources()
                        .getString(R.string.user_successfuly_added)))
                .addOnFailureListener(e -> System.err.println(context.getResources()
                        .getString(R.string.user_failed_to_add) + e.getMessage()));
    }


    public void getUser(String userId, OnSuccessListener<DocumentSnapshot> onSuccess, OnFailureListener onFailure) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    // --------------------
    // LIBROS
    // --------------------

    public void addBook(String userId, String bookId, String title, String author, String status) {
        Map<String, Object> book = new HashMap<>();
        book.put("title", title);
        book.put("author", author);
        book.put("status", status); // Ejemplo: "Leyendo", "Leído", "Quiero Leer"

        db.collection("users").document(userId)
                .collection("books").document(bookId)
                .set(book)
                .addOnSuccessListener(aVoid -> System.out.println(context.getResources().
                        getString(R.string.book_successfuly_added)))
                .addOnFailureListener(e -> System.err.println(context.getResources().
                        getString(R.string.book_failed_to_add) + e.getMessage()));
    }

    public void getUserBooks(String userId, OnSuccessListener<QuerySnapshot> onSuccess, OnFailureListener onFailure) {
        db.collection("users").document(userId)
                .collection("books")
                .get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    // --------------------
    // PUBLICACIONES
    // --------------------

    public void addPost(String userId, String postId, String content) {
        Map<String, Object> post = new HashMap<>();
        post.put("userId", userId);
        post.put("content", content);
        post.put("timestamp", FieldValue.serverTimestamp());

        db.collection("posts").document(postId)
                .set(post)
                .addOnSuccessListener(aVoid -> System.out.println(context.getResources().
                        getString(R.string.user_successfuly_added)))
                .addOnFailureListener(e -> System.err.println(context.getResources().
                        getString(R.string.user_successfuly_added) + e.getMessage()));
    }

    public void getAllPosts(OnSuccessListener<QuerySnapshot> onSuccess, OnFailureListener onFailure) {
        db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    // --------------------
    // COMENTARIOS
    // --------------------

    public void addComment(String postId, String userId, String commentText) {
        Map<String, Object> comment = new HashMap<>();
        comment.put("userId", userId);
        comment.put("commentText", commentText);
        comment.put("timestamp", FieldValue.serverTimestamp());

        db.collection("posts").document(postId)
                .collection("comments")
                .add(comment)
                .addOnSuccessListener(aVoid -> System.out.println(context.getResources()
                        .getString(R.string.comment_successfuly_added)))
                .addOnFailureListener(e -> System.err.println(context.getResources()
                        .getString(R.string.comment_failed_to_add) + e.getMessage()));
    }

    public void getComments(String postId, OnSuccessListener<QuerySnapshot> onSuccess, OnFailureListener onFailure) {
        db.collection("posts").document(postId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }
}
