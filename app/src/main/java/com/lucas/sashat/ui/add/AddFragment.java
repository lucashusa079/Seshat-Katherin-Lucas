package com.lucas.sashat.ui.add;

import android.app.Activity;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lucas.sashat.R;
import com.lucas.sashat.ui.home.CreatePostFragment;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddFragment extends Fragment {
    private EditText titleEditText, authorEditText, descriptionEditText;
    private Spinner genreSpinner;
    private Button selectImageButton, addButton;
    private ImageView bookImage;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Uri imageUri;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    Log.d("AddFragment", "Imagen seleccionada: " + imageUri);
                    Glide.with(this)
                            .load(imageUri)
                            .placeholder(R.drawable.ic_book_placeholder)
                            .error(R.drawable.ic_book_placeholder)
                            .into(bookImage);
                } else {
                    Log.w("AddFragment", "No se seleccionó ninguna imagen");
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add, container, false);

        titleEditText = view.findViewById(R.id.title_edit_text);
        authorEditText = view.findViewById(R.id.author_edit_text);
        descriptionEditText = view.findViewById(R.id.description_edit_text);
        genreSpinner = view.findViewById(R.id.list_selector);
        selectImageButton = view.findViewById(R.id.select_image_button);
        addButton = view.findViewById(R.id.add_button);
        bookImage = view.findViewById(R.id.book_image);
        ImageButton closeButton = view.findViewById(R.id.closeButton);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Configurar el Spinner con los géneros según el idioma del dispositivo
        String[] genres = getResources().getStringArray(R.array.genres);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, genres);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genreSpinner.setAdapter(adapter);

        // Log del idioma detectado
        String language = Locale.getDefault().getLanguage();
        Log.d("AddFragment", "Idioma del dispositivo: " + language);

        selectImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        addButton.setOnClickListener(v -> addBookToFirestore());
        closeButton.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(AddFragment.this);
            navController.popBackStack();
        });
        return view;
    }

    private void addBookToFirestore() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Debes iniciar sesión para añadir un libro", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = titleEditText.getText().toString().trim();
        String author = authorEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String genre = genreSpinner.getSelectedItem().toString();

        if (title.isEmpty() || author.isEmpty() || genre.isEmpty()) {
            Toast.makeText(getContext(), "Por favor, completa los campos obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri == null) {
            Toast.makeText(getContext(), "Por favor, selecciona una imagen", Toast.LENGTH_SHORT).show();
            return;
        }

        saveBookToFirestore(title, author, description, genre, imageUri.toString(), currentUser.getUid());
    }

    private void saveBookToFirestore(String title, String author, String description, String genre, String imageUriString, String userId) {
        Map<String, Object> book = new HashMap<>();
        book.put("title", title);
        book.put("author", author);
        book.put("genre", genre);
        book.put("description", description);
        book.put("imageUri", imageUriString);
        book.put("addedBy", userId);
        book.put("timestamp", com.google.firebase.Timestamp.now());

        db.collection("users")
                .document(userId)
                .collection("user_books")
                .add(book)
                .addOnSuccessListener(documentReference -> {
                    Log.d("AddFragment", "Libro guardado con ID: " + documentReference.getId());
                    Toast.makeText(getContext(), "Libro añadido con éxito", Toast.LENGTH_SHORT).show();
                    clearFields();
                })
                .addOnFailureListener(e -> {
                    Log.e("AddFragment", "Error al guardar libro: " + e.getMessage());
                    Toast.makeText(getContext(), "Error al añadir el libro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void clearFields() {
        titleEditText.setText("");
        authorEditText.setText("");
        descriptionEditText.setText("");
        genreSpinner.setSelection(0);
        bookImage.setImageResource(R.drawable.ic_book_placeholder);
        imageUri = null;
    }
}