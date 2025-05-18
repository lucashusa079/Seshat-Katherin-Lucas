package com.lucas.sashat.ui.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.lucas.sashat.R;
import com.lucas.sashat.databinding.FragmentSettingsBinding;
import com.lucas.sashat.ui.login.LoginActivity;

import java.util.Locale;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SharedPreferences preferences;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = requireContext().getSharedPreferences("settings", requireContext().MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        loadLocale(); // Cargar el idioma guardado
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // MODO NOCTURNO
        binding.rlNightMode.setOnClickListener(v -> {
            boolean currentMode = preferences.getBoolean("night_mode", false);
            SharedPreferences.Editor editor = preferences.edit();

            if (currentMode) {
                editor.putBoolean("night_mode", false);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                editor.putBoolean("night_mode", true);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }

            editor.apply();
            requireActivity().recreate(); // Reiniciar la actividad para aplicar cambios
        });

        // NOTIFICACIONES
        binding.rlNotifications.setOnClickListener(v -> {
            boolean notificationsEnabled = preferences.getBoolean("notifications_enabled", true);
            SharedPreferences.Editor editor = preferences.edit();

            if (notificationsEnabled) {
                editor.putBoolean("notifications_enabled", false);
                Toast.makeText(requireContext(), "Off", Toast.LENGTH_SHORT).show();
            } else {
                editor.putBoolean("notifications_enabled", true);
                Toast.makeText(requireContext(), "On", Toast.LENGTH_SHORT).show();
            }

            editor.apply();
        });


        // CAMBIO DE IDIOMA
        binding.rlLanguage.setOnClickListener(v -> openLanguageDialog());

        // CERRAR SESIÓN
        binding.rlLogOut.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut(); // Cierra sesión en Firebase

            Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            requireActivity().finish();
        });
        binding.rlSavePosts.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.savedPostsFragment);
        });

        binding.ivAtras.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.navigation_personal);
        });
    }

    private void openLanguageDialog() {
        String[] languages = {"English", "Català", "Español", "Français", "Deutsch"};
        String[] languageCodes = {"en", "ca", "es", "fr", "de"};

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.choose_language))
                .setItems(languages, (dialog, which) -> {
                    setLocale(languageCodes[which]); // Cambiar el idioma
                    requireActivity().recreate(); // Reiniciar la actividad para aplicar el cambio
                })
                .create()
                .show();
    }

    private void setLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);

        requireContext().getResources().updateConfiguration(config, requireContext().getResources().getDisplayMetrics());

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("language", languageCode);
        editor.apply();
    }

    private void loadLocale() {
        String languageCode = preferences.getString("language", "en"); // Por defecto inglés
        setLocale(languageCode);
    }

}
