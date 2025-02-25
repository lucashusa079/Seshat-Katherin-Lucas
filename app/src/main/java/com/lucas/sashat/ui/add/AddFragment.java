package com.lucas.sashat.ui.add;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.firestore.FirebaseFirestore;
import com.lucas.sashat.R;
import com.lucas.sashat.databinding.FragmentAddBinding;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AddFragment extends Fragment {
    private EditText titleEditText, authorEditText, genreEditText, descriptionEditText, imageUrlEditText;
    private ImageView bookImage;
    private Spinner listSelector;
    private Button addButton, selectImageButton;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;
    private FirebaseAuth mAuth;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_add, container, false);

        titleEditText = rootView.findViewById(R.id.title_edit_text);
        authorEditText = rootView.findViewById(R.id.author_edit_text);
        genreEditText = rootView.findViewById(R.id.genre_edit_text);
        descriptionEditText = rootView.findViewById(R.id.description_edit_text);
        bookImage = rootView.findViewById(R.id.book_image);
        addButton = rootView.findViewById(R.id.add_button);
        selectImageButton = rootView.findViewById(R.id.select_image_button);
        listSelector = rootView.findViewById(R.id.list_selector);

        // Inicializar el FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // Acción para el botón de "Subir Imagen"
        selectImageButton.setOnClickListener(v -> openFileChooser());

        // Acción para el botón "Agregar"
        addButton.setOnClickListener(view -> {
            String title = titleEditText.getText().toString();
            String author = authorEditText.getText().toString();
            String genre = genreEditText.getText().toString();
            String imageUrl = imageUrlEditText.getText().toString();
            String description = descriptionEditText.getText().toString();
            String list = listSelector.getSelectedItem().toString();

            // Subir el libro a Firestore
            addBookToFirestore(title, author, genre, description, imageUrl, list);
        });

        return rootView;
    }

    private void addBookToFirestore(String title, String author, String genre, String description, String imageUrl, String list) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> book = new HashMap<>();
        book.put("title", title);
        book.put("author", author);
        book.put("genre", genre);
        book.put("description", description);
        book.put("coverImage", imageUrl.isEmpty() ? getImageUrl() : imageUrl);
        book.put("list", list); // Se agrega a qué lista pertenece
        book.put("userId", mAuth.getCurrentUser().getUid()); // Asociar el libro al usuario

        db.collection("Books")
                .add(book)
                .addOnSuccessListener(documentReference -> Log.d("AddFragment", "Libro agregado"))
                .addOnFailureListener(e -> Log.e("AddFragment", "Error al agregar libro", e));
    }

    // Método para obtener la URL de la imagen
    private String getImageUrl() {
        if (imageUri != null) {
            // Puedes subir la imagen a Firebase Storage y obtener la URL
            return uploadImageToStorage();
        }
        // Si no hay imagen seleccionada, retorna la URL por defecto
        return "https://example.com/default_image.jpg";  // Cambiar por una URL de imagen por defecto
    }

    // Método para subir la imagen seleccionada a Firebase Storage
    private String uploadImageToStorage() {
        // Aquí deberías subir la imagen a Firebase Storage y obtener la URL de descarga
        // Ejemplo de código para subir la imagen y obtener la URL
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference().child("book_images/" + UUID.randomUUID().toString());
        UploadTask uploadTask = storageRef.putFile(imageUri);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                // Aquí se puede agregar la URL de la imagen al libro
                Log.d("AddFragment", "Imagen subida con éxito. URL: " + downloadUrl);
            });
        }).addOnFailureListener(e -> Log.e("AddFragment", "Error al subir la imagen", e));

        return "default_url";  // Retornar una URL temporal hasta que la imagen sea subida
    }

    // Método para abrir el selector de archivos y elegir una imagen
    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    // Método para manejar la imagen seleccionada
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            bookImage.setImageURI(imageUri); // Muestra la imagen seleccionada
        }
    }
}
