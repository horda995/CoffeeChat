package com.coffeechat;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.animation.BounceInterpolator;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;


public class MainActivity extends AppCompatActivity {

    public void moveToLoginActivity() {
        Intent moveToLoginScreen = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(moveToLoginScreen);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //ANIMACIO
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ConstraintLayout mainLayout = findViewById(R.id.main);
        mainLayout.setTranslationY(-300f);
        ObjectAnimator bounceAnimator = ObjectAnimator.ofFloat(mainLayout, "translationY", 0f);
        bounceAnimator.setDuration(1200);
        bounceAnimator.setInterpolator(new BounceInterpolator());
        bounceAnimator.start();
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            moveToLoginActivity();
            return true;
        }
        return super.onTouchEvent(event);
    }
}