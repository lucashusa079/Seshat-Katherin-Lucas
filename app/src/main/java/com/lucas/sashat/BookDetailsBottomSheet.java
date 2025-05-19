package com.lucas.sashat;

import android.app.DatePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.parceler.Parcels;

import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class BookDetailsBottomSheet extends BottomSheetDialogFragment {
    private static final String ARG_BOOK = "book";
    private static final String ARG_USER_ID = "userId";
    private static final String ARG_LIST_TYPE = "listType";
    private Book book;
    private String userId;
    private String listType;
    private FirebaseFirestore db;
    private TextView tvStartDate;
    private TextView tvEndDate;

    public static BookDetailsBottomSheet newInstance(Book book, String userId, String listType) {
        BookDetailsBottomSheet fragment = new BookDetailsBottomSheet();
        Bundle args = new Bundle();
        args.putParcelable(ARG_BOOK, Parcels.wrap(book));
        args.putString(ARG_USER_ID, userId);
        args.putString(ARG_LIST_TYPE, listType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        if (getArguments() != null) {
            book = Parcels.unwrap(getArguments().getParcelable(ARG_BOOK));
            userId = getArguments().getString(ARG_USER_ID);
            listType = getArguments().getString(ARG_LIST_TYPE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_book_details_sheet, container, false);

        // Inicializar vistas
        ShapeableImageView ivBookCover = view.findViewById(R.id.ivBookCover);
        TextView tvBookTitle = view.findViewById(R.id.tvBookTitle);
        TextView tvBookAuthor = view.findViewById(R.id.tvBookAuthor);
        TextView tvBookGenre = view.findViewById(R.id.tvBookGenre);
        tvStartDate = view.findViewById(R.id.tvStartDate);
        tvEndDate = view.findViewById(R.id.tvEndDate);
        RatingBar rbRating = view.findViewById(R.id.rbRating);
        EditText etBookNotes = view.findViewById(R.id.etBookNotes);
        Button btnSave = view.findViewById(R.id.btnSave);
        Button btnClose = view.findViewById(R.id.btnClose);

        // Configurar datos del libro
        tvBookTitle.setText(book.getTitle() != null ? book.getTitle() : "Sin título");
        tvBookAuthor.setText(book.getAuthor() != null ? "Autor: " + book.getAuthor() : "Autor: Desconocido");
        tvBookGenre.setText(book.getGenre() != null ? "Género: " + book.getGenre() : "Género: Desconocido");
        etBookNotes.setText(book.getNotes() != null ? book.getNotes() : "");
        rbRating.setRating((float) book.getRating());

        updateDateTextView(book.getStartDate(), tvStartDate, "Fecha de inicio: No disponible");
        updateDateTextView(book.getEndDate(), tvEndDate, "Fecha de finalización: No disponible");

        if (book.getCoverImage() != null && !book.getCoverImage().isEmpty()) {
            try {
                String coverImage = book.getCoverImage();

                if (coverImage.startsWith("http://") || coverImage.startsWith("https://")) {
                    // Es URL remota
                    Glide.with(this)
                            .load(coverImage)
                            .placeholder(R.drawable.ic_book_placeholder)
                            .error(R.drawable.ic_book_placeholder)
                            .into(ivBookCover);
                } else {
                    // Es URI local o path local
                    Uri uri = Uri.parse(coverImage);
                    Glide.with(this)
                            .load(uri)
                            .placeholder(R.drawable.ic_book_placeholder)
                            .error(R.drawable.ic_book_placeholder)
                            .into(ivBookCover);
                }
            } catch (Exception e) {
                ivBookCover.setImageResource(R.drawable.ic_book_placeholder);
            }
        } else {
            ivBookCover.setImageResource(R.drawable.ic_book_placeholder);
        }

        // Configurar DatePicker para fechas
        tvStartDate.setOnClickListener(v -> showDatePickerDialog(true));
        tvEndDate.setOnClickListener(v -> showDatePickerDialog(false));

        // Guardar cambios
        btnSave.setOnClickListener(v -> {
            String startDate = tvStartDate.getText().toString();
            String endDate = tvEndDate.getText().toString();
            float rating = rbRating.getRating();
            String notes = etBookNotes.getText().toString();

            if (book.getDocumentId() != null && !book.getDocumentId().isEmpty()) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                DocumentReference bookRef = db
                        .collection("users")
                        .document(userId)
                        .collection(getCollectionPath())
                        .document(book.getId());


                Map<String, Object> updates = new HashMap<>();
                updates.put("startDate", book.getStartDate());
                updates.put("endDate", book.getEndDate());
                updates.put("rating", rating);
                updates.put("notes", notes);

                bookRef.update(updates)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("BookDetailsBottomSheet", "Datos actualizados correctamente.");
                            dismiss(); // cerrar el BottomSheet si quieres
                        })
                        .addOnFailureListener(e -> {
                            Log.e("BookDetailsBottomSheet", "Error al actualizar datos", e);
                        });
            } else {
                // opcional: manejar caso para libros locales si quieres
                Log.d("BookDetailsBottomSheet", "Este libro no está en Firebase");
            }
        });


        // Cerrar
        btnClose.setOnClickListener(v -> dismiss());

        return view;
    }

    private void showDatePickerDialog(boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, month, dayOfMonth);
                    // Crear un Timestamp a partir de la fecha seleccionada
                    Timestamp timestamp = new Timestamp(selectedCalendar.getTime());
                    if (isStartDate) {
                        book.setStartDate(timestamp);
                        updateDateTextView(book.getStartDate(), tvStartDate, "Fecha de inicio: No disponible");
                    } else {
                        book.setEndDate(timestamp);
                        updateDateTextView(book.getEndDate(), tvEndDate, "Fecha de finalización: No disponible");
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateDateTextView(Timestamp timestamp, TextView textView, String defaultText) {
        if (timestamp != null && timestamp.toDate() != null) {
            textView.setText(new SimpleDateFormat("dd/MM/yyyy").format(timestamp.toDate()));
        } else {
            textView.setText(defaultText);
        }
    }

    private String getCollectionPath() {
        switch (listType) {
            case "Leído":
                return "read";
            case "En lectura":
                return "reading";
            case "Pendientes":
                return "to_read";
            default:
                return "";
        }
    }
}
