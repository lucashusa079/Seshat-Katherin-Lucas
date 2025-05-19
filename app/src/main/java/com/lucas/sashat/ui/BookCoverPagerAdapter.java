package com.lucas.sashat.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.lucas.sashat.Book;

import java.util.ArrayList;
import java.util.List;

public class BookCoverPagerAdapter extends FragmentStateAdapter {
    private List<Book> books = new ArrayList<>();

    public BookCoverPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    public void setBooks(List<Book> books) {
        this.books = books;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return BookCoverFragment.newInstance(books.get(position).getImageUrl());
    }

    @Override
    public int getItemCount() {
        return books.size();
    }
}