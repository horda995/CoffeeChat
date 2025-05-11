package com.coffeechat;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.animation.BounceInterpolator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class MainActivity extends AppCompatActivity {
    private FirebaseUser currentUser;
    ObjectAnimator bounceAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Set up the layout and animation
        ConstraintLayout mainLayout = findViewById(R.id.setUsernameMain);
        mainLayout.setTranslationY(-300f);  // Start position for the animation

        // ObjectAnimator to animate the translationY property to 0 (bounce effect)
        bounceAnimator = ObjectAnimator.ofFloat(mainLayout, "translationY", 0f);
        bounceAnimator.setDuration(1200);
        bounceAnimator.setInterpolator(new BounceInterpolator());

        // Add listener to detect when the animation ends
        bounceAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(@NonNull Animator animation) {
                // No action needed here
            }

            @Override
            public void onAnimationEnd(@NonNull Animator animation) {
                // After the animation finishes, check if the user is logged in
                if (currentUser != null) {
                    moveToChatListActivity();  // Move to ChatListActivity if logged in
                } else {
                    moveToLoginActivity();  // Move to LoginActivity if not logged in
                }
            }

            @Override
            public void onAnimationCancel(@NonNull Animator animation) {
                if (currentUser != null) {
                    moveToChatListActivity();
                } else {
                    moveToLoginActivity();
                }
            }

            @Override
            public void onAnimationRepeat(@NonNull Animator animation) {
                // No action needed here
            }
        });

        // Start the animation
        bounceAnimator.start();
    }

    // Method to navigate to LoginActivity
    public void moveToLoginActivity() {
        Intent moveToLoginScreen = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(moveToLoginScreen);
        finish();  // Finish MainActivity so the user can't go back
    }

    // Method to navigate to ChatListActivity
    public void moveToChatListActivity() {
        Intent moveToChatListScreen = new Intent(MainActivity.this, ChatListActivity.class);
        startActivity(moveToChatListScreen);
        finish();  // Finish MainActivity so the user can't go back
    }

    // Touch event handling (optional, based on your requirements)
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (bounceAnimator != null && bounceAnimator.isRunning()) {
                bounceAnimator.cancel();
            }
            return true;
        }
        return super.onTouchEvent(event);
    }
}