package com.lucas.sashat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {
    private List<Book> bookList;

    public BookAdapter(List<Book> bookList) {
        this.bookList = bookList;
    }

    // ViewHolder para vincular los elementos de item_book_search.xml
    public static class BookViewHolder extends RecyclerView.ViewHolder {
        public ImageView bookImage;
        public TextView bookTitle;
        public TextView bookAuthor;
        public TextView bookGenre;
        public Button btnRead;
        public Button btnToRead;
        public Button btnReading;

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
    }

    @Override
    public BookViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_book_search, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(BookViewHolder holder, int position) {
        Book book = bookList.get(position);
        holder.bookImage.setImageResource(book.getImageResId());
        holder.bookTitle.setText(book.getTitle());
        holder.bookAuthor.setText(book.getAuthor());
        holder.bookGenre.setText(book.getGenre());

        // Ejemplo de acción en los botones (opcional)
        holder.btnRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println(book.getTitle() + " marcado como Leído");
            }
        });

        holder.btnToRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println(book.getTitle() + " marcado como Por Leer");
            }
        });

        holder.btnReading.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println(book.getTitle() + " marcado como Leyendo");
            }
        });
    }

    @Override
    public int getItemCount() {
        return bookList.size();
    }
}
