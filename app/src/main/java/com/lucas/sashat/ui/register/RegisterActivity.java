package com.lucas.sashat.ui.register;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.lucas.sashat.R;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         << << << < HEAD
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

EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
