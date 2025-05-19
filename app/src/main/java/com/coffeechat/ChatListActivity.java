package com.coffeechat;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ChatListActivity extends AppCompatActivity {

    private static final String LOG_TAG = ChatListActivity.class.getName();
    private ImageView settingsIcon;
    RecyclerView ChatListRecyclerView;
    ChatListAdapter adapter;
    private UserViewModel chatListViewModel;
    FirebaseAuth mAuth;
    FirebaseUser mUser;
    FirebaseFirestore mDatabase;
    CoffeeChatUser coffeeChatUser = CoffeeChatUser.getInstance();
    ShapeableImageView avatarImageView;
    TextView username;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.chatListMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        chatListViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        ChatListRecyclerView = findViewById(R.id.chatListRecyclerView);
        adapter = new ChatListAdapter();
        adapter.setOnItemClickListener(user -> {
            Log.d("username: ", user.getUserName());
            Log.d("avatar:", user.getAvatarUrl());
            Log.d("otherUid:", user.getUid());
            Log.d("chatId:", user.getChatId());
            NavigationUtils.moveToChatActivity(ChatListActivity.this, user.getUserName(), user.getAvatarUrl(), user.getUid(), user.getChatId());
        });
        ChatListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ChatListRecyclerView.setAdapter(adapter);
        settingsIcon = findViewById(R.id.settingsIcon);
        avatarImageView = findViewById(R.id.AvatarImageView);
        username = findViewById(R.id.usernameLabel);
    }

    protected void onStart() {
        super.onStart();
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        FirebaseUtils.checkLogin(ChatListActivity.this);
        mDatabase = FirebaseFirestore.getInstance();
        FirebaseUtils.checkAndUpdateEmailIfNeeded(ChatListActivity.this, mDatabase, mAuth, LOG_TAG);
        ImageUtils.loadAvatar(ChatListActivity.this, mUser, avatarImageView, coffeeChatUser, LOG_TAG);
        FirebaseUtils.readFieldFromFirestore(mDatabase, "users", mUser.getUid(), "username", new FirebaseUtils.FirestoreReadFieldCallback() {
            @Override
            public void onFieldRetrieved(Object value) {
                String usernameValue = value.toString();
                coffeeChatUser.setUsername(usernameValue);
                username.setText(coffeeChatUser.getUsername());
                chatListViewModel.getChatList().observe(ChatListActivity.this, users -> {
                    adapter.submitList(users);
                });
                chatListViewModel.startListeningChatList(mDatabase, coffeeChatUser.getUid());
            }

            @Override
            public void onError(Exception e) {

            }
        });

    }


    private void SpinAnimation() {
        ObjectAnimator rotateAnimator = ObjectAnimator.ofFloat(settingsIcon, "rotation", 0f, 360f);
        rotateAnimator.setDuration(400);

        rotateAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                NavigationUtils.moveToActivity(ChatListActivity.this, SettingsActivity.class);
            }
        });

        rotateAnimator.start();
    }

    public void newChatIconOnClick(View view) {

    }

    public void settingsIconOnClick(View view) {
        FirebaseUtils.checkAndUpdateEmailIfNeeded(ChatListActivity.this, mDatabase, mAuth, LOG_TAG);
        SpinAnimation();
    }

    public void friendsIconOnClick(View view) {
        FirebaseUtils.checkAndUpdateEmailIfNeeded(ChatListActivity.this, mDatabase, mAuth, LOG_TAG);
        NavigationUtils.moveToActivity(ChatListActivity.this, PeopleActivity.class);
    }
}