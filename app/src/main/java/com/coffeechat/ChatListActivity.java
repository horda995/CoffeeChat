package com.coffeechat;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;


public class ChatListActivity extends AppCompatActivity {

    private static final String LOG_TAG = ChatListActivity.class.getName();
    private ImageView settingsIcon;

    RecyclerView recyclerView;
    ChatListAdapter adapter;


    FirebaseAuth mAuth;
    FirebaseUser mUser;
    FirebaseFirestore mDatabase;

    protected List<OtherUser> chatList = new ArrayList<>();
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
        recyclerView = findViewById(R.id.chatListRecyclerView);
        adapter = new ChatListAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);
        CoffeeChatUser.getInstance().setOnNewMessageListener((chatId, messageText) -> {
            new Handler(Looper.getMainLooper()).post(() -> {
                adapter.updateLastMessage(chatId, messageText);
            });
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
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
        FirebaseUtils.readFieldFromFirestore(mDatabase, "users", mUser.getUid(), "username", new FirestoreReadFieldCallback() {
            @Override
            public void onFieldRetrieved(Object value) {
                String usernameValue = value.toString();
                coffeeChatUser.setUsername(usernameValue);
                username.setText(coffeeChatUser.getUsername());
            }

            @Override
            public void onError(Exception e) {

            }
        });
        FirebaseUtils.readFieldFromFirestore(mDatabase, "users", mUser.getUid(), "chatlist", new FirestoreReadFieldCallback() {
            @Override
            public void onFieldRetrieved(Object value) {
                if (value instanceof List<?>) {
                    ArrayList<String> chatList = new ArrayList<>();
                    for (Object item : (List<?>) value) {
                        if (item instanceof String) {
                            chatList.add((String) item);
                        }
                    }
                    coffeeChatUser.setChatList(chatList);
                    loadChatPreviews();
                } else {
                    Log.e(LOG_TAG, "chatlist field is not a List");
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(LOG_TAG, "Error reading chatlist: " + e);
            }
        });

    }

    /** @noinspection unchecked*/
    private void loadChatPreviews() {
        List<OtherUser> previewList = new ArrayList<>();

        mDatabase.collection("chats")
                .whereArrayContains("uid", coffeeChatUser.getUid())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Log.d("fetchChats", "No chats found.");
                        return;
                    }

                    for (DocumentSnapshot chatDoc : querySnapshot.getDocuments()) {
                        List<String> uidList = (List<String>) chatDoc.get("uid");

                        if (uidList == null || uidList.size() != 2) continue;

                        String otherUid = uidList.get(0).equals(coffeeChatUser.getUid()) ? uidList.get(1) : uidList.get(0);
                        String chatId = chatDoc.getId();

                        mDatabase.collection("messages")
                                .whereEqualTo("chatId", chatId)
                                .orderBy("timestamp", Query.Direction.DESCENDING)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(messageSnapshot -> {
                                    final String lastMessageText;
                                    if (!messageSnapshot.isEmpty()) {
                                        DocumentSnapshot lastMessageDoc = messageSnapshot.getDocuments().get(0);
                                        lastMessageText = lastMessageDoc.getString("messageText");
                                    } else {
                                        lastMessageText = "No messages yet";
                                    }

                                    mDatabase.collection("users")
                                            .document(otherUid)
                                            .get()
                                            .addOnSuccessListener(userDoc -> {
                                                if (userDoc.exists()) {
                                                    String username = userDoc.getString("username");
                                                    String avatar = userDoc.getString("avatarPicture");

                                                    previewList.add(new OtherUser(username, avatar, otherUid, lastMessageText, chatId));
                                                    chatList.clear();
                                                    chatList.addAll(previewList);
                                                    adapter.updateList(chatList);

                                                }
                                            });
                                });
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