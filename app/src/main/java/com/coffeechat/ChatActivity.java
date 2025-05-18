package com.coffeechat;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;

public class ChatActivity extends AppCompatActivity {

    private static final String LOG_TAG = ChatActivity.class.getName();

    String username;
    String avatarUrl;
    String otherUserUid;
    ShapeableImageView avatarImageView;
    TextView chatUsernameTextView;
    EditText chatInput;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    FirebaseFirestore mDatabase;
    CoffeeChatUser coffeeChatUser = CoffeeChatUser.getInstance();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.chatMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        username = getIntent().getStringExtra("username");
        avatarUrl = getIntent().getStringExtra("avatarUrl");
        otherUserUid = getIntent().getStringExtra("uid");
        chatInput = findViewById(R.id.chatInput);
        avatarImageView = findViewById(R.id.chatAvatarImageView);
        chatUsernameTextView = findViewById(R.id.chatUsernameLabel);
        chatUsernameTextView.setText(username);
        Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.coffee_default_avatar)
                .into(avatarImageView);
    }

    protected void onStart() {
        super.onStart();
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        FirebaseUtils.checkLogin(ChatActivity.this);
        mDatabase = FirebaseFirestore.getInstance();
        FirebaseUtils.checkAndUpdateEmailIfNeeded(ChatActivity.this, mDatabase, mAuth, LOG_TAG);
    }


    public void friendsIconOnClick(View view) {
        NavigationUtils.moveToActivity(ChatActivity.this, PeopleActivity.class);
    }

    public void chatListIconOnClick(View view) {
        NavigationUtils.moveToActivity(ChatActivity.this, ChatListActivity.class);
    }
    private String chatIdGenerator() {
        String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        int LENGTH = 20;
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        String generated = "chat_" + sb.toString() + "_id";
        Log.d("chatIdGenerator", "Generated chatId: " + generated);
        return generated;
    }
    public void sendMessageOnClick(View view) {
        if(!chatInput.getText().toString().trim().isEmpty()) {
            String soundUrl = "lol";
            String messageText = chatInput.getText().toString().trim();
            FirebaseUtils.checkIfChatWithUserExists(mDatabase, coffeeChatUser.getUid(), otherUserUid, new FirebaseChatIdCallback() {
                @Override
                public void onChatIdFound(String chatId) {
                    FirebaseUtils.addMessageToFirestore(mDatabase, chatId, coffeeChatUser.getUid(), messageText, soundUrl, chatInput);
                }
                @Override
                public void onChatNotFound() {
                    ArrayList<String> uidList = new ArrayList<>();
                    if(coffeeChatUser.getUid() != null && otherUserUid != null) {
                        uidList.add(coffeeChatUser.getUid());
                        uidList.add(otherUserUid);
                        String chatId = chatIdGenerator();
                        FirebaseUtils.addChatToChatCollection(mDatabase, chatId, uidList, () -> {
                            FirebaseUtils.addMessageToFirestore(mDatabase, chatId, coffeeChatUser.getUid(), messageText, soundUrl, chatInput);
                        });
                    }

                }
                @Override
                public void onError(Exception e) {
                    Log.d(LOG_TAG, e.toString());
                }
            });
        }
        else {
            Log.w("chatInput", "Message is empty");
        }
    }
}