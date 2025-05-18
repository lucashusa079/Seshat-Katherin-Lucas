package com.lucas.sashat.ui.add;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lucas.sashat.R;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class AddFragment extends Fragment {
    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText titleEditText, authorEditText, descriptionEditText;
    private Spinner genreSpinner, stateSpinner;
    private ImageView bookImage;
    private Button selectImageButton, addButton;
    private Uri imageUri;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add, container, false);

        // Inicializar vistas
        titleEditText = view.findViewById(R.id.title_edit_text);
        authorEditText = view.findViewById(R.id.author_edit_text);
        descriptionEditText = view.findViewById(R.id.description_edit_text);
        genreSpinner = view.findViewById(R.id.genre_spinner);
        stateSpinner = view.findViewById(R.id.currentState_selector);
        bookImage = view.findViewById(R.id.book_image);
        selectImageButton = view.findViewById(R.id.select_image_button);
        addButton = view.findViewById(R.id.add_button);

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Configurar el Spinner de géneros
        ArrayAdapter<CharSequence> genreAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.genres,
                android.R.layout.simple_spinner_item
        );
        genreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genreSpinner.setAdapter(genreAdapter);

        // Configurar el Spinner de estado
        ArrayAdapter<CharSequence> stateAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.book_lists,
                android.R.layout.simple_spinner_item
        );
        stateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        stateSpinner.setAdapter(stateAdapter);

        // Configurar el botón de selección de imagen
        selectImageButton.setOnClickListener(v -> openImagePicker());

        // Configurar el botón de agregar
        addButton.setOnClickListener(v -> saveBookToFirestore());

        return view;
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Seleccionar Imagen"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            Glide.with(this).load(imageUri).into(bookImage);
        }
    }

    private void saveBookToFirestore() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Debes iniciar sesión para añadir un libro", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = titleEditText.getText().toString().trim();
        String author = authorEditText.getText().toString().trim();
        String genre = genreSpinner.getSelectedItem().toString();
        String description = descriptionEditText.getText().toString().trim();
        String state = stateSpinner.getSelectedItem().toString();
        String userId = currentUser.getUid();

        if (title.isEmpty() || author.isEmpty() || genre.isEmpty()) {
            Toast.makeText(getContext(), "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mapear la lista seleccionada a la subcolección correspondiente
        String listType;
        switch (state) {
            case "Leídos":
                listType = "read";
                break;
            case "Leyendo":
                listType = "reading";
                break;
            case "Por Leer":
                listType = "to_read";
                break;
            default:
                Toast.makeText(getContext(), "Estado no válido", Toast.LENGTH_SHORT).show();
                return;
        }

        // Generar un ID único para el libro
        String bookId = UUID.randomUUID().toString();

        // Mapear los datos del libro
        Map<String, Object> bookData = new HashMap<>();
        bookData.put("id", bookId);
        bookData.put("title", title);
        bookData.put("author", author);
        bookData.put("genre", genre);
        bookData.put("description", description);
        bookData.put("imageUri", imageUri != null ? imageUri.toString() : "");
        bookData.put("addedBy", userId);
        bookData.put("timestamp", com.google.firebase.Timestamp.now());

        // Verificar si el libro ya está en la lista seleccionada
        db.collection("users")
                .document(userId)
                .collection(listType)
                .whereEqualTo("title", title)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(getContext(), "El libro ya está en " + state, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Eliminar el libro de las otras listas
                    String[] otherLists = {"read", "to_read", "reading"};
                    AtomicInteger listsChecked = new AtomicInteger(0);
                    int totalOtherLists = 2;

                    for (String otherList : otherLists) {
                        if (!otherList.equals(listType)) {
                            db.collection("users")
                                    .document(userId)
                                    .collection(otherList)
                                    .whereEqualTo("title", title)
                                    .get()
                                    .addOnSuccessListener(queryDocumentSnapshots2 -> {
                                        for (var doc : queryDocumentSnapshots2) {
                                            doc.getReference().delete();
                                            Log.d("AddFragment", "Libro eliminado de " + otherList);
                                        }
                                        if (listsChecked.incrementAndGet() == totalOtherLists) {
                                            // Añadir el libro a la lista seleccionada
                                            Log.d("AddFragment", "Añadiendo libro " + bookId + " a " + listType);
                                            db.collection("users")
                                                    .document(userId)
                                                    .collection(listType)
                                                    .document(bookId)
                                                    .set(bookData)
                                                    .addOnSuccessListener(aVoid -> {
                                                        Toast.makeText(getContext(), "Libro añadido a " + state, Toast.LENGTH_SHORT).show();
                                                        clearFields();
                                                        Log.d("AddFragment", "Libro añadido con ID: " + bookId);
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(getContext(), "Error al añadir libro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        Log.e("AddFragment", "Error adding to " + listType + ": " + e.getMessage());
                                                    });
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("AddFragment", "Error checking other lists: " + e.getMessage());
                                        listsChecked.incrementAndGet();
                                        if (listsChecked.get() == totalOtherLists) {
                                            // Añadir el libro incluso si falla una consulta
                                            Log.d("AddFragment", "Añadiendo libro " + bookId + " a " + listType + " (tras error)");
                                            db.collection("users")
                                                    .document(userId)
                                                    .collection(listType)
                                                    .document(bookId)
                                                    .set(bookData)
                                                    .addOnSuccessListener(aVoid -> {
                                                        Toast.makeText(getContext(), "Libro añadido a " + state, Toast.LENGTH_SHORT).show();
                                                        clearFields();
                                                        Log.d("AddFragment", "Libro añadido con ID: " + bookId);
                                                    })
                                                    .addOnFailureListener(e2 -> {
                                                        Toast.makeText(getContext(), "Error al añadir libro: " + e2.getMessage(), Toast.LENGTH_SHORT).show();
                                                        Log.e("AddFragment", "Error adding to " + listType + ": " + e2.getMessage());
                                                    });
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al verificar la lista: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("AddFragment", "Error checking " + listType + ": " + e.getMessage());
                });
    }

    private void clearFields() {
        titleEditText.setText("");
        authorEditText.setText("");
        descriptionEditText.setText("");
        genreSpinner.setSelection(0);
        stateSpinner.setSelection(0);
        bookImage.setImageResource(R.drawable.ic_book_placeholder);
        imageUri = null;
    }
}