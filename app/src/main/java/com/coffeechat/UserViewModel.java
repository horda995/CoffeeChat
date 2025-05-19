package com.coffeechat;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;


import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class UserViewModel extends ViewModel {
    private final MutableLiveData<List<OtherUser>> userList = new MutableLiveData<>();
    private ListenerRegistration userListener;
    public LiveData<List<OtherUser>> getUserList() {
        return userList;
    }

    private final MutableLiveData<List<OtherUser>> chatList = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<OtherUser>> getChatList() {
        return chatList;
    }
    private final Map<String, OtherUser> chatListMap = new HashMap<>();
    private ListenerRegistration chatListListener;

    public void startListeningChatList(FirebaseFirestore db, String currentUserUid) {
        db.collection("chats")
                .whereArrayContains("uid", currentUserUid)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    for (DocumentSnapshot chatDoc : querySnapshots) {
                        String chatId = chatDoc.getId();
                        chatListListen(chatId, db, currentUserUid);
                    }
                });
    }

    /** @noinspection unchecked*/
    private void chatListListen(String chatId, FirebaseFirestore db, String currentUserUid) {
        chatListListener= db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || snapshot.isEmpty()) return;

                    for (DocumentChange dc : snapshot.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            DocumentSnapshot msg = dc.getDocument();
                            String messageText = msg.getString("messageText");
                            Timestamp timestamp = msg.getTimestamp("timestamp");
                            db.collection("chats").document(chatId).get().addOnSuccessListener(chatDoc -> {
                                String chatName = chatDoc.getString("name");
                                List<String> uidList = (List<String>) chatDoc.get("uid");

                                if (chatName != null && !chatName.isEmpty()) {
                                    // Group chat
                                    chatListMap.put(chatId, new OtherUser(chatName, null, chatId, messageText, chatId, timestamp));
                                    chatList.setValue(
                                            chatListMap.values().stream()
                                                    .sorted((o1, o2) -> o2.getTimestamp().compareTo(o1.getTimestamp()))
                                                    .collect(Collectors.toCollection(ArrayList::new))
                                    );
                                } else if (uidList != null && uidList.size() == 2) {
                                    String otherUid = uidList.get(0).equals(currentUserUid) ? uidList.get(1) : uidList.get(0);
                                    db.collection("users").document(otherUid).get().addOnSuccessListener(userDoc -> {
                                        String username = userDoc.getString("username");
                                        String avatar = userDoc.getString("avatarPicture");

                                        Timestamp timestampSafe = timestamp != null ? timestamp : Timestamp.now();
                                        chatListMap.put(chatId, new OtherUser(username, avatar, otherUid, messageText, chatId, timestampSafe));
                                        chatList.setValue(
                                                chatListMap.values().stream()
                                                        .sorted((o1, o2) -> {
                                                            Timestamp t1 = o1.getTimestamp();
                                                            Timestamp t2 = o2.getTimestamp();
                                                            if (t1 == null && t2 == null) return 0;
                                                            if (t1 == null) return 1;
                                                            if (t2 == null) return -1;
                                                            return t2.compareTo(t1);
                                                        })
                                                        .collect(Collectors.toCollection(ArrayList::new))
                                        );
                                    });
                                }
                            });
                        }
                    }
                });
    }


     public void startListeningForUsers(FirebaseFirestore db, String excludeUsername) {
        if (userListener != null) {
            userListener.remove();
        }

        userListener = db.collection("users")
                .whereNotEqualTo("username", excludeUsername)
                .orderBy("username")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e("UserViewModel", "Listen failed: ", error);
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        List<OtherUser> users = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            String username = doc.getString("username");
                            String avatarUrl = doc.getString("avatarPicture");
                            String uid = doc.getString("uid");
                            users.add(new OtherUser(username, avatarUrl, uid, null, null, null));
                        }
                        userList.setValue(users);
                    } else {
                        userList.setValue(new ArrayList<>());
                    }
                });
    }

    private final MutableLiveData<List<OtherUser>> filteredUsers = new MutableLiveData<>();
    public LiveData<List<OtherUser>> getFilteredUsers() { return filteredUsers; }
    public void filterUsersByUsername(String query) {
        List<OtherUser> currentList = userList.getValue();
        if (currentList != null) {
            List<OtherUser> filtered = new ArrayList<>();
            for (OtherUser user : currentList) {
                if (user.getUserName().toLowerCase().contains(query.toLowerCase())) {
                    filtered.add(user);
                }
            }
            filteredUsers.setValue(filtered);
        }
    }

    private final MutableLiveData<List<ChatMessages>> chatMessages = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<ChatMessages>> getChatMessages() {
        return chatMessages;
    }
    private ListenerRegistration chatListener;

    public void startListeningChat(FirebaseFirestore db, String chatId) {
        chatListener = db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(100)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e("UserViewModel", "Listen failed: ", error);
                        return;
                    }
                    if (snapshots != null && !snapshots.isEmpty()) {
                        List<ChatMessages> messages = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            String messageText = doc.getString("messageText");
                            String sentByUid = doc.getString("sentByUid");
                            String soundUrl = doc.getString("soundUrl");
                            String messageChatId = doc.getString("chatId");
                            Timestamp timestamp = doc.getTimestamp("timestamp");
                            messages.add(new ChatMessages(messageChatId, messageText, sentByUid, soundUrl, timestamp));
                        }
                        chatMessages.setValue(messages);
                    } else {
                        chatMessages.setValue(new ArrayList<>());
                    }
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (userListener != null) {
            userListener.remove();
        }
        if (chatListListener != null) {
            chatListListener.remove();
        }
        if (chatListener != null) {
            chatListener.remove();
        }
    }


}
