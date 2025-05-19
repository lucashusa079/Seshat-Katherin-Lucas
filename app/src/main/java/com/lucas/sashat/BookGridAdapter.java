package com.lucas.sashat;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;

import java.util.List;

public class BookGridAdapter extends RecyclerView.Adapter<BookGridAdapter.BookViewHolder> {
    private static final String TAG = "BookGridAdapter";
    private List<Book> books;
    private Context context;
    private OnBookClickListener clickListener;

    public interface OnBookClickListener {
        void onBookClick(Book book);
    }

    public BookGridAdapter(Context context, List<Book> books, OnBookClickListener clickListener) {
        this.context = context;
        this.books = books;
        this.clickListener = clickListener;
        Log.d(TAG, "BookGridAdapter inicializado con " + books.size() + " libros");
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_book_grid, parent, false);
        Log.d(TAG, "Creando ViewHolder");
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = books.get(position);
        Log.d(TAG, "Vinculando libro en posición " + position + ": " + book.getTitle() + ", imageUri: " + book.getCoverImage());
        if (book.getCoverImage() != null && !book.getCoverImage().isEmpty()) {
            try {
                if (book.isLocalImage()) {
                    Uri uri = Uri.parse(book.getCoverImage());
                    Glide.with(context)
                            .load(uri)
                            .placeholder(R.drawable.ic_book_placeholder)
                            .error(R.drawable.ic_book_placeholder)
                            .into(holder.bookCover);
                } else {
                    Glide.with(context)
                            .load(book.getCoverImage())
                            .placeholder(R.drawable.ic_book_placeholder)
                            .error(R.drawable.ic_book_placeholder)
                            .into(holder.bookCover);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al cargar imagen: " + book.getCoverImage(), e);
                holder.bookCover.setImageResource(R.drawable.ic_book_placeholder);
            }
        } else {
            Log.w(TAG, "imageUri es null para el libro: " + book.getTitle());
            holder.bookCover.setImageResource(R.drawable.ic_book_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "Clic en el libro: " + book.getTitle());
            clickListener.onBookClick(book);
        });
    }

    @Override
    public int getItemCount() {
        Log.d(TAG, "getItemCount: " + books.size());
        return books.size();
    }

    public void updateBooks(List<Book> newBooks) {
        Log.d(TAG, "Actualizando libros, nuevos: " + newBooks.size());
        this.books.clear();
        this.books.addAll(newBooks);
        notifyDataSetChanged();
    }

    static class BookViewHolder extends RecyclerView.ViewHolder {
        ImageView bookCover;

        BookViewHolder(View itemView) {
            super(itemView);
            bookCover = itemView.findViewById(R.id.book_cover);
            Log.d(TAG, "BookViewHolder creado, bookCover: " + (bookCover != null ? "no nulo" : "nulo"));
        }
    }
}