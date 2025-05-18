package com.coffeechat;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class UserViewModel extends ViewModel {
    private final MutableLiveData<List<OtherUser>> userList = new MutableLiveData<>();
    private ListenerRegistration userListener;

    public LiveData<List<OtherUser>> getUserList() {
        return userList;
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
                            users.add(new OtherUser(username, avatarUrl, uid, "Not here"));
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

    @Override
    protected void onCleared() {
        super.onCleared();
        if (userListener != null) {
            userListener.remove();
        }
    }
}
