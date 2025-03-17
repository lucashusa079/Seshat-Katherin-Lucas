package com.lucas.sashat.ui.search;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.lucas.sashat.Book;
import com.lucas.sashat.R;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {
    private RecyclerView recyclerView;
    private BookAdapter adapter; // Adaptador personalizado
    private List<Book> bookList = new ArrayList<>();
    private FirebaseFirestore db;
    private DocumentSnapshot lastVisible;
    private boolean isLoading = false;
    private static final int PAGE_SIZE = 20;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BookAdapter(bookList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        loadInitialBooks();

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                int totalItemCount = layoutManager.getItemCount();
                int lastVisibleItem = layoutManager.findLastVisibleItemPosition();

                if (!isLoading && totalItemCount <= (lastVisibleItem + 10)) {
                    loadMoreBooks();
                }
            }
        });

        return view;
    }

    private void loadInitialBooks() {
        isLoading = true;
        Query query = db.collection("books")
                .orderBy("title")
                .limit(PAGE_SIZE);

        query.get(Source.CACHE).addOnSuccessListener(queryDocumentSnapshots -> {
            if (queryDocumentSnapshots.isEmpty()) {
                // Si no hay datos en caché, consulta al servidor
                query.get(Source.SERVER).addOnSuccessListener(serverSnapshots -> {
                    processQueryResults(serverSnapshots);
                });
            } else {
                processQueryResults(queryDocumentSnapshots);
            }
        }).addOnFailureListener(e -> {
            Log.e("SearchFragment", "Error loading books", e);
            isLoading = false;
        });
    }

    private void loadMoreBooks() {
        if (lastVisible == null || isLoading) return;

        isLoading = true;
        Query query = db.collection("books")
                .orderBy("title")
                .startAfter(lastVisible)
                .limit(PAGE_SIZE);

        query.get(Source.CACHE).addOnSuccessListener(queryDocumentSnapshots -> {
            if (queryDocumentSnapshots.isEmpty()) {
                query.get(Source.SERVER).addOnSuccessListener(serverSnapshots -> {
                    processQueryResults(serverSnapshots);
                });
            } else {
                processQueryResults(queryDocumentSnapshots);
            }
        }).addOnFailureListener(e -> {
            Log.e("SearchFragment", "Error loading more books", e);
            isLoading = false;
        });
    }

    private void processQueryResults(QuerySnapshot queryDocumentSnapshots) {
        int startPosition = bookList.size();
        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
            bookList.add(document.toObject(Book.class));
        }
        if (!queryDocumentSnapshots.isEmpty()) {
            lastVisible = queryDocumentSnapshots.getDocuments()
                    .get(queryDocumentSnapshots.size() - 1);
        }
        adapter.notifyItemRangeInserted(startPosition, queryDocumentSnapshots.size());
        isLoading = false;
    }

    // Adaptador personalizado
    public static class BookAdapter extends RecyclerView.Adapter<BookViewHolder> {
        private List<Book> books;

        public BookAdapter(List<Book> books) {
            this.books = books;
        }

        @NonNull
        @Override
        public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_book_search, parent, false);
            return new BookViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
            holder.bind(books.get(position));
        }

        @Override
        public int getItemCount() {
            return books.size();
        }
    }

    public static class BookViewHolder extends RecyclerView.ViewHolder {
        private ImageView bookImage;
        private TextView bookTitle;
        private TextView bookAuthor;
        private TextView bookGenre;

        public BookViewHolder(View itemView) {
            super(itemView);
            bookImage = itemView.findViewById(R.id.book_image);
            bookTitle = itemView.findViewById(R.id.book_title);
            bookAuthor = itemView.findViewById(R.id.book_author);
            bookGenre = itemView.findViewById(R.id.book_genre);
        }

        public void bind(Book book) {
            bookTitle.setText(book.getTitle());
            bookAuthor.setText(book.getAuthor());
            bookGenre.setText(String.join(", ", book.getGenre()));
            Glide.with(itemView.getContext())
                    .load(book.getCoverImage())
                    .into(bookImage);
        }
    }
}