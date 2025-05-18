package com.coffeechat;

public interface FirestoreReadFieldCallback {
    void onFieldRetrieved(Object value);
    void onError(Exception e);
}
