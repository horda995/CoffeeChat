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

public class SetUsernameActivity extends AppCompatActivity {

    private final static String LOG_TAG = SetUsernameActivity.class.getName();
    EditText usernameEditText;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_set_username);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.setUsernameMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        usernameEditText = findViewById(R.id.usernameEditText);
    }


    public void goToSetProfilePictureActivity() {
        Intent moveToChatList = new Intent(SetUsernameActivity.this, SetProfilePictureActivity.class);
        startActivity(moveToChatList);
        finish();
    }

    public void buttonSetUsernameClick(View view) {
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Log.d(LOG_TAG, "UNAUTHENTICATED USER!");
                finish();
                return;
            }
            Log.d(LOG_TAG, "User is authenticated.");

            String enteredUsername = usernameEditText.getText().toString().trim();
            InputChecker.validateNotNullOrEmpty(enteredUsername, "Username");
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Check if the username already exists
            db.collection("users")
                    .whereEqualTo("username", enteredUsername)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            Toast.makeText(this, "Username already taken", Toast.LENGTH_SHORT).show();
                        } else {
                            // Username is unique, update Firestore
                            String uid = user.getUid();
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("username", enteredUsername);

                            db.collection("users").document(uid)
                                    .update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(LOG_TAG, "Username updated.");
                                        goToSetProfilePictureActivity();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Failed to set username", Toast.LENGTH_SHORT).show();
                                        Log.e(LOG_TAG, "Update error: " + e.getMessage());
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error checking username", Toast.LENGTH_SHORT).show();
                        Log.e(LOG_TAG, "Firestore query failed: " + e.getMessage());
                    });

        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(LOG_TAG, "Exception: " + e.getMessage());
        }
    }
}
