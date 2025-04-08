package com.lucas.sashat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
   /*
public class BookGridAdapter extends RecyclerView.Adapter<BookGridAdapter.BookGridViewHolder> {
 private List<Book> bookList;

    public BookGridAdapter(List<Book> bookList) {
        this.bookList = bookList;
    }

    public static class BookGridViewHolder extends RecyclerView.ViewHolder {
        public ImageView bookImage;

        public BookGridViewHolder(View itemView) {
            super(itemView);
            bookImage = itemView.findViewById(R.id.book_image); // Asumiendo que el layout contiene un ImageView con este ID
        }
    }

    @Override
    public BookGridViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflar un layout solo con la portada (si es necesario otro archivo XML o el mismo que el de BookAdapter)
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_book_search, parent, false); // Puedes usar el mismo layout de BookAdapter
        return new BookGridViewHolder(view);
    }

    @Override
    public void onBindViewHolder(BookGridViewHolder holder, int position) {
        Book book = bookList.get(position);
        holder.bookImage.setImageResource(book.getImageResId()); // Solo cargar la portada del libro
    }

    @Override
    public int getItemCount() {
        return bookList.size();
    }
}
*/