package com.coffeechat;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class ImageUtils {

    public static File createImageFile(Activity activity, FirebaseUser currentUser) throws IOException {
        String imageFileName = currentUser.getUid();
        File storageDir = activity.getCacheDir();
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }
    public static CameraOutput openCamera(Activity activity, FirebaseUser currentUser, ActivityResultLauncher<Intent> takePictureLauncher, String LOG_TAG) {
        try {
            File photoFile = ImageUtils.createImageFile(activity, currentUser);
            Uri photoUri = FileProvider.getUriForFile(activity, "com.coffeechat.fileprovider", photoFile);

            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            takePictureLauncher.launch(cameraIntent);

            return new CameraOutput(photoFile, photoUri);
        } catch (IOException e) {
            Log.e(LOG_TAG, "IOException: " + e);
            return null;
        }
    }
    public static CameraOutput openCameraWithPermission(Activity activity, FirebaseUser currentUser,  ActivityResultLauncher<Intent> takePictureLauncher, ActivityResultLauncher<String> permissionLauncher, String LOG_TAG) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            return openCamera(activity, currentUser, takePictureLauncher, LOG_TAG);
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
            return null;
        }
    }

    public static void takePicture(Activity activity, ActivityResult result, File avatarFile, ActivityResultLauncher<Intent> cropLauncher) {
        if (result.getResultCode() == RESULT_OK) {
            Log.d("Camera result:", "OK");
            if (avatarFile != null) {
                Uri sourceUri = Uri.fromFile(avatarFile);
                Uri destinationUri = Uri.fromFile(new File(activity.getCacheDir(), "cropped_" + avatarFile.getName()));
                Intent uCropIntent = UCrop.of(sourceUri, destinationUri)
                        .withAspectRatio(1, 1)
                        .withMaxResultSize(700, 700)
                        .getIntent(activity);
                cropLauncher.launch(uCropIntent);
            }
        } else {
            Log.e("Camera result:", "Cancelled or failed");
        }
    }
    public static void openGallery(ActivityResultLauncher<Intent> galleryLauncher) {
        Intent openGallery = new Intent(Intent.ACTION_PICK);
        openGallery.setType("image/*");
        galleryLauncher.launch(openGallery);
    }

    public static void galleryLauncher(Activity activity, ActivityResult result, ActivityResultLauncher<Intent> cropLauncher, String LOG_TAG) {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Uri imageUri = result.getData().getData();
            if(imageUri != null) {
                Log.d(LOG_TAG, "Image opened successfully: " + imageUri);
                File croppedFile = new File(activity.getCacheDir(), "cropped_" + System.currentTimeMillis() + ".jpg");
                Uri outputUri = Uri.fromFile(croppedFile);
                Intent uCropIntent = UCrop.of(imageUri, outputUri)
                        .withAspectRatio(1, 1)
                        .withMaxResultSize(700, 700)
                        .getIntent(activity);
                cropLauncher.launch(uCropIntent);
            }
        }
    }

    public static void setAvatarImageView(ShapeableImageView avatarImageView, Uri resultUri, String LOG_TAG) {
        if (resultUri != null) {
            Glide.with(avatarImageView.getContext())
                    .load(resultUri)
                    .placeholder(R.drawable.coffee_default_avatar)
                    .error(R.drawable.coffee_default_avatar)
                    .into(avatarImageView);

            Log.d(LOG_TAG, "Image successfully set to ImageView using Glide");
        } else {
            Log.e(LOG_TAG, "Error: resultUri is null, cannot load image");
        }
    }

    public static Uri cropImage(ActivityResult result, String LOG_TAG) {
        if (result.getResultCode() == RESULT_OK) {
            if(result.getData() != null) {
                return UCrop.getOutput(result.getData());
            } else {
                return null;
            }
        } else if (result.getResultCode() == UCrop.RESULT_ERROR) {
            Log.e(LOG_TAG, "uCrop error");
            return null;
        }
        return null;
    }


    public static void uploadImage(Activity activity, Uri resultUri, FirebaseFirestore db, FirebaseUser user, String LOG_TAG) {

        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference avatarRef = storageRef.child("avatars/" + user.getUid() + ".jpg");
        avatarRef.putFile(resultUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Toast.makeText(activity, "Successfully updated your avatar!", Toast.LENGTH_LONG).show();
                    avatarRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        HashMap<String, Object> userMap = new HashMap<>();
                        userMap.put("avatarPicture", downloadUrl);
                        Log.d(LOG_TAG, "Avatar uploaded to storage");
                        FirebaseUtils.updateCollectionOnFirestore(db, "users", user.getUid(), userMap, LOG_TAG, new FirebaseUtilsCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d(LOG_TAG, "Avatar URL updated in Firestore");
                            }
                            @Override
                            public void onFailure(Exception e) {
                                Log.e(LOG_TAG, "Couldn't update avatar URL in Firestore:" + e);
                            }
                        });
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(LOG_TAG, "Upload failed: " + e.getMessage());
                    Toast.makeText(activity, "Failed to update your avatar!", Toast.LENGTH_LONG).show();
                });
    }

    public static void loadAvatar(Activity activity, FirebaseUser firebaseUser, ShapeableImageView avatarImageView, CoffeeChatUser coffeeChatUser, String LOG_TAG) {
        if (coffeeChatUser.getAvatarPicture() != null) {
            if(avatarImageView != null) {
                avatarImageView.setImageBitmap(coffeeChatUser.getAvatarPicture());
                Log.d(LOG_TAG, "Avatar loaded from memory");
            }
        } else {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference avatarRef = storage.getReference().child("avatars/" + firebaseUser.getUid() + ".jpg");

            avatarRef.getMetadata()
                    .addOnSuccessListener(metadata -> avatarRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                if (avatarImageView != null) {
                                    Glide.with(activity.getApplicationContext())
                                            .load(uri)  // load the Uri, not StorageReference
                                            .placeholder(R.drawable.coffee_default_avatar)
                                            .error(R.drawable.coffee_default_avatar)
                                            .into(avatarImageView);
                                }

                                // Load bitmap off main thread if needed
                                new Thread(() -> {
                                    try {
                                        Bitmap bitmap = Glide.with(activity.getApplicationContext())
                                                .asBitmap()
                                                .load(uri)
                                                .submit()
                                                .get();
                                        coffeeChatUser.setAvatarUri(uri);
                                        coffeeChatUser.setAvatarPicture(bitmap);
                                    } catch (Exception e) {
                                        Log.e(LOG_TAG, "Failed to convert Glide image to bitmap", e);
                                    }
                                }).start();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(LOG_TAG, "Failed to get download URL", e);
                                if (avatarImageView != null) {
                                    avatarImageView.setImageResource(R.drawable.coffee_default_avatar);
                                }
                            }))
                    .addOnFailureListener(e -> {
                        if (e instanceof StorageException && ((StorageException) e).getErrorCode() == StorageException.ERROR_OBJECT_NOT_FOUND) {
                            Log.w(LOG_TAG, "Avatar not found. Skipping.");
                        } else {
                            Log.e(LOG_TAG, "Error checking avatar existence", e);
                        }
                        if (avatarImageView != null) {
                            avatarImageView.setImageResource(R.drawable.coffee_default_avatar);
                        }
                    });
        }
    }
}
