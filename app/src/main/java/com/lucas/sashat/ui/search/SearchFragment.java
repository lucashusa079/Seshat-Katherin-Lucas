package com.lucas.sashat.ui.search;

import androidx.annotation.NonNull;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lucas.sashat.ApiBook;
import com.lucas.sashat.BookResponse;
import com.lucas.sashat.GoogleBooksApi;
import com.lucas.sashat.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private Map<String, Set<String>> bookListStatus;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        searchView = view.findViewById(R.id.search_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BookAdapter(bookList);
        recyclerView.setAdapter(adapter);

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        bookListStatus = new HashMap<>();

        // Expandir el SearchView al cargar el fragmento
        searchView.setIconifiedByDefault(false);
        searchView.setIconified(false);

        // Aplicar el drawable personalizado al SearchView
        searchView.setBackgroundResource(R.drawable.rounded_search_background);

        // Quitar la línea de guía del search_plate
        int searchPlateId = searchView.getContext().getResources().getIdentifier("android:id/search_plate", null, null);
        View searchPlate = searchView.findViewById(searchPlateId);
        if (searchPlate != null) {
            searchPlate.setBackgroundResource(android.R.color.transparent);
            Log.d("SearchFragment", "Línea de guía de search_plate eliminada");
        } else {
            Log.w("SearchFragment", "No se encontró search_plate");
        }

        // Asegurarse de que el EditText no tenga una línea adicional
        int searchEditTextId = searchView.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);
        EditText searchEditText = searchView.findViewById(searchEditTextId);
        if (searchEditText != null) {
            searchEditText.setBackground(null);
            Log.d("SearchFragment", "Fondo de search_src_text eliminado");
        } else {
            Log.w("SearchFragment", "No se encontró search_src_text");
        }

        // Cargar libros genéricos al abrir el fragmento
        loadInitialBooks();

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                int totalItemCount = layoutManager.getItemCount();
                int lastVisibleItem = layoutManager.findLastVisibleItemPosition();

                if (!isLoading && totalItemCount <= (lastVisibleItem + 5)) {
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
        bookListStatus.clear();
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
        bookListStatus.clear();
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
            public void onResponse(@NonNull Call<BookResponse> call, @NonNull Response<BookResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ApiBook> newBooks = response.body().getItems();
                    for (ApiBook book : newBooks) {
                        bookList.add(book);
                        bookListStatus.put(book.getId(), new HashSet<>());
                        checkBookInLists(book.getId());
                    }
                    adapter.notifyItemRangeInserted(bookList.size() - newBooks.size(), newBooks.size());
                    SearchFragment.this.startIndex += newBooks.size();
                }
                isLoading = false;
                adapter.setLoading(false);
                long endTime = System.currentTimeMillis();
                Log.d("SearchFragment", "Load time: " + (endTime - startTime) + "ms, Books loaded: " + bookList.size());
            }

            @Override
            public void onFailure(@NonNull Call<BookResponse> call, @NonNull Throwable t) {
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
            public void onResponse(@NonNull Call<BookResponse> call, @NonNull Response<BookResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ApiBook> newBooks = response.body().getItems();
                    for (ApiBook book : newBooks) {
                        bookList.add(book);
                        bookListStatus.put(book.getId(), new HashSet<>());
                        checkBookInLists(book.getId());
                    }
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
            public void onFailure(@NonNull Call<BookResponse> call, @NonNull Throwable t) {
                Log.e("SearchFragment", "Error in initial load: " + t.getMessage());
                fetchBooksInSequence(calls, startTime, isInitialLoad);
            }
        });
    }

    // Verificar si un libro está en alguna lista
    private void checkBookInLists(String bookId) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        String[] lists = {"read", "to_read", "reading"};

        for (String listType : lists) {
            db.collection("users")
                    .document(userId)
                    .collection(listType)
                    .whereEqualTo("id", bookId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            bookListStatus.get(bookId).add(listType);
                            adapter.notifyDataSetChanged();
                        }
                    })
                    .addOnFailureListener(e -> Log.e("SearchFragment", "Error checking " + listType + ": " + e.getMessage()));
        }
    }

    // Añadir un libro a una lista, eliminándolo de las otras
    private void addBookToList(ApiBook book, String listType, Button button, int position) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Debes iniciar sesión para añadir un libro", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String bookId = book.getId();

        // Mapear los datos del ApiBook
        Map<String, Object> bookData = new HashMap<>();
        bookData.put("id", bookId);
        bookData.put("title", book.getVolumeInfo().getTitle());
        bookData.put("author", String.join(", ", book.getVolumeInfo().getAuthors()));
        bookData.put("genre", String.join(", ", book.getVolumeInfo().getCategories()));
        bookData.put("description", book.getVolumeInfo().getDescription() != null ? book.getVolumeInfo().getDescription() : "");
        bookData.put("imageUrl", book.getVolumeInfo().getImageLinks() != null ?
                (book.getVolumeInfo().getImageLinks().getThumbnail() != null ?
                        book.getVolumeInfo().getImageLinks().getThumbnail() : "") : "");
        bookData.put("addedBy", userId);
        bookData.put("timestamp", com.google.firebase.Timestamp.now());

        // Contador para rastrear las listas revisadas
        AtomicInteger listsChecked = new AtomicInteger(0);
        int totalOtherLists = 2; // Siempre hay 2 listas "otras" (read, to_read, reading menos la seleccionada)

        // Verificar si el libro ya está en la lista seleccionada
        db.collection("users")
                .document(userId)
                .collection(listType)
                .whereEqualTo("id", bookId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(getContext(), "El libro ya está en " + getListName(listType), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Eliminar el libro de las otras listas
                    String[] otherLists = {"read", "to_read", "reading"};
                    for (String otherList : otherLists) {
                        if (!otherList.equals(listType)) {
                            db.collection("users")
                                    .document(userId)
                                    .collection(otherList)
                                    .whereEqualTo("id", bookId)
                                    .get()
                                    .addOnSuccessListener(queryDocumentSnapshots2 -> {
                                        for (var doc : queryDocumentSnapshots2) {
                                            doc.getReference().delete();
                                            bookListStatus.get(bookId).remove(otherList);
                                            Log.d("SearchFragment", "Libro eliminado de " + otherList);
                                        }
                                        if (listsChecked.incrementAndGet() == totalOtherLists) {
                                            // Añadir el libro solo después de revisar todas las listas
                                            Log.d("SearchFragment", "Añadiendo libro " + bookId + " a " + listType);
                                            db.collection("users")
                                                    .document(userId)
                                                    .collection(listType)
                                                    .add(bookData)
                                                    .addOnSuccessListener(documentReference -> {
                                                        bookListStatus.get(bookId).add(listType);
                                                        adapter.notifyItemChanged(position);
                                                        Toast.makeText(getContext(), "Libro añadido a " + getListName(listType), Toast.LENGTH_SHORT).show();
                                                        Log.d("SearchFragment", "Libro añadido con ID: " + documentReference.getId());
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(getContext(), "Error al añadir libro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        Log.e("SearchFragment", "Error adding to " + listType + ": " + e.getMessage());
                                                    });
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("SearchFragment", "Error checking other lists: " + e.getMessage());
                                        listsChecked.incrementAndGet(); // Continuar incluso si falla
                                        if (listsChecked.get() == totalOtherLists) {
                                            // Añadir el libro si todas las listas fueron revisadas
                                            Log.d("SearchFragment", "Añadiendo libro " + bookId + " a " + listType + " (tras error)");
                                            db.collection("users")
                                                    .document(userId)
                                                    .collection(listType)
                                                    .add(bookData)
                                                    .addOnSuccessListener(documentReference -> {
                                                        bookListStatus.get(bookId).add(listType);
                                                        adapter.notifyItemChanged(position);
                                                        Toast.makeText(getContext(), "Libro añadido a " + getListName(listType), Toast.LENGTH_SHORT).show();
                                                        Log.d("SearchFragment", "Libro añadido con ID: " + documentReference.getId());
                                                    })
                                                    .addOnFailureListener(e2 -> {
                                                        Toast.makeText(getContext(), "Error al añadir libro: " + e2.getMessage(), Toast.LENGTH_SHORT).show();
                                                        Log.e("SearchFragment", "Error adding to " + listType + ": " + e2.getMessage());
                                                    });
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al verificar la lista: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("SearchFragment", "Error checking " + listType + ": " + e.getMessage());
                });
    }

    // Eliminar un libro de una lista
    private void removeBookFromList(ApiBook book, String listType, Button button, int position) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Debes iniciar sesión para eliminar un libro", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String bookId = book.getId();

        db.collection("users")
                .document(userId)
                .collection(listType)
                .whereEqualTo("id", bookId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (var doc : queryDocumentSnapshots) {
                        doc.getReference().delete();
                        Log.d("SearchFragment", "Libro eliminado de " + listType + ": " + bookId);
                    }
                    bookListStatus.get(bookId).remove(listType);
                    adapter.notifyItemChanged(position);
                    Toast.makeText(getContext(), "Libro eliminado de " + getListName(listType), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al eliminar libro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("SearchFragment", "Error removing from " + listType + ": " + e.getMessage());
                });
    }

    // Helper para obtener un nombre legible para la lista
    private String getListName(String listType) {
        switch (listType) {
            case "read":
                return "Leídos";
            case "to_read":
                return "Por Leer";
            case "reading":
                return "Leyendo";
            default:
                return listType;
        }
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
                ((BookViewHolder) holder).bind(books.get(position), position);
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

        public void bind(ApiBook book, int position) {
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

            // Configurar el estado visual de los botones
            updateButtonState(btnRead, book.getId(), "read");
            updateButtonState(btnToRead, book.getId(), "to_read");
            updateButtonState(btnReading, book.getId(), "reading");

            // Configurar clics de los botones
            btnRead.setOnClickListener(v -> toggleBookInList(book, "read", btnRead, position));
            btnToRead.setOnClickListener(v -> toggleBookInList(book, "to_read", btnToRead, position));
            btnReading.setOnClickListener(v -> toggleBookInList(book, "reading", btnReading, position));
        }

        private void updateButtonState(Button button, String bookId, String listType) {
            Set<String> lists = bookListStatus.getOrDefault(bookId, new HashSet<>());
            if (lists.contains(listType)) {
                button.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_red_light));
                button.setText("Eliminar");
            } else {
                button.setBackgroundTintList(getResources().getColorStateList(R.color.colorBotones));
                button.setText(getListName(listType));
            }
        }

        private void toggleBookInList(ApiBook book, String listType, Button button, int position) {
            Set<String> lists = bookListStatus.getOrDefault(book.getId(), new HashSet<>());
            if (lists.contains(listType)) {
                removeBookFromList(book, listType, button, position);
            } else {
                addBookToList(book, listType, button, position);
            }
        }
    }

    public static class LoadingViewHolder extends RecyclerView.ViewHolder {
        public LoadingViewHolder(View itemView) {
            super(itemView);
        }
    }
}