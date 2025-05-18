package com.coffeechat;

public class OtherUser {
    private String username;
    private String uid;
    private String avatarUrl;
    private String lastMessage;

    public OtherUser(String username, String avatarUrl, String uid, String lastMessage) {
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.uid = uid;
    }

    public String getUserName() {
        return username;
    }

    public void N(String username) {
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
}