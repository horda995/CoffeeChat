package com.coffeechat;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;

public class SetProfilePictureActivity extends AppCompatActivity {
    private final static String LOG_TAG = SetUsernameActivity.class.getName();
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> takePictureLauncher;
    private Uri photoUri;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_set_profile_picture);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.setProfilePictureMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        /*Uri imageUri = result.getData().getData();

                        // For example: set image to an ImageView
                        ImageView imageView = findViewById(R.id.avatarImageView);
                        imageView.setImageURI(imageUri);*/
                        Toast.makeText(SetProfilePictureActivity.this, "GECI", Toast.LENGTH_SHORT).show();
                    }
                });

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // Photo was taken successfully
                        Toast.makeText(this, "Photo saved: " + photoUri, Toast.LENGTH_SHORT).show();
                        // You can use photoUri here (e.g., display the image)
                    }
                });

    }

    public void openCameraOnClick(View view) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
            return;
        }

        // Create an image file URI using FileProvider
        File photoFile = new File(getExternalFilesDir(null), "photo_" + System.currentTimeMillis() + ".jpg");
        photoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        takePictureLauncher.launch(cameraIntent);
    }

    public void openGalleryOnClick(View view) {
        Intent openGallery = new Intent(Intent.ACTION_PICK);
        openGallery.setType("image/*");
        galleryLauncher.launch(openGallery);
    }

    public void forwardButtonOnClick(View view) {
        Intent moveToLoginScreen = new Intent(SetProfilePictureActivity.this, ChatListActivity.class);
        startActivity(moveToLoginScreen);
        finish();
    }
}
