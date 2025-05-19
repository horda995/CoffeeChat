package com.coffeechat;


import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.BounceInterpolator;

import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private final static String LOG_TAG = MainActivity.class.getName();

    /** @noinspection FieldCanBeLocal*/
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private FirebaseFirestore mDatabase;
    private ObjectAnimator bounceAnimator;

    CoffeeChatUser coffeeChatUser = CoffeeChatUser.getInstance();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ConstraintLayout mainLayout = findViewById(R.id.setUsernameMain);
        mainLayout.setTranslationY(-300f);
        configureBounceAnimator(mainLayout);
        scheduleChatNotificationAlarm(this);
    }

    protected void onStart(){
        super.onStart();
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        mDatabase = FirebaseFirestore.getInstance();
        bounceAnimator.start();
    }


    public void scheduleChatNotificationAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager == null) {
            Log.e(LOG_TAG, "AlarmManager is null, cannot schedule alarm");
            return;
        }

        Intent intent = new Intent(context, ChatNotificationReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long interval = 60 * 1000L;
        long startTime = System.currentTimeMillis() + interval;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startTime, alarmIntent);
                    Log.d(LOG_TAG, "Chat notification alarm scheduled (exact)");
                } catch (SecurityException e) {
                    Log.e(LOG_TAG, "SecurityException scheduling exact alarm", e);
                    // Fallback to inexact alarm
                    alarmManager.set(AlarmManager.RTC_WAKEUP, startTime, alarmIntent);
                    Log.d(LOG_TAG, "Chat notification alarm scheduled (inexact fallback)");
                }
            } else {
                Log.w(LOG_TAG, "Cannot schedule exact alarms, scheduling inexact alarm instead");
                alarmManager.set(AlarmManager.RTC_WAKEUP, startTime, alarmIntent);
            }
        } else {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startTime, alarmIntent);
                Log.d(LOG_TAG, "Chat notification alarm scheduled (pre-API31)");
            } catch (SecurityException e) {
                Log.e(LOG_TAG, "SecurityException scheduling exact alarm pre-API31", e);
                alarmManager.set(AlarmManager.RTC_WAKEUP, startTime, alarmIntent);
                Log.d(LOG_TAG, "Chat notification alarm scheduled (inexact fallback pre-API31)");
            }
        }
    }

    private void checkLoginOnMainActivity(){
        if (mUser != null && !mUser.getUid().isEmpty()) {
            scheduleChatNotificationAlarm(this);
            FirebaseUtils.readFieldFromFirestore(mDatabase, "users", mUser.getUid(), "username", new FirebaseUtils.FirestoreReadFieldCallback() {
                public void onFieldRetrieved(Object value) {
                    if (value != null) {
                        String username = value.toString();
                        coffeeChatUser.setUsername(username);
                        coffeeChatUser.setUid(mUser.getUid());
                        Log.d(LOG_TAG, "Username exists, moving on to ChatlistActivity. Username: " + username);
                        NavigationUtils.moveToActivity(MainActivity.this, ChatListActivity.class);
                    }
                    else {
                        Log.w(LOG_TAG, "Username does not exist, moving on to SetUserNameActivity");
                        NavigationUtils.moveToActivity(MainActivity.this, SetUsernameActivity.class);
                    }
                }
                @Override
                public void onError(Exception e) {
                    Log.e(LOG_TAG, "Error fetching field", e);
                    Log.w(LOG_TAG, "Username does not exist, moving on to SetUserNameActivity");
                    NavigationUtils.moveToActivity(MainActivity.this, SetUsernameActivity.class);
                }
            });
        }
        else {
            Log.d(LOG_TAG, "User does not exist, moving on to LoginActivity");
            NavigationUtils.moveToActivity(MainActivity.this, LoginActivity.class);
        }
    }

    private void loadDataDuringAnimation() {
        if (mUser != null && !mUser.getUid().isEmpty()) {
            FirebaseUtils.readFieldFromFirestore(mDatabase, "users", mUser.getUid(), "email", new FirebaseUtils.FirestoreReadFieldCallback() {
                @Override
                public void onFieldRetrieved(Object value) {
                    String email = value.toString();
                    coffeeChatUser.setEmail(email);
                }

                @Override
                public void onError(Exception e) {
                    Log.e(LOG_TAG, "Error reading email: " + e);
                }
            });

            //Nincs imageView, nincs problmema
            ImageUtils.loadAvatar(MainActivity.this, mUser, null, coffeeChatUser, LOG_TAG);
        }
    }

    private void configureBounceAnimator(ConstraintLayout layout)
    {
        bounceAnimator = ObjectAnimator.ofFloat(layout, "translationY", 0f);
        bounceAnimator.setDuration(1200);
        bounceAnimator.setInterpolator(new BounceInterpolator());

        bounceAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(@NonNull Animator animation) {
                loadDataDuringAnimation();
            }

            @Override
            public void onAnimationEnd(@NonNull Animator animation) {
                checkLoginOnMainActivity();
            }

            @Override
            public void onAnimationCancel(@NonNull Animator animation) {
                //
            }

            @Override
            public void onAnimationRepeat(@NonNull Animator animation) {
                // Ennek a léte nélkül rinyál az IDE
            }
        });
    }

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