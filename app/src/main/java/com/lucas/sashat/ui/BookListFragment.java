package com.lucas.sashat.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.lucas.sashat.Book;
import com.lucas.sashat.BookDetailsBottomSheet;
import com.lucas.sashat.BookGridAdapter;
import com.lucas.sashat.R;

import java.util.ArrayList;
import java.util.List;

public class BookListFragment extends Fragment {
    private static final String TAG = "BookListFragment";
    private static final String ARG_LIST_TYPE = "list_type";
    private static final String ARG_USER_ID = "viewedUserId";
    private String listType;
    private String viewedUserId;
    private RecyclerView recyclerView;
    private TextView tvEmptyList;
    private BookGridAdapter adapter;
    private List<Book> books;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public static BookListFragment newInstance(String listType, String viewedUserId) {
        BookListFragment fragment = new BookListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_LIST_TYPE, listType);
        args.putString(ARG_USER_ID, viewedUserId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            listType = getArguments().getString(ARG_LIST_TYPE);
            viewedUserId = getArguments().getString(ARG_USER_ID);
            Log.d(TAG, "Inicializando con listType: " + listType + ", viewedUserId: " + viewedUserId);
        }
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "Creando vista para " + listType);
        View view = inflater.inflate(R.layout.fragment_book_list, container, false);
        recyclerView = view.findViewById(R.id.recycler_view_books);
        tvEmptyList = view.findViewById(R.id.tv_empty_list);
        if (recyclerView == null || tvEmptyList == null) {
            Log.e(TAG, "Error: recyclerView o tvEmptyList es nulo");
        }
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Configurar RecyclerView
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        books = new ArrayList<>();
        adapter = new BookGridAdapter(getContext(), books, book -> {
            showBookDetailsDialog(book);
        });
        recyclerView.setAdapter(adapter);
        Log.d(TAG, "RecyclerView configurado para " + listType);

        // Cargar libros
        loadBooks();
    }

    private void loadBooks() {
        if (viewedUserId == null) {
            Log.e(TAG, "viewedUserId es nulo");
            Toast.makeText(getContext(), "Error: usuario no especificado", Toast.LENGTH_SHORT).show();
            return;
        }

        String collectionPath;
        switch (listType) {
            case "Leído":
                collectionPath = "read";
                break;
            case "En lectura":
                collectionPath = "reading";
                break;
            case "Pendientes":
                collectionPath = "to_read";
                break;
            default:
                Log.e(TAG, "Tipo de lista desconocido: " + listType);
                return;
        }

        Log.d(TAG, "Cargando libros para userId: " + viewedUserId + ", colección: " + collectionPath);

        db.collection("users")
                .document(viewedUserId)
                .collection(collectionPath)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error al cargar libros", e);
                        Toast.makeText(getContext(), "Error al cargar libros: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<Book> bookList = new ArrayList<>();
                    if (queryDocumentSnapshots != null) {
                        Log.d(TAG, "Documentos encontrados: " + queryDocumentSnapshots.size());
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            try {
                                Book book = document.toObject(Book.class);
                                bookList.add(book);
                                Log.d(TAG, "Libro cargado: " + book.getTitle() + ", imageUri: " + book.getCoverImage());
                            } catch (Exception ex) {
                                Log.e(TAG, "Error al mapear documento: " + document.getId(), ex);
                            }
                        }
                    } else {
                        Log.w(TAG, "queryDocumentSnapshots es nulo");
                    }
                    Log.d(TAG, "Total libros cargados: " + bookList.size());
                    adapter.updateBooks(bookList);
                    tvEmptyList.setVisibility(bookList.isEmpty() ? View.VISIBLE : View.GONE);
                    recyclerView.setVisibility(bookList.isEmpty() ? View.GONE : View.VISIBLE);
                    Log.d(TAG, "RecyclerView visibility: " + (recyclerView.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
                });
    }

    private void showBookDetailsDialog(Book book) {
        BookDetailsBottomSheet dialog = BookDetailsBottomSheet.newInstance(book, viewedUserId, listType);
        dialog.show(getParentFragmentManager(), "BookDetailsBottomSheet");
    }
}