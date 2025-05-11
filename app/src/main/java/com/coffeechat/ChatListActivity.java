package com.coffeechat;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;


public class ChatListActivity extends AppCompatActivity {

    private static final String LOG_TAG = ChatListActivity.class.getName();
    ImageView settingsIcon;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.setUsernameMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        settingsIcon = findViewById(R.id.settingsIcon);
        ConstraintLayout grid = findViewById(R.id.setUsernameMain);
        grid.setVisibility(View.INVISIBLE);
        TextView welcomeText = findViewById(R.id.usernameLabel);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            FirebaseFirestore.getInstance().collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            if (username != null) {
                                welcomeText.setText(getString(R.string.usernameLabelString, username));
                            } else {
                                welcomeText.setText(getString(R.string.welcome));
                            }
                            grid.setVisibility(View.VISIBLE);
                        }
                    })
                    .addOnFailureListener(e -> {
                        welcomeText.setText(getString(R.string.welcome));
                        grid.setVisibility(View.VISIBLE);
                        Log.e(LOG_TAG, "Error fetching username: " + e.getMessage());
                    });
        } else {
            Log.d(LOG_TAG, "UNAUTHENTICATED USER!");
            finish();
        }
    }
    public void moveToLoginActivity() {
        Intent moveToLoginScreen = new Intent(ChatListActivity.this, LoginActivity.class);
        moveToLoginScreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(moveToLoginScreen);
        finish();
    }
    public void buttonLogOutOnClick(View view) {
        try {
            FirebaseAuth.getInstance().signOut();
            Log.d(LOG_TAG, "User signed out.");
            moveToLoginActivity();
        } catch(Exception e) {
            Log.e(LOG_TAG, "Sign out failed: " + e.getMessage(), e);
            Toast.makeText(ChatListActivity.this, "Logout failed. Please try again.", Toast.LENGTH_LONG).show();
        }

    }

    public void SpinAnimation() {
        ObjectAnimator rotateAnimator = ObjectAnimator.ofFloat(settingsIcon, "rotation", 0f, 360f);
        rotateAnimator.setDuration(400);

        rotateAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                //ide az intent
            }
        });

        rotateAnimator.start(); // Start the animation
    }

    public void newChatIconOnClick(View view) {
    }

    public void settingsIconOnClick(View view) {
        SpinAnimation();
    }
}