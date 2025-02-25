package com.lucas.sashat.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.lucas.sashat.R;
import com.lucas.sashat.ui.login.LoginActivity;
import com.lucas.sashat.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FirebaseAuth mAuth;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // Inicializar FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textHome;
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        // Configurar el botón de cerrar sesión
        Button btnLogout = binding.getRoot().findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            // Cerrar sesión
            mAuth.signOut();
            Toast.makeText(getContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show();

            // Redirigir a la pantalla de inicio de sesión
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
            getActivity().finish();  // Finalizar la actividad actual
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
