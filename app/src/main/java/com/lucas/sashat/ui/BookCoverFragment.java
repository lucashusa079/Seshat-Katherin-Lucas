package com.lucas.sashat.ui;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.lucas.sashat.R;

public class BookCoverFragment extends Fragment {
    private static final String ARG_COVER_IMAGE = "cover_image";

    public static BookCoverFragment newInstance(String coverImage) {
        BookCoverFragment fragment = new BookCoverFragment();
        Bundle args = new Bundle();
        args.putString(ARG_COVER_IMAGE, coverImage);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_book_cover, container, false);
        ImageView ivBookCover = view.findViewById(R.id.ivBookCover);

        String coverImage = getArguments() != null ? getArguments().getString(ARG_COVER_IMAGE) : null;
        if (coverImage != null && !coverImage.isEmpty()) {
            try {
                // Intentar cargar como URI (para libros del usuario)
                Uri uri = Uri.parse(coverImage);
                Glide.with(this)
                        .load(uri)
                        .placeholder(R.drawable.import_contacts_24px)
                        .error(R.drawable.import_contacts_24px)
                        .into(ivBookCover);
            } catch (Exception e) {
                // Si falla como URI, cargar como URL (para Google Books)
                Glide.with(this)
                        .load(coverImage)
                        .placeholder(R.drawable.import_contacts_24px)
                        .error(R.drawable.import_contacts_24px)
                        .into(ivBookCover);
            }
        } else {
            ivBookCover.setImageResource(R.drawable.import_contacts_24px);
        }

        return view;
    }
}