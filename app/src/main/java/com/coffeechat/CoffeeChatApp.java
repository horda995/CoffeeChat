package com.coffeechat;

import android.app.Application;

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

        if (mUser != null) {
            coffeeChatUser.setUid(mUser.getUid());
            FirebaseUtils.checkAndUpdateEmailIfNeeded(getApplicationContext(), mDatabase, mAuth, "Email-check");
            mDatabase.collection("users").document(coffeeChatUser.getUid()).get().addOnSuccessListener(documentSnapshot -> {
                //noinspection unchecked
                List<String> chatlist = (List<String>) documentSnapshot.get("chatlist");
                if (chatlist != null) {
                    coffeeChatUser.startListening(mDatabase, coffeeChatUser.getUid(), chatlist, getApplicationContext());
                }
            });
        }
    }
}
