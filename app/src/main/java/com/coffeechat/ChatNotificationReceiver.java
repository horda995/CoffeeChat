package com.coffeechat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import android.app.AlarmManager;
import android.app.PendingIntent;


import com.google.firebase.firestore.FirebaseFirestore;

public class ChatNotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("ChatNotificationReceiver", "Alarm triggered, checking for new messages...");


        CoffeeChatUser user = CoffeeChatUser.getInstance();

        if (user.getUid() == null) {
            Log.w("ChatNotificationReceiver", "No user logged in, skipping check");
        } else {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            if (user.getChatList() != null && !user.getChatList().isEmpty()) {
                Log.d("Alarm manager:", "LISTENING TO MESSAGES IN THE BACKGROUND");
                user.startListening(db, user.getUid(), user.getChatList(), context);
            } else {
                Log.d("ChatNotificationReceiver", "User chat list is empty, nothing to listen for.");
            }
        }

        scheduleNextAlarm(context);
    }


    private void scheduleNextAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, ChatNotificationReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long interval = 60 * 1000L;
        long nextTrigger = System.currentTimeMillis() + interval;

        if (alarmManager == null) {
            Log.e("ChatNotificationReceiver", "AlarmManager is null, cannot schedule alarm");
            return;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger, alarmIntent);
                    Log.d("ChatNotificationReceiver", "Rescheduled exact alarm in 1 minute");
                } catch (SecurityException e) {
                    Log.e("ChatNotificationReceiver", "SecurityException: Cannot schedule exact alarm", e);
                    scheduleInexactAlarmFallback(alarmManager, nextTrigger, alarmIntent);
                }
            } else {
                Log.w("ChatNotificationReceiver", "Cannot schedule exact alarms on this device, using inexact alarm");
                scheduleInexactAlarmFallback(alarmManager, nextTrigger, alarmIntent);
            }
        } else {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger, alarmIntent);
                Log.d("ChatNotificationReceiver", "Rescheduled exact alarm in 1 minute (pre-API31)");
            } catch (SecurityException e) {
                Log.e("ChatNotificationReceiver", "SecurityException: Cannot schedule exact alarm", e);
                scheduleInexactAlarmFallback(alarmManager, nextTrigger, alarmIntent);
            }
        }
    }

    private void scheduleInexactAlarmFallback(AlarmManager alarmManager, long triggerAtMillis, PendingIntent alarmIntent) {
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, alarmIntent);
        Log.d("ChatNotificationReceiver", "Scheduled inexact alarm as fallback");
    }

}
