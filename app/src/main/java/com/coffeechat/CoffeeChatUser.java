package com.coffeechat;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class CoffeeChatUser {

    private static CoffeeChatUser instance;
    private String email;
    private String username;
    private String uid;
    private Bitmap avatarPicture;
    private Uri avatarUri;

    private List<String> chatList = new ArrayList<>();

    private CoffeeChatUser() {}

    public static synchronized CoffeeChatUser getInstance() {
        if (instance == null) {
            instance = new CoffeeChatUser();
        }
        return instance;
    }

    public static void deleteCoffeeChatUserInstance() {
        if (instance != null) {
            instance.email = null;
            instance.uid = null;
            instance.username = null;
            instance.avatarPicture = null;
            instance.avatarUri = null;
            instance.chatList = null;
            instance = null;
        }
    }

    private static ListenerRegistration listenerRegistration;

    protected void startListening(FirebaseFirestore db, String currentUserId, List<String> userChatList, Context context) {
        if (listenerRegistration != null) return;
        Log.d("LISTENER REGISTRATION", "CHATLIST LISTENING STARTED");
        listenerRegistration = db.collection("messages")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w("MessageListener", "Listen failed.", e);
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                DocumentSnapshot doc = dc.getDocument();
                                String chatId = doc.getString("chatId");
                                String messageText = doc.getString("messageText");
                                String sentByUid = doc.getString("sentByUid");
                                String sentByName = doc.getString("sentByName");

                                if (chatId != null && userChatList.contains(chatId) && !currentUserId.equals(sentByUid)) {
                                    //TODO
                                    //showNotification(context, sentByName, messageText);
                                }
                                else if (!userChatList.contains(chatId)) {
                                    Log.e("Chatlist:", "Chatlist does not contain chatid");
                                }
                                else if (chatId == null) {
                                    Log.e("Chatlist:", "chatid null");
                                }
                            }
                        }
                    }
                });
        listenerRegistration = db.collection("users")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w("chatlist", "Failed to update chatlist.", e);
                        return;
                    }
                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.MODIFIED) {
                                DocumentSnapshot doc = dc.getDocument();
                                if (doc.contains("chatlist")) {
                                    //noinspection unchecked
                                    List<String> chatList = (List<String>) doc.get("chatlist");
                                    setChatList(chatList);
                                    Log.d("chatlist", "chatlist updated for user: " + doc.getId() + " -> " + this.chatList);
                                }
                            }
                        }
                    }
                });
    }

    protected void stopListening() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    private static void showNotification(Context context, String username, String message) {
        // Replace with real notification logic
        Log.d(username, message);
        Toast.makeText(context, username + ": " + message, Toast.LENGTH_LONG).show();
    }

    public Bitmap getAvatarPicture() {

        return avatarPicture;
    }

    public void setAvatarPicture(Bitmap avatarPicture) {

        this.avatarPicture = avatarPicture;
    }

    public String getUsername() {

        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Uri getAvatarUri() {
        return avatarUri;
    }

    public void setAvatarUri(Uri avatarUri) {
        this.avatarUri = avatarUri;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public List<String> getChatList() {
        return chatList;
    }

    public void setChatList(List<String> chatList) {
        this.chatList = chatList;
    }
}
