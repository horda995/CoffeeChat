package com.coffeechat;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;


public class LoginActivity extends AppCompatActivity {
    private EditText loginEmail;
    private EditText loginPassword;
    private final static String LOG_TAG = "LoginActivity";
    CoffeeChatUser coffeeChatUser = CoffeeChatUser.getInstance();
    FirebaseFirestore mDatabase;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.setUsernameMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        loginEmail = findViewById(R.id.loginEmail);
        loginPassword = findViewById(R.id.loginPassword);

    }

    protected void onStart() {
        super.onStart();
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseFirestore.getInstance();
    }

    private void prepareLogin() {
        String email = loginEmail.getText().toString().trim();
        String password = loginPassword.getText().toString().trim();

        FirebaseUtils.performLogin(LoginActivity.this, mAuth, mDatabase, email, password, LOG_TAG, new LoginCallback() {
            @Override
            public void onSuccess(FirebaseUser currentUser) {
                coffeeChatUser.setEmail(currentUser.getEmail());
                coffeeChatUser.setUid(currentUser.getUid());
                Log.d(LOG_TAG, "Login was successful");
            }
            @Override
            public void onFailure(Exception exception) {
                Log.e(LOG_TAG, "Input validation failed.");
            }
        });
    }

    public void buttonLoginOnClick(View view) {
        prepareLogin();
    }
    public void buttonGoToRegistrationOnClick(View view) {
        NavigationUtils.moveToActivity(this, RegistrationActivity.class);
    }
}