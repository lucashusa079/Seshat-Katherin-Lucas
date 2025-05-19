package com.lucas.sashat;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.lucas.sashat.ui.BookListFragment;

public class BookListPagerAdapter extends FragmentStateAdapter {
    private static final String[] LIST_TYPES = {"Leído", "En lectura", "Pendientes"};
    private final String viewedUserId;

    public BookListPagerAdapter(@NonNull FragmentActivity fragmentActivity, String viewedUserId) {
        super(fragmentActivity);
        this.viewedUserId = viewedUserId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Log.d("BookListPagerAdapter", "Creando fragmento para: " + LIST_TYPES[position]);
        return BookListFragment.newInstance(LIST_TYPES[position], viewedUserId);
    }

    @Override
    public int getItemCount() {
        return LIST_TYPES.length;
    }
}