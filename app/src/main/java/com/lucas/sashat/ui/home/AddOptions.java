package com.lucas.sashat.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.lucas.sashat.R;

public class AddOptions extends Fragment {

    public AddOptions() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.add_options, container, false);

        Button btnAddBook = view.findViewById(R.id.btnAddBook);
        Button btnAddPost = view.findViewById(R.id.btnAddPost);

        btnAddBook.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.fragment_add);
        });

        btnAddPost.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.createPostFragment);
        });

        return view;
    }
}

