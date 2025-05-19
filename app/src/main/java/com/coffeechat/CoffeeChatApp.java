package com.coffeechat;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class CoffeeChatApp extends Application {
    public void onCreate() {

        super.onCreate();

        CoffeeChatUser coffeeChatUser = CoffeeChatUser.getInstance();

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser mUser = mAuth.getCurrentUser();
        FirebaseFirestore mDatabase = FirebaseFirestore.getInstance();
        createNotificationChannel();
        if (mUser != null) {
            coffeeChatUser.setUid(mUser.getUid());
            FirebaseUtils.checkAndUpdateEmailIfNeeded(getApplicationContext(), mDatabase, mAuth, "Email-check");
            mDatabase.collection("users").document(coffeeChatUser.getUid()).get().addOnSuccessListener(documentSnapshot -> {
                //noinspection unchecked
                List<String> chatlist = (List<String>) documentSnapshot.get("chatlist");
                if (chatlist != null) {
                    Log.d("CoffeeChatApp", "Chatlist not NULL");
                    coffeeChatUser.startListening(mDatabase, coffeeChatUser.getUid(), chatlist, getApplicationContext());
                }
            });
        }
    }

    private void createNotificationChannel() {
        String channelId = "messages_channel";
        CharSequence name = "Message Notifications";
        String description = "Notifications for new messages";
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel(channelId, name, importance);
        channel.setDescription(description);
        channel.enableVibration(true);
        channel.enableLights(true);
        channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build());

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
