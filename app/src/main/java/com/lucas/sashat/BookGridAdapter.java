package com.lucas.sashat;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

public class BookGridAdapter extends RecyclerView.Adapter<BookGridAdapter.BookViewHolder> {
    private static final String TAG = "BookGridAdapter";
    private List<Book> books;
    private Context context;

    public BookGridAdapter(Context context, List<Book> books) {
        this.context = context;
        this.books = books;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_book_grid, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = books.get(position);
        Log.d(TAG, "Cargando imagen para: " + book.getTitle() + ", URL: " + book.getImageUrl());
        Glide.with(context)
                .load(book.getImageUrl()) // Cambiado de getCoverImage a getImageUrl
                .placeholder(R.drawable.ic_book_placeholder)
                .error(R.drawable.ic_book_placeholder)
                .into(holder.bookCover);
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    public void updateBooks(List<Book> newBooks) {
        this.books.clear();
        this.books.addAll(newBooks);
        notifyDataSetChanged();
    }

    static class BookViewHolder extends RecyclerView.ViewHolder {
        ImageView bookCover;

        BookViewHolder(View itemView) {
            super(itemView);
            bookCover = itemView.findViewById(R.id.book_cover);
        }
    }
}
