package com.coffeechat;

import com.google.firebase.Timestamp;

public class OtherUser {
    private String username;
    private String uid;
    private String avatarUrl;
    private String lastMessage;
    private String chatId;
    private Timestamp timestamp;

    public OtherUser(String username, String avatarUrl, String uid, String lastMessage, String chatId, Timestamp timestamp) {
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.uid = uid;
        this.lastMessage = lastMessage;
        this.chatId = chatId;
        this.timestamp = timestamp;
    }

    public String getUserName() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }
}
