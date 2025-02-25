package com.lucas.sashat.ui.register;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lucas.sashat.MainActivity;
import com.lucas.sashat.R;
import com.lucas.sashat.firebase.FirebaseFirestoreHelper;
import com.lucas.sashat.ui.login.LoginActivity;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends AppCompatActivity {
    TextInputEditText etEmail, etPassword, etUser;
    Button btnCreateAccount, btnSignIn;
    FirebaseAuth mAuth;
    FirebaseFirestoreHelper firestoreHelper;

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        firestoreHelper = new FirebaseFirestoreHelper(this, FirebaseFirestore.getInstance());

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etUser = findViewById(R.id.etUser);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        btnSignIn = findViewById(R.id.btnSignIn);

        btnSignIn.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            finish();
        });

        btnCreateAccount.setOnClickListener(view -> {
            String user = etUser.getText().toString();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(RegisterActivity.this, R.string.enter_email, Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(password)) {
                Toast.makeText(RegisterActivity.this, R.string.enter_password, Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(user)) {
                Toast.makeText(RegisterActivity.this, R.string.enter_user, Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                String userId = firebaseUser.getUid();

                                // Guardar en Firestore
                                firestoreHelper.addUser(userId, email, user, "", ""); // photoUrl y description vacíos

                                Toast.makeText(RegisterActivity.this, R.string.account_created, Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                finish();
                            }
                        } else {
                            Toast.makeText(RegisterActivity.this, R.string.authentication_failed, Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}
