package com.coffeechat;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;

public class SetAvatarActivity extends AppCompatActivity {
    private final static String LOG_TAG = SetAvatarActivity.class.getName();
    private ShapeableImageView avatarImageView;
    private ActivityResultLauncher<Intent> openGalleryLauncher;
    private ActivityResultLauncher<Intent> takePictureLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<Intent> cropLauncher;
    private Uri avatarUri;
    private File avatarFile;
    private Bitmap avatarPicture;
    FirebaseAuth mAuth;
    FirebaseUser mUser;
    FirebaseFirestore mDatabase;

    CoffeeChatUser coffeeChatUser = CoffeeChatUser.getInstance();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_avatar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.setProfilePictureMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        avatarImageView = findViewById(R.id.setAvatarImageView);

        openGalleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> ImageUtils.galleryLauncher(SetAvatarActivity.this, result, cropLauncher, LOG_TAG));

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> ImageUtils.takePicture(SetAvatarActivity.this, result, avatarFile, cropLauncher)
        );

        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        CameraOutput output = ImageUtils.openCamera(SetAvatarActivity.this, mUser, takePictureLauncher, LOG_TAG);
                        if (output != null) {
                            avatarFile = output.photoFile;
                            avatarUri = output.photoUri;
                        }
                    } else {
                        Log.w(LOG_TAG, "Camera permission denied");
                    }
                }
        );

        cropLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    avatarUri = ImageUtils.cropImage(result, LOG_TAG);
                    if(avatarUri != null) {
                        ImageUtils.setAvatarImageView(avatarImageView, avatarUri, LOG_TAG);
                        new Thread(() -> {
                            try {
                                avatarPicture = Glide.with(SetAvatarActivity.this.getApplicationContext())
                                        .asBitmap()
                                        .load(avatarUri)
                                        .submit()
                                        .get();

                            } catch (Exception e) {
                                Log.e(LOG_TAG, "Failed to convert Glide image to bitmap", e);
                            }
                        }).start();
                    }
                }
        );
    }

    protected void onStart() {
        super.onStart();
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        FirebaseUtils.checkLogin(SetAvatarActivity.this);
        mDatabase = FirebaseFirestore.getInstance();
        FirebaseUtils.checkAndUpdateEmailIfNeeded(SetAvatarActivity.this, mDatabase, mAuth, LOG_TAG);
    }

    public void openCameraOnClick(View view) {
        CameraOutput output = ImageUtils.openCameraWithPermission(SetAvatarActivity.this, mUser, takePictureLauncher, requestCameraPermissionLauncher, LOG_TAG);
        if (output != null) {
            avatarFile = output.photoFile;
            avatarUri = output.photoUri;
        }
    }

    public void openGalleryOnClick(View view) {
        ImageUtils.openGallery(openGalleryLauncher);
    }

    public void forwardButtonOnClick(View view) {
        if(avatarUri != null) {
            ImageUtils.uploadImage(SetAvatarActivity.this, avatarUri, mDatabase, mUser, LOG_TAG);
            coffeeChatUser.setAvatarUri(avatarUri);
            coffeeChatUser.setAvatarPicture(avatarPicture);
        }
        NavigationUtils.moveToActivity(SetAvatarActivity.this, ChatListActivity.class);
    }
}
