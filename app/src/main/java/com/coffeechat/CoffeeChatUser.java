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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoffeeChatUser {

    private static CoffeeChatUser instance;

    private String email;
    private String username;
    private String uid;
    private Bitmap avatarPicture;
    private Uri avatarUri;
    private List<String> chatList = new ArrayList<>();

    // Singleton constructor
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

    // Getters and setters
    public Bitmap getAvatarPicture() { return avatarPicture; }
    public void setAvatarPicture(Bitmap avatarPicture) { this.avatarPicture = avatarPicture; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Uri getAvatarUri() { return avatarUri; }
    public void setAvatarUri(Uri avatarUri) { this.avatarUri = avatarUri; }
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public List<String> getChatList() { return chatList; }
    public void setChatList(List<String> chatList) { this.chatList = chatList; }

    public interface OnNewMessageListener {
        void onNewMessageReceived(String chatId, String messageText);
    }

    private OnNewMessageListener messageListener;

    public void setOnNewMessageListener(OnNewMessageListener listener) {
        this.messageListener = listener;
    }


    private final Map<String, ListenerRegistration> messageListeners = new HashMap<>();
    private final Map<String, Boolean> initialSnapshotLoaded = new HashMap<>();

    public void startListening(FirebaseFirestore db, String currentUserId, List<String> userChatList, Context context) {
        if (!messageListeners.isEmpty()) {
            Log.d("CoffeeChatUser", "Already listening to chats, ignoring startListening call.");
            return;
        }

        Log.d("CoffeeChatUser", "Starting listeners for chats: " + userChatList);
        for (String chatId : userChatList) {
            initialSnapshotLoaded.put(chatId, false);

            ListenerRegistration listener = db.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .addSnapshotListener((snapshots, e) -> {
                        if (e != null) {
                            Log.w("CoffeeChatUser", "Listen failed for chat: " + chatId, e);
                            return;
                        }
                        if (snapshots != null && !snapshots.isEmpty()) {
                            boolean isInitialLoad = Boolean.FALSE.equals(initialSnapshotLoaded.get(chatId));

                            for (DocumentChange dc : snapshots.getDocumentChanges()) {
                                if (dc.getType() == DocumentChange.Type.ADDED) {
                                    DocumentSnapshot doc = dc.getDocument();
                                    String messageText = doc.getString("messageText");
                                    String sentByUid = doc.getString("sentByUid");

                                    Log.d("CoffeeChatUser", "New message in chatId: " + chatId +
                                            ", messageText: " + messageText +
                                            ", sentByUid: " + sentByUid);

                                    if (!isInitialLoad && sentByUid != null && !currentUserId.equals(sentByUid)) {
                                        showNewMessageNotification(context, messageText);
                                        if (messageListener != null) {
                                            messageListener.onNewMessageReceived(chatId, messageText);
                                        }
                                    }
                                }
                            }

                            if (isInitialLoad) {
                                initialSnapshotLoaded.put(chatId, true);
                            }
                        }
                    });

            messageListeners.put(chatId, listener);
        }
    }


    public void stopListening() {
        for (Map.Entry<String, ListenerRegistration> entry : messageListeners.entrySet()) {
            ListenerRegistration listener = entry.getValue();
            if (listener != null) {
                listener.remove();
            }
        }
        messageListeners.clear();
        Log.d("CoffeeChatUser", "All chat listeners stopped.");
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
}
