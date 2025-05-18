package com.coffeechat;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class RegistrationActivity extends AppCompatActivity {
    private final String LOG_TAG = RegistrationActivity.class.getName();
    EditText registrationEmail;
    EditText registrationPassword;
    EditText registrationConfirmPassword;

    FirebaseAuth mAuth;
    FirebaseUser mUser;

    CoffeeChatUser coffeeChatUser = CoffeeChatUser.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.setUsernameMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        registrationEmail = findViewById(R.id.registrationEmail);
        registrationPassword = findViewById(R.id.registrationPassword);
        registrationConfirmPassword = findViewById(R.id.registrationConfirmPassword);
    }

    protected void onStart() {
        super.onStart();
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        if(mUser != null) {
            NavigationUtils.moveToActivity(RegistrationActivity.this, ChatListActivity.class);
        }
    }

    private void registrationPreparation() {
        try {
            String email = registrationEmail.getText().toString();
            String password = registrationPassword.getText().toString();
            String confirmPassword = registrationConfirmPassword.getText().toString();

            FirebaseUtils.firebaseRegistration(email, password, confirmPassword, RegistrationActivity.this, LOG_TAG, coffeeChatUser);

        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "Input validation failed: " + e.getMessage());
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void buttonBackToLoginOnClick(View view) {
        NavigationUtils.moveToLoginActivity(RegistrationActivity.this);
    }

    public void buttonRegisterOnClick(View view) {
        registrationPreparation();
    }
}
