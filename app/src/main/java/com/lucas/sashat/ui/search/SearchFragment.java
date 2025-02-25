package com.lucas.sashat.ui.search;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lucas.sashat.Book;
import com.lucas.sashat.BookAdapter;
import com.lucas.sashat.R;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private RecyclerView recyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflar el layout correcto del Fragment
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        // Inicializar el RecyclerView usando la vista inflada
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Datos de ejemplo
        List<Book> bookList = new ArrayList<>();
        bookList.add(new Book("El Señor de los Anillos", "J.R.R. Tolkien", "Fantasía", R.drawable.import_contacts_24px));
        bookList.add(new Book("Cien Años de Soledad", "Gabriel García Márquez", "Realismo Mágico", R.drawable.import_contacts_24px));
        bookList.add(new Book("1984", "George Orwell", "Distopía", R.drawable.import_contacts_24px));

        // Vincular el adaptador al RecyclerView
        BookAdapter adapter = new BookAdapter(bookList);
        recyclerView.setAdapter(adapter);

        return view;
    }
    /*public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search, container, false);

        // Configurar el RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Datos de ejemplo
        List<Book> bookList = new ArrayList<>();
        bookList.add(new Book("El Señor de los Anillos", "J.R.R. Tolkien", "Fantasía", R.drawable.import_contacts_24px);
        bookList.add(new Book("Cien Años de Soledad", "Gabriel García Márquez", "Realismo Mágico", R.drawable.import_contacts_24px);
        bookList.add(new Book("1984", "George Orwell", "Distopía", R.drawable.import_contacts_24px));

        // Vincular el adaptador al RecyclerView
        BookAdapter adapter = new BookAdapter(bookList);
        recyclerView.setAdapter(adapter);
        /*
        searchInput = rootView.findViewById(R.id.search_input);
        recyclerView = rootView.findViewById(R.id.recycler_view);
        bookAdapter = new BookAdapter(getContext(), new ArrayList<>());
        recyclerView.setAdapter(bookAdapter);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                searchBooks(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        return rootView;
        */
    }

    private void searchBooks(String query) {
        // Aquí puedes hacer una consulta a Firestore para buscar los libros que coincidan con la query
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Books")
                .whereGreaterThanOrEqualTo("title", query)
                .whereLessThanOrEqualTo("title", query + '\uf8ff')
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Book> books = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Book book = document.toObject(Book.class);
                        books.add(book);
                    }
                    bookAdapter.updateBooks(books);
                })
                .addOnFailureListener(e -> Log.e("SearchFragment", "Error al buscar libros", e));
    }
}
