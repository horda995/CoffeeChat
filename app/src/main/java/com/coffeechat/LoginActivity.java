package com.coffeechat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {
    EditText loginEmail;
    EditText loginPassword;
    private FirebaseAuth mAuth;
    private final static String LOG_TAG = "LoginActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.setUsernameMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        loginEmail = findViewById(R.id.loginEmail);
        loginPassword = findViewById(R.id.loginPassword);
        mAuth = FirebaseAuth.getInstance();
    }
    public void goToChatListActivity() {
        Intent moveToChatList = new Intent(LoginActivity.this, ChatListActivity.class);
        startActivity(moveToChatList);
        finish();
    }
    public void goToSetUsernameActivity() {
        Intent moveToUsername = new Intent(LoginActivity.this, SetUsernameActivity.class);
        startActivity(moveToUsername);
        finish();
    }
    public void buttonLoginOnClick(View view) {
        try {
            String email = loginEmail.getText().toString().trim();
            String password = loginPassword.getText().toString().trim();

            InputChecker.validateNotNullOrEmpty(email, "E-mail");
            InputChecker.validateNotNullOrEmpty(password, "password");

            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    Log.d(LOG_TAG, "Login was successful!");
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        db.collection("users").document(user.getUid())
                                .get()
                                .addOnSuccessListener(document -> {
                                    if (document.exists() && document.contains("username") && document.getString("username") != null) {
                                        goToChatListActivity();
                                        finish();
                                    } else {
                                        Log.d(LOG_TAG, "Username not set, redirecting to SetUsernameActivity.");
                                        goToSetUsernameActivity();
                                        finish();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(LOG_TAG, "Failed to check username: " + e.getMessage());
                                    Toast.makeText(LoginActivity.this, "Login failed: couldn't verify username", Toast.LENGTH_SHORT).show();
                                });
                    }
                } else {
                    Log.d(LOG_TAG, "Login was unsuccessful!");
                    Toast.makeText(LoginActivity.this, "Login failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "Input validation failed: " + e.getMessage());
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }
    public void buttonGoToRegistrationOnClick(View view) {
        Intent moveToRegistrationScreen = new Intent(LoginActivity.this, RegistrationActivity.class);
        startActivity(moveToRegistrationScreen);
        finish();
    }
}