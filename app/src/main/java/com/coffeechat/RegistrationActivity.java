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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RegistrationActivity extends AppCompatActivity {
    private final String LOG_TAG = "RegistrationActivity";
    EditText registrationEmail;
    EditText registrationPassword;
    EditText registrationConfirmPassword;

    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.setUsernameMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        registrationEmail = findViewById(R.id.registrationEmail);
        registrationPassword = findViewById(R.id.registrationPassword);
        registrationConfirmPassword = findViewById(R.id.registrationConfirmPassword);
        mAuth = FirebaseAuth.getInstance();
    }

    public void buttonBackToLoginOnClick(View view) {
        Intent moveToLoginScreen = new Intent(RegistrationActivity.this, LoginActivity.class);
        startActivity(moveToLoginScreen);
        finish();
    }

    public void goToSetUsernameActivity() {
        Intent moveToSetUsername = new Intent(RegistrationActivity.this, SetUsernameActivity.class);
        startActivity(moveToSetUsername);
        finish();
    }
    public void buttonRegisterOnClick(View view) {
        try {
            String email = registrationEmail.getText().toString();
            String password = registrationPassword.getText().toString();
            String confirmPassword = registrationConfirmPassword.getText().toString();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            InputChecker.validateNotNullOrEmpty(email, "E-mail");
            InputChecker.validateNotNullOrEmpty(password, "Password");
            InputChecker.validateNotNullOrEmpty(confirmPassword, "Confirmed password");

            if (!password.equals(confirmPassword)) {
                Log.e(LOG_TAG, "Password mismatch!");
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    Log.d(LOG_TAG, "Account creation is successful");

                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        String uid = user.getUid();
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("email", email);

                        db.collection("users").document(uid)
                                .set(userMap)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(LOG_TAG, "User email successfully saved to Firestore");
                                    goToSetUsernameActivity();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(LOG_TAG, "Error saving user email to Firestore: " + e.getMessage());
                                    Toast.makeText(this, "Failed to save e-mail", Toast.LENGTH_SHORT).show();
                                });
                    }
                } else {
                    Log.e(LOG_TAG, "Account creation failed: " + Objects.requireNonNull(task.getException()).getMessage());
                    Toast.makeText(this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "Input validation failed: " + e.getMessage());
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
