package com.coffeechat;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseNotificationJobService extends JobService {

    private static final String TAG = "NotificationJobService";

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "Job started");

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            CoffeeChatUser.getInstance().startListening(
                    db,
                    uid,
                    CoffeeChatUser.getInstance().getChatList(),
                    getApplicationContext()
            );
        }

        jobFinished(params, false);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "Job stopped");
        return true;
    }
}