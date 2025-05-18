package com.coffeechat;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

public class NavigationUtils {

    public static void moveToLoginActivity(Context context) {
        Intent moveToLogin = new Intent(context, LoginActivity.class);
        moveToLogin.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(moveToLogin);
        if (context instanceof Activity) {
            ((Activity) context).finish();
        }

    }
    public static void moveToActivity(Activity activity, Class<?> targetActivity) {
        Intent movToTargetActivity = new Intent(activity, targetActivity);
        activity.startActivity(movToTargetActivity);
        activity.finish();
    }

    public static void moveToChatActivity(Activity activity, String username, String avatarUrl, String uid) {
        Intent moveToChat = new Intent(activity, ChatActivity.class);
        moveToChat.putExtra("username", username);
        moveToChat.putExtra("avatarUrl", avatarUrl);
        moveToChat.putExtra("uid", uid);
        activity.startActivity(moveToChat);
        activity.finish();
    }

}