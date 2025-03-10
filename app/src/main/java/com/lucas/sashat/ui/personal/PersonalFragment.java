package com.lucas.sashat.ui.personal;

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
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.lucas.sashat.R;
import com.lucas.sashat.databinding.FragmentPersonalBinding;
import com.lucas.sashat.ui.login.LoginActivity;

public class PersonalFragment extends Fragment {
    private TextView usernameTextView, bioTextView;
    private RecyclerView booksRecyclerView;

    private @NonNull FragmentPersonalBinding binding;
    private FirebaseAuth mAuth;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // Inicializar FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        PersonalViewModel homeViewModel =
                new ViewModelProvider(this).get(PersonalViewModel.class);

        binding = FragmentPersonalBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

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


    /*
    *  View rootView = inflater.inflate(R.layout.fragment_personal, container, false);

        return rootView;
    * */
}
