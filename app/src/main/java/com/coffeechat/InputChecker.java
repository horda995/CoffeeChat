package com.coffeechat;

import android.util.Log;

public class InputChecker {
    static String LOG_TAG = "InputChecker";
    //Ezzel a fugvennyel vizsgaljuk meg, ures-e egy input mezo
    public static void validateNotNullOrEmpty(String value, String fieldName) {
        if (value == null || value.isEmpty()) {
            Log.e(LOG_TAG, fieldName + " cannot be empty!");
            throw new IllegalArgumentException(fieldName + " cannot be null or empty.");
        }
    }
}
