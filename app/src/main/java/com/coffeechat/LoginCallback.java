package com.coffeechat;

import com.google.firebase.auth.FirebaseUser;

public interface LoginCallback {
    void onSuccess(FirebaseUser currentUser);
    void onFailure(Exception exception);
}