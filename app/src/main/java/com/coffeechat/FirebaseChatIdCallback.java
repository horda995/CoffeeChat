package com.coffeechat;

public interface FirebaseChatIdCallback {
    void onChatIdFound(String chatId);
    void onChatNotFound();
    void onError(Exception e);
}
