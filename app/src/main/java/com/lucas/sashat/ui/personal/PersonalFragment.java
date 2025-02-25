package com.lucas.sashat.ui.personal;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lucas.sashat.Book;
import com.lucas.sashat.BookAdapter;
import com.lucas.sashat.R;
import com.lucas.sashat.databinding.FragmentNotificationsBinding;

import java.util.ArrayList;
import java.util.List;

public class PersonalFragment extends Fragment {
    private TextView usernameTextView, bioTextView;
    private RecyclerView booksRecyclerView;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_personal, container, false);

        usernameTextView = rootView.findViewById(R.id.username_text_view);
        bioTextView = rootView.findViewById(R.id.bio_text_view);
        booksRecyclerView = rootView.findViewById(R.id.books_recycler_view);

        // Aquí mostrarías la información personal del usuario
        loadUserData();

        return rootView;
    }

    private void loadUserData() {
        // Obtener datos del usuario de Firestore (su nombre, bio, libros leídos, etc.)
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("Users").document(userID).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String username = documentSnapshot.getString("username");
                    String bio = documentSnapshot.getString("bio");

                    usernameTextView.setText(username);
                    bioTextView.setText(bio);

                    // Cargar los libros del usuario (leídos, en lectura, por leer)
                    loadBooks(userID);
                })
                .addOnFailureListener(e -> Log.e("PersonalFragment", "Error al cargar los datos del usuario", e));
    }

    private void loadBooks(String userID) {
        // Aquí puedes cargar los libros del usuario de Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Ejemplo para cargar libros leídos
        db.collection("Users").document(userID)
                .collection("books")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Book> books = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Book book = document.toObject(Book.class);
                        books.add(book);
                    }

                    // Actualizar el RecyclerView con los libros
                    booksRecyclerView.setAdapter(new BookAdapter(getContext(), books));
                })
                .addOnFailureListener(e -> Log.e("PersonalFragment", "Error al cargar los libros", e));
    }
}
