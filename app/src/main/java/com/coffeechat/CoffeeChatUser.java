package com.coffeechat;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

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

    public interface OnNewMessageListener {
        void onNewMessageReceived(String chatId, String messageText);
    }

    private OnNewMessageListener messageListener;

    public void setOnNewMessageListener(OnNewMessageListener listener) {
        this.messageListener = listener;
    }

    private void showNewMessageNotification(Context context, String messageText) {
        String channelId = "messages_channel";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.coffee_mug)
                .setContentTitle("New Message")
                .setContentText(messageText != null ? messageText : "You received a new message.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
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

                                if (chatId != null && userChatList.contains(chatId) && !currentUserId.equals(sentByUid)) {
                                    if (messageListener != null) {
                                        messageListener.onNewMessageReceived(chatId, messageText);
                                        showNewMessageNotification(context, messageText);
                                    }
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


}
