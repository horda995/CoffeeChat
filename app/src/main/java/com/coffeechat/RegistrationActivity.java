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

public class RegistrationActivity extends AppCompatActivity {
    private final String LOG_TAG = "RegistrationActivity";
    EditText registrationEmail;
    EditText registrationUserName;
    EditText registrationPassword;
    EditText registrationConfirmPassword;
    private FirebaseAuth mailAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        registrationEmail = findViewById(R.id.registrationEmail);
        registrationUserName = findViewById(R.id.registrationUserName);
        registrationPassword = findViewById(R.id.registrationPassword);
        registrationConfirmPassword = findViewById(R.id.registrationConfirmPassword);
        mailAuth = FirebaseAuth.getInstance();
    }

    public void buttonBackToLoginOnClick(View view) {
        Intent moveToLoginScreen = new Intent(RegistrationActivity.this, LoginActivity.class);
        startActivity(moveToLoginScreen);
        finish();
    }

    public void goToChatListActivity() {
        Intent moveToChatList = new Intent(RegistrationActivity.this, ChatListActivity.class);
        startActivity(moveToChatList);
        finish();
    }
    public void buttonRegisterOnClick(View view) {
        try {
            String email = registrationEmail.getText().toString();
            String userName = registrationUserName.getText().toString();
            String password = registrationPassword.getText().toString();
            String confirmPassword = registrationConfirmPassword.getText().toString();

            InputChecker.validateNotNullOrEmpty(email, "E-mail");
            InputChecker.validateNotNullOrEmpty(userName, "Username");
            InputChecker.validateNotNullOrEmpty(password, "Password");
            InputChecker.validateNotNullOrEmpty(confirmPassword, "Confirmed password");

            if (!password.equals(confirmPassword)) {
                Log.e(LOG_TAG, "Password mismatch!");
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
                return;
            }
            mailAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    Log.d(LOG_TAG, "Account creation is successful");
                    goToChatListActivity();
                } else {
                    Log.d(LOG_TAG, "Account creation failed: " + Objects.requireNonNull(task.getException()).getMessage());
                    Toast.makeText(this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "Input validation failed: " + e.getMessage());
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}