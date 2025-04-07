package com.lucas.sashat.ui.search;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.lucas.sashat.ApiBook;
import com.lucas.sashat.BookResponse;
import com.lucas.sashat.GoogleBooksApi;
import com.lucas.sashat.R;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment {
    private RecyclerView recyclerView;
    private SearchView searchView;
    private BookAdapter adapter;
    private List<ApiBook> bookList = new ArrayList<>();
    private int startIndex = 0;
    private boolean isLoading = false;
    private static final int INITIAL_LOAD_SIZE = 100;
    private static final int PAGE_SIZE = 20;
    private static final int MAX_RESULTS_PER_REQUEST = 40;
    private String currentQuery = "";
    private static final String API_KEY = "AIzaSyDXqqsDXqHbC8oc9l7q5KmKNscMJn0oN6s";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        searchView = view.findViewById(R.id.search_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BookAdapter(bookList);
        recyclerView.setAdapter(adapter);

        // Expandir el SearchView al cargar el fragmento
        searchView.setIconifiedByDefault(false); // Evita que se colapse al ícono
        searchView.setIconified(false); // Asegura que esté expandido

        // Cargar libros genéricos al abrir el fragmento
        loadInitialBooks();

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                int totalItemCount = layoutManager.getItemCount();
                int lastVisibleItem = layoutManager.findLastVisibleItemPosition();

                if (!isLoading && totalItemCount <= (lastVisibleItem + 5)) {
                    loadMoreBooks();
                }
            }
        });

        // Configurar el SearchView: solo buscar al presionar "buscar"
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // No hacer nada mientras el usuario escribe
                return true;
            }
        });

        return view;
    }

    private void loadInitialBooks() {
        isLoading = true;
        adapter.setLoading(true);
        startIndex = 0;
        bookList.clear();
        fetchInitialBooks(currentQuery);
    }

    private void loadMoreBooks() {
        if (isLoading) return;
        isLoading = true;
        adapter.setLoading(true);
        fetchBooks(currentQuery, startIndex, PAGE_SIZE);
    }

    private void performSearch(String queryText) {
        currentQuery = queryText.trim();
        Log.d("SearchFragment", "Búsqueda iniciada: " + currentQuery);
        bookList.clear();
        adapter.notifyDataSetChanged();
        loadInitialBooks();
    }

    private void fetchInitialBooks(String query) {
        long startTime = System.currentTimeMillis();
        List<Call<BookResponse>> calls = new ArrayList<>();

        int remainingBooks = INITIAL_LOAD_SIZE;
        int currentIndex = 0;

        while (remainingBooks > 0) {
            int requestSize = Math.min(remainingBooks, MAX_RESULTS_PER_REQUEST);
            calls.add(GoogleBooksApi.create().searchBooks(
                    query.isEmpty() ? "books" : query,
                    currentIndex,
                    requestSize,
                    API_KEY
            ));
            currentIndex += requestSize;
            remainingBooks -= requestSize;
        }

        fetchBooksInSequence(calls, startTime, true);
    }

    private void fetchBooks(String query, int startIndex, int maxResults) {
        long startTime = System.currentTimeMillis();
        GoogleBooksApi api = GoogleBooksApi.create();
        Call<BookResponse> call = api.searchBooks(
                query.isEmpty() ? "books" : query,
                startIndex,
                maxResults,
                API_KEY
        );

        call.enqueue(new Callback<BookResponse>() {
            @Override
            public void onResponse(Call<BookResponse> call, Response<BookResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ApiBook> newBooks = response.body().getItems();
                    bookList.addAll(newBooks);
                    adapter.notifyItemRangeInserted(bookList.size() - newBooks.size(), newBooks.size());
                    SearchFragment.this.startIndex += newBooks.size();
                }
                isLoading = false;
                adapter.setLoading(false);
                long endTime = System.currentTimeMillis();
                Log.d("SearchFragment", "Load time: " + (endTime - startTime) + "ms, Books loaded: " + bookList.size());
            }

            @Override
            public void onFailure(Call<BookResponse> call, Throwable t) {
                Log.e("SearchFragment", "Error loading books: " + t.getMessage());
                isLoading = false;
                adapter.setLoading(false);
            }
        });
    }

    private void fetchBooksInSequence(List<Call<BookResponse>> calls, long startTime, boolean isInitialLoad) {
        if (calls.isEmpty()) {
            isLoading = false;
            adapter.setLoading(false);
            long endTime = System.currentTimeMillis();
            Log.d("SearchFragment", "Initial load time: " + (endTime - startTime) + "ms, Books loaded: " + bookList.size());
            return;
        }

        Call<BookResponse> currentCall = calls.remove(0);
        currentCall.enqueue(new Callback<BookResponse>() {
            @Override
            public void onResponse(Call<BookResponse> call, Response<BookResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ApiBook> newBooks = response.body().getItems();
                    bookList.addAll(newBooks);
                    if (isInitialLoad) {
                        adapter.notifyDataSetChanged();
                    } else {
                        adapter.notifyItemRangeInserted(bookList.size() - newBooks.size(), newBooks.size());
                    }
                    SearchFragment.this.startIndex += newBooks.size();
                }
                fetchBooksInSequence(calls, startTime, isInitialLoad);
            }

            @Override
            public void onFailure(Call<BookResponse> call, Throwable t) {
                Log.e("SearchFragment", "Error in initial load: " + t.getMessage());
                fetchBooksInSequence(calls, startTime, isInitialLoad);
            }
        });
    }

    public class BookAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_BOOK = 0;
        private static final int TYPE_LOADING = 1;
        private List<ApiBook> books;
        private boolean showLoading = false;

        public BookAdapter(List<ApiBook> books) {
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

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
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
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof BookViewHolder) {
                ((BookViewHolder) holder).bind(books.get(position));
            }
        }

        @Override
        public int getItemCount() {
            return showLoading ? books.size() + 1 : books.size();
        }
    }

    public class BookViewHolder extends RecyclerView.ViewHolder {
        private ImageView bookImage;
        private TextView bookTitle;
        private TextView bookAuthor;
        private TextView bookGenre;
        private Button btnRead;
        private Button btnToRead;
        private Button btnReading;

        public BookViewHolder(View itemView) {
            super(itemView);
            bookImage = itemView.findViewById(R.id.book_image);
            bookTitle = itemView.findViewById(R.id.book_title);
            bookAuthor = itemView.findViewById(R.id.book_author);
            bookGenre = itemView.findViewById(R.id.book_genre);
            btnRead = itemView.findViewById(R.id.btn_read);
            btnToRead = itemView.findViewById(R.id.btn_to_read);
            btnReading = itemView.findViewById(R.id.btn_reading);
        }

        public void bind(ApiBook book) {
            bookTitle.setText(book.getVolumeInfo().getTitle());
            bookAuthor.setText(getString(R.string.book_author) + " " + String.join(", ", book.getVolumeInfo().getAuthors()));
            bookGenre.setText(getString(R.string.book_genre) + " " + String.join(", ", book.getVolumeInfo().getCategories()));

            String imageUrl = book.getVolumeInfo().getImageLinks() != null ?
                    (book.getVolumeInfo().getImageLinks().getSmallThumbnail() != null ?
                            book.getVolumeInfo().getImageLinks().getSmallThumbnail() :
                            book.getVolumeInfo().getImageLinks().getThumbnail()) : null;

            Log.d("SearchFragment", "Intentando cargar imagen para " + book.getVolumeInfo().getTitle() + ": " + imageUrl);

            Glide.with(itemView.getContext())
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(90, 100)
                    .placeholder(R.drawable.import_contacts_24px)
                    .error(R.drawable.import_contacts_24px)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e("SearchFragment", "Error cargando imagen: " + e.getMessage());
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            Log.d("SearchFragment", "Imagen cargada con éxito: " + model);
                            return false;
                        }
                    })
                    .into(bookImage);

            btnRead.setOnClickListener(v -> Log.d("SearchFragment", "Añadir a Leído: " + book.getVolumeInfo().getTitle()));
            btnToRead.setOnClickListener(v -> Log.d("SearchFragment", "Añadir a Por Leer: " + book.getVolumeInfo().getTitle()));
            btnReading.setOnClickListener(v -> Log.d("SearchFragment", "Añadir a Leyendo: " + book.getVolumeInfo().getTitle()));
        }
    }

    public static class LoadingViewHolder extends RecyclerView.ViewHolder {
        public LoadingViewHolder(View itemView) {
            super(itemView);
        }
    }
}