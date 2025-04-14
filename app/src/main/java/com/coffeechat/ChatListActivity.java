package com.coffeechat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChatListActivity extends AppCompatActivity {
    private static final String LOG_TAG = ChatListActivity.class.getName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        //Kijelzi a bejelentkezett user email cimet
        FirebaseUser user;
        user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null) {
            TextView welcomeText = findViewById(R.id.welcomeText);
            welcomeText.setText(user.getEmail());
            Log.d(LOG_TAG, "User is authenticated.");
        } else {
            Log.d(LOG_TAG, "UNAUTHENTICATED USER!");
            finish();
        }
    }
    public void moveToMainActivity() {
        Intent moveToMainScreen = new Intent(ChatListActivity.this, MainActivity.class);
        moveToMainScreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(moveToMainScreen);
        finish();
    }
    public void buttonLogOutOnClick(View view) {
        try {
            FirebaseAuth.getInstance().signOut();
            Log.d(LOG_TAG, "User signed out.");
            moveToMainActivity();
        } catch(Exception e) {
            Log.e(LOG_TAG, "Sign out failed: " + e.getMessage(), e);
            Toast.makeText(ChatListActivity.this, "Logout failed. Please try again.", Toast.LENGTH_LONG).show();
        }

    }
}