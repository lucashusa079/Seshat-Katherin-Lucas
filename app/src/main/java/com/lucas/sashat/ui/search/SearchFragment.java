package com.lucas.sashat.ui.search;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
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
    private SearchView searchView;
    private BookAdapter adapter;
    private List<Book> bookList = new ArrayList<>();
    private FirebaseFirestore db;
    private DocumentSnapshot lastVisible;
    private boolean isLoading = false;
    private static final int PAGE_SIZE = 10; // Reducido para carga más rápida
    private String currentQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        searchView = view.findViewById(R.id.search_view);
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

                if (!isLoading && totalItemCount <= (lastVisibleItem + 3)) { // Umbral bajo para precargar
                    loadMoreBooks();
                }
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                performSearch(newText);
                return true;
            }
        });

        return view;
    }

    private void loadInitialBooks() {
        isLoading = true;
        adapter.setLoading(true);
        Query query = buildQuery(currentQuery, null);

        long startTime = System.currentTimeMillis();
        query.get(Source.CACHE).addOnSuccessListener(cachedSnapshots -> {
            processQueryResults(cachedSnapshots, startTime);
            // Actualizar desde el servidor en segundo plano
            query.get(Source.SERVER).addOnSuccessListener(serverSnapshots -> {
                updateQueryResults(serverSnapshots, startTime);
            });
        }).addOnFailureListener(e -> {
            Log.e("SearchFragment", "Error loading from cache", e);
            query.get(Source.SERVER).addOnSuccessListener(serverSnapshots -> {
                processQueryResults(serverSnapshots, startTime);
            }).addOnFailureListener(e2 -> {
                Log.e("SearchFragment", "Error loading from server", e2);
                adapter.setLoading(false);
                isLoading = false;
            });
        });
    }

    private void loadMoreBooks() {
        if (lastVisible == null || isLoading) return;

        isLoading = true;
        adapter.setLoading(true);
        Query query = buildQuery(currentQuery, lastVisible);

        long startTime = System.currentTimeMillis();
        query.get(Source.CACHE).addOnSuccessListener(cachedSnapshots -> {
            processQueryResults(cachedSnapshots, startTime);
            query.get(Source.SERVER).addOnSuccessListener(serverSnapshots -> {
                updateQueryResults(serverSnapshots, startTime);
            });
        }).addOnFailureListener(e -> {
            query.get(Source.SERVER).addOnSuccessListener(serverSnapshots -> {
                processQueryResults(serverSnapshots, startTime);
            }).addOnFailureListener(e2 -> {
                Log.e("SearchFragment", "Error loading more books", e2);
                adapter.setLoading(false);
                isLoading = false;
            });
        });
    }

    private Query buildQuery(String searchText, DocumentSnapshot startAfter) {
        Query query = db.collection("books")
                .orderBy("title")
                .limit(PAGE_SIZE); // Ordenar por title sin select()
        if (startAfter != null) {
            query = query.startAfter(startAfter);
        }
        return query;
    }

    private void performSearch(String queryText) {
        currentQuery = queryText.trim();
        bookList.clear();
        lastVisible = null;
        adapter.notifyDataSetChanged();
        loadInitialBooks();
    }

    private void processQueryResults(QuerySnapshot queryDocumentSnapshots, long startTime) {
        int startPosition = bookList.size();
        String searchLower = currentQuery.toLowerCase();

        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
            Book book = document.toObject(Book.class);
            String titleLower = book.getTitle().toLowerCase();
            String authorLower = book.getAuthor().toLowerCase();

            if (searchLower.isEmpty() ||
                    titleLower.contains(searchLower) ||
                    authorLower.contains(searchLower)) {
                bookList.add(book);
            }
        }

        if (!queryDocumentSnapshots.isEmpty()) {
            lastVisible = queryDocumentSnapshots.getDocuments()
                    .get(queryDocumentSnapshots.size() - 1);
        }

        adapter.notifyItemRangeInserted(startPosition, bookList.size() - startPosition);
        adapter.setLoading(false);
        isLoading = false;
        long endTime = System.currentTimeMillis();
        Log.d("SearchFragment", "Load time: " + (endTime - startTime) + "ms, Books found: " + bookList.size());
    }

    private void updateQueryResults(QuerySnapshot queryDocumentSnapshots, long startTime) {
        bookList.clear();
        processQueryResults(queryDocumentSnapshots, startTime);
    }

    // Adaptador personalizado
    public static class BookAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_BOOK = 0;
        private static final int TYPE_LOADING = 1;
        private List<Book> books;
        private boolean showLoading = false;

        public BookAdapter(List<Book> books) {
            this.books = books;
        }

        public void setLoading(boolean loading) {
            showLoading = loading;
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            if (position == books.size() && showLoading) {
                return TYPE_LOADING;
            }
            return TYPE_BOOK;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_BOOK) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_book_search, parent, false);
                return new BookViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_loading, parent, false);
                return new LoadingViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof BookViewHolder) {
                ((BookViewHolder) holder).bind(books.get(position));
            }
        }

        @Override
        public int getItemCount() {
            return showLoading ? books.size() + 1 : books.size();
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
            String author = book.getAuthor();
            if (author != null && !author.isEmpty()) {
                bookAuthor.setText(author);
            } else {
                bookAuthor.setText("");
            }
            bookGenre.setText(book.getGenre() != null ? String.join(", ", book.getGenre()) : "");
            Glide.with(itemView.getContext())
                    .load(book.getCoverImage())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(100, 100)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(bookImage);
        }
    }

    public static class LoadingViewHolder extends RecyclerView.ViewHolder {
        public LoadingViewHolder(View itemView) {
            super(itemView);
        }
    }
}