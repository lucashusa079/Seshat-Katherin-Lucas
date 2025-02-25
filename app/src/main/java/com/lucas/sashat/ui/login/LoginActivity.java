package com.lucas.sashat.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.lucas.sashat.MainActivity;
import com.lucas.sashat.R;
import com.lucas.sashat.ui.register.RegisterActivity;

public class LoginActivity extends AppCompatActivity {

    // UI Components
    TextInputEditText editTextEmail, editTextPassword;
    Button btnBack, buttonLogin;
    TextView tvForgotPassword;


    // Firebase Authentication
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // UI Initialization
        editTextEmail = findViewById(R.id.etEmail);
        editTextPassword = findViewById(R.id.etPassword);
        btnBack = findViewById(R.id.btnBack);
        buttonLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);


        tvForgotPassword.setOnClickListener(view -> showResetPasswordDialog());

        // Set up button listeners
        setupButtonListeners();
    }

    // En caso de olvidar la contraseña
    private void showResetPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.restablecer_contrase_a);

        // Crear un EditText para que el usuario ingrese su correo
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        builder.setView(input);

        // Configurar los botones del diálogo
        builder.setPositiveButton(R.string.enviar, (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (!email.isEmpty()) {
                resetPassword(email);
            } else {
                Toast.makeText(this, R.string.ingresa_un_correo_v_lido, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.cancelar, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void resetPassword(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, R.string.correo_enviado, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, R.string.error_correo, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setupButtonListeners() {
        // Register Button Listener
        btnBack.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
            startActivity(intent);
            finish();
        });

        // Email and Password Login Button Listener
        buttonLogin.setOnClickListener(view -> loginWithEmailPassword());
    }

    private void loginWithEmailPassword() {
        String email = editTextEmail.getText() != null ? editTextEmail.getText().toString().trim() : "";
        String password = editTextPassword.getText() != null ? editTextPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(LoginActivity.this, R.string.enter_email, Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(LoginActivity.this, R.string.enter_password, Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getApplicationContext(), R.string.login_successful, Toast.LENGTH_SHORT).show();
                        navigateToHome();
                    } else {
                        Toast.makeText(LoginActivity.this, R.string.authentication_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToHome() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToHome();
        }
    }
}