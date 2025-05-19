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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class SetUsernameActivity extends AppCompatActivity {

    private final static String LOG_TAG = SetUsernameActivity.class.getName();
    EditText usernameEditText;
    FirebaseAuth mAuth;
    FirebaseUser mUser;
    FirebaseFirestore mDatabase;

    CoffeeChatUser coffeeChatUser = CoffeeChatUser.getInstance();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_username);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.setUsernameMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        usernameEditText = findViewById(R.id.usernameEditText);
    }

    protected void onStart() {
        super.onStart();
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        FirebaseUtils.checkLogin(SetUsernameActivity.this);
        mDatabase = FirebaseFirestore.getInstance();
        FirebaseUtils.checkAndUpdateEmailIfNeeded(SetUsernameActivity.this, mDatabase, mAuth, LOG_TAG);
    }

    public void buttonSetUsernameClick(View view) {
        try {
            String enteredUsername = usernameEditText.getText().toString().trim();
            InputCheckerUtils.validateNotNullOrEmpty(enteredUsername, "Username");

            FirebaseUtils.checkIfAlreadyExistsInFirestore(SetUsernameActivity.this, mDatabase, "users", "username", enteredUsername, LOG_TAG, new FirebaseUtils.FirebaseUtilsCallback() {
                @Override
                public void onSuccess() {
                    String uid = mUser.getUid();
                    HashMap<String, Object> userMap = new HashMap<>();
                    userMap.put("username", enteredUsername);
                    FirebaseUtils.updateCollectionOnFirestore(mDatabase, "users", uid, userMap, LOG_TAG, new FirebaseUtils.FirebaseUtilsCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(LOG_TAG, "Username added");
                            coffeeChatUser.setUsername(enteredUsername);
                            NavigationUtils.moveToActivity(SetUsernameActivity.this, SetAvatarActivity.class);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(LOG_TAG, "Failed to add username:" + e.getMessage());
                            Toast.makeText(SetUsernameActivity.this, "Failed to add username", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(LOG_TAG, "Firestore failure:" + e.getMessage());
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(LOG_TAG, "Exception: " + e.getMessage());
        }
    }
}
