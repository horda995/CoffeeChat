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
import java.util.Objects;

public class LoginActivity extends AppCompatActivity {
    EditText loginEmail;
    EditText loginPassword;
    private FirebaseAuth mailAuth;
    private final static String LOG_TAG = "LoginActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        loginEmail = findViewById(R.id.loginEmail);
        loginPassword = findViewById(R.id.loginPassword);
        mailAuth = FirebaseAuth.getInstance();
    }
    public void goToChatListActivity() {
        Intent moveToChatList = new Intent(LoginActivity.this, ChatListActivity.class);
        startActivity(moveToChatList);
        finish();
    }
    public void buttonLoginOnClick(View view) {
        try {
            String email = loginEmail.getText().toString();
            String password = loginPassword.getText().toString();

            InputChecker.validateNotNullOrEmpty(email, "E-mail");
            InputChecker.validateNotNullOrEmpty(password, "password");

            mailAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
                if(task.isSuccessful()) {
                    Log.d(LOG_TAG, "Login was successful!");
                    goToChatListActivity();
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