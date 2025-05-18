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
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
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
                    coffeeChatUser.startListening(mDatabase, mUser.getUid(), coffeeChatUser.getChatList(), ChatListActivity.this);
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

    private void loadChatPreviews() {

        if (mUser == null) return;
        mDatabase.collection("users").document(mUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    //noinspection unchecked
                    List<String> chatlist = (List<String>) documentSnapshot.get("chatlist");
                    if (chatlist != null) {
                        fetchChats(chatlist);
                    }
                });
    }

    private void fetchChats(List<String> chatIds) {
        if (chatIds == null || chatIds.isEmpty()) {
            Log.d("fetchChats", "chatId list is empty");
            return;
        }

        List<OtherUser> previewList = new ArrayList<>();
        int[] completedCount = {0};

        for (String chatId : chatIds) {
            Log.d("fetchChats", "Fetching last message for chatId: " + chatId);
            mDatabase.collection("messages")
                    .whereEqualTo("chatId", chatId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot messageDoc = querySnapshot.getDocuments().get(0);
                            String messageText = messageDoc.getString("messageText");
                            String sentByUid = messageDoc.getString("sentByUid");
                            String sentByName = messageDoc.getString("sentByName");
                            Log.d("fetchChats", "Message found: " + messageText + " by " + sentByName + "(" + sentByUid + ")");
                            if (sentByUid != null) {
                                mDatabase.collection("users").document(sentByUid).get()
                                        .addOnSuccessListener(userDoc -> {
                                            String name = userDoc.getString("username");
                                            String avatarUrl = userDoc.getString("avatarPicture");
                                            Log.d("fetchChats", "User fetched: " + name);
                                            previewList.add(new OtherUser(name, avatarUrl, chatId, messageText));
                                            checkAndUpdateAdapter(previewList, ++completedCount[0], chatIds.size());
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("fetchChats", "Failed to fetch user: " + sentByUid, e);
                                            checkAndUpdateAdapter(previewList, ++completedCount[0], chatIds.size());
                                        });
                            } else {
                                Log.d("fetchChats", "sentByUid is null");
                                checkAndUpdateAdapter(previewList, ++completedCount[0], chatIds.size());
                            }
                        } else {
                            Log.d("fetchChats", "No messages for chatId: " + chatId);
                            checkAndUpdateAdapter(previewList, ++completedCount[0], chatIds.size());
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("fetchChats", "Failed to fetch message for chatId: " + chatId, e);
                        checkAndUpdateAdapter(previewList, ++completedCount[0], chatIds.size());
                    });
        }
    }

    private void checkAndUpdateAdapter(List<OtherUser> list, int completed, int total) {
        Log.d("fetchChats", "Completed " + completed + "/" + total);
        if (completed == total) {
            Log.d("fetchChats", "All fetches done. Updating adapter with " + list.size() + " items.");
            adapter.updateList(list);
        }
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