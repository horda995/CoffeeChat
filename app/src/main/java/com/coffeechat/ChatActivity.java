package com.coffeechat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;

public class ChatActivity extends AppCompatActivity {

    private static final String LOG_TAG = ChatActivity.class.getName();

    String username;
    String avatarUrl;
    String otherUserUid;
    ShapeableImageView avatarImageView;
    TextView chatUsernameTextView;
    EditText chatInput;

    private MediaRecorder recorder;
    private File audioFile;
    private Handler handler = new Handler(Looper.getMainLooper());
    private ImageView micIcon;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    FirebaseFirestore mDatabase;
    CoffeeChatUser coffeeChatUser = CoffeeChatUser.getInstance();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.chatMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        micIcon = findViewById(R.id.sendSoundMessageIcon);
        username = getIntent().getStringExtra("username");
        avatarUrl = getIntent().getStringExtra("avatarUrl");
        otherUserUid = getIntent().getStringExtra("uid");
        chatInput = findViewById(R.id.chatInput);
        avatarImageView = findViewById(R.id.chatAvatarImageView);
        chatUsernameTextView = findViewById(R.id.chatUsernameLabel);
        chatUsernameTextView.setText(username);
        Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.coffee_default_avatar)
                .into(avatarImageView);
    }

    protected void onStart() {
        super.onStart();
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        FirebaseUtils.checkLogin(ChatActivity.this);
        mDatabase = FirebaseFirestore.getInstance();
        FirebaseUtils.checkAndUpdateEmailIfNeeded(ChatActivity.this, mDatabase, mAuth, LOG_TAG);
    }


    public void friendsIconOnClick(View view) {
        NavigationUtils.moveToActivity(ChatActivity.this, PeopleActivity.class);
    }

    public void chatListIconOnClick(View view) {
        NavigationUtils.moveToActivity(ChatActivity.this, ChatListActivity.class);
    }
    private String chatIdGenerator() {
        String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        int LENGTH = 20;
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        String generated = "chat_" + sb.toString() + "_id";
        Log.d("chatIdGenerator", "Generated chatId: " + generated);
        return generated;
    }
    public void sendMessageOnClick(View view) {
        if(!chatInput.getText().toString().trim().isEmpty()) {
            String soundUrl = "lol";
            String messageText = chatInput.getText().toString().trim();
            FirebaseUtils.checkIfChatWithUserExists(mDatabase, coffeeChatUser.getUid(), otherUserUid, new FirebaseChatIdCallback() {
                @Override
                public void onChatIdFound(String chatId) {
                    FirebaseUtils.addMessageToFirestore(mDatabase, chatId, coffeeChatUser.getUid(), messageText, soundUrl, chatInput);
                }
                @Override
                public void onChatNotFound() {
                    ArrayList<String> uidList = new ArrayList<>();
                    if(coffeeChatUser.getUid() != null && otherUserUid != null) {
                        uidList.add(coffeeChatUser.getUid());
                        uidList.add(otherUserUid);
                        String chatId = chatIdGenerator();
                        FirebaseUtils.addChatToChatCollection(mDatabase, chatId, uidList, () -> {
                            FirebaseUtils.addMessageToFirestore(mDatabase, chatId, coffeeChatUser.getUid(), messageText, soundUrl, chatInput);
                        });
                    }

                }
                @Override
                public void onError(Exception e) {
                    Log.d(LOG_TAG, e.toString());
                }
            });
        }
        else {
            Log.w("chatInput", "Message is empty");
        }
    }

    public void recordMessageOnClick(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 101);
            return;
        }

        try {
            micIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            audioFile = File.createTempFile("voice_", ".m4a", getCacheDir());

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setOutputFile(audioFile.getAbsolutePath());
            recorder.prepare();
            recorder.start();

            handler.postDelayed(this::stopRecordingAndUpload, 10_000);

        } catch (IOException e) {
            Log.e(LOG_TAG, "IO error" + e);
        }
    }

    private void stopRecordingAndUpload() {
        try {
            recorder.stop();
            recorder.release();
            recorder = null;

            micIcon.setColorFilter(null);
            uploadAudioToFirebase(audioFile);

        } catch (Exception e) {
            Log.e(LOG_TAG, "error:" + e);
        }
    }

    private void uploadAudioToFirebase(File file) {
        Uri fileUri = Uri.fromFile(file);
        String fileName = "voiceMessages/" + System.currentTimeMillis() + ".m4a";

        FirebaseStorage.getInstance().getReference(fileName)
                .putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(uri -> {
                        Toast.makeText(getApplicationContext(), "Audio uploaded", Toast.LENGTH_LONG).show();
                        String downloadUrl = uri.toString();
                        Log.d("AudioUpload", "Download URL: " + downloadUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("AudioUpload", "Upload failed", e);
                });
    }

}