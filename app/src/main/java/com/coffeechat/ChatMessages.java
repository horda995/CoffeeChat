package com.coffeechat;

import com.google.firebase.Timestamp;


public class ChatMessages {
    private String chatId;
    private String messageText;
    private String sentByUid;
    private String soundUrl;
    private Timestamp timestamp;
    ChatMessages(String chatId, String messageText, String sentByUid, String soundUrl, Timestamp timestamp) {
        this.chatId = chatId;
        this.messageText = messageText;
        this.sentByUid = sentByUid;
        this.soundUrl = soundUrl;
        this.timestamp = timestamp;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public String getSentByUid() {
        return sentByUid;
    }

    public void setSentByUid(String sentByUid) {
        this.sentByUid = sentByUid;
    }

    public String getSoundUrl() {
        return soundUrl;
    }

    public void setSoundUrl(String soundUrl) {
        this.soundUrl = soundUrl;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
