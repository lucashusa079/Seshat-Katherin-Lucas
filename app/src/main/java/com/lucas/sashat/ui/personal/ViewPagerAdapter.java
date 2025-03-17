package com.lucas.sashat.ui.personal;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new FinishedBooksFragment(); // Lecturas acabadas
            case 1:
                return new ReadingBooksFragment();  // En lectura
            case 2:
                return new PendingBooksFragment();  // Pendientes
            default:
                return new FinishedBooksFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3; // Número de pestañas
    }
}
