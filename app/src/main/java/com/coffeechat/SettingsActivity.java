package com.coffeechat;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    private final static String LOG_TAG = AppCompatActivity.class.getName();
    FirebaseAuth mAuth;
    FirebaseUser mUser;
    FirebaseFirestore mDatabase;
    ListView settingsList;
    private ShapeableImageView avatarImageView;
    private ActivityResultLauncher<Intent> openGalleryLauncher;
    private ActivityResultLauncher<Intent> takePictureLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<Intent> cropLauncher;
    private File avatarFile;
    private Uri avatarUri;
    private Bitmap avatarPicture;

    CoffeeChatUser coffeeChatUser = CoffeeChatUser.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settingsMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;

        });

        settingsList = findViewById(R.id.settingsListView);
        ArrayAdapter<String> adapter = getStringArrayAdapter();

        settingsList.setOnItemLongClickListener((parent, view, position, id) -> {
            String selectedItem = parent.getItemAtPosition(position).toString();

            if (selectedItem.equals("Log Out")) {
                try {
                    logOut();
                } catch(Exception e) {
                    Log.e(LOG_TAG, "Sign out failed: " + e.getMessage(), e);
                    Toast.makeText(SettingsActivity.this, "Logout failed. Please try again.", Toast.LENGTH_LONG).show();
                }
                return true;
            }
            else if (selectedItem.equals("Delete account")) {
                try {
                    FirebaseUtils.checkAndUpdateEmailIfNeeded(SettingsActivity.this, mDatabase, mAuth, LOG_TAG);
                    showDeleteAccountDialog();
                } catch(Exception e) {
                    Log.e(LOG_TAG, "Sign out failed: " + e.getMessage(), e);
                    Toast.makeText(SettingsActivity.this, "Account deletion failed. Please try again.", Toast.LENGTH_LONG).show();
                }
                return true;
            }

            return false;
        });

        settingsList.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItem = parent.getItemAtPosition(position).toString();

            switch (selectedItem) {
                case "Change e-mail":
                    try {
                        FirebaseUtils.checkAndUpdateEmailIfNeeded(SettingsActivity.this, mDatabase, mAuth, LOG_TAG);
                        showChangeEmailDialog();
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Failed to open the change e-mail dialog " + e.getMessage(), e);
                    }
                    break;
                case "Change username":
                    try {
                        FirebaseUtils.checkAndUpdateEmailIfNeeded(SettingsActivity.this, mDatabase, mAuth, LOG_TAG);
                        showChangeUsernameDialog();
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Failed to open the change username dialog " + e.getMessage(), e);
                    }
                    break;
                case "Change profile picture":
                    try {
                        FirebaseUtils.checkAndUpdateEmailIfNeeded(SettingsActivity.this, mDatabase, mAuth, LOG_TAG);
                        showChangeAvatarDialog();
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Failed to open the change profile picture dialog " + e.getMessage(), e);
                    }
                    break;
                case "Change password":
                    try {
                        FirebaseUtils.checkAndUpdateEmailIfNeeded(SettingsActivity.this, mDatabase, mAuth, LOG_TAG);
                        showChangePasswordDialog();
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Failed to open the change password dialog " + e.getMessage(), e);
                    }
                    break;
                case "Log out":
                case "Delete account":
                    try {
                        FirebaseUtils.checkAndUpdateEmailIfNeeded(SettingsActivity.this, mDatabase, mAuth, LOG_TAG);
                    }
                    catch (Exception e) {
                        Log.e(LOG_TAG, "Error");
                    }
                    break;
            }
        });
        settingsList.setAdapter(adapter);

        openGalleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> ImageUtils.galleryLauncher(SettingsActivity.this, result, cropLauncher, LOG_TAG));

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> ImageUtils.takePicture(SettingsActivity.this, result, avatarFile, cropLauncher)
        );

        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        CameraOutput output = ImageUtils.openCamera(SettingsActivity.this, mUser, takePictureLauncher, LOG_TAG);
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
                                avatarPicture = Glide.with(SettingsActivity.this.getApplicationContext())
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
        FirebaseUtils.checkLogin(SettingsActivity.this);
        mDatabase = FirebaseFirestore.getInstance();
        FirebaseUtils.checkAndUpdateEmailIfNeeded(SettingsActivity.this, mDatabase, mAuth, LOG_TAG);

    }


    private void logOut() {
        CoffeeChatUser.deleteCoffeeChatUserInstance();
        FirebaseAuth.getInstance().signOut();
        Log.d(LOG_TAG, "User signed out.");
        NavigationUtils.moveToLoginActivity(this);
    }

    @NonNull
    private ArrayAdapter<String> getStringArrayAdapter() {
        List<String> settingsItems = new ArrayList<>();
        settingsItems.add("Change e-mail");
        settingsItems.add("Change username");
        settingsItems.add("Change profile picture");
        settingsItems.add("Change password");
        settingsItems.add("Delete account");
        settingsItems.add("Log Out");

        return new ArrayAdapter<>(this, R.layout.settings_list_view, R.id.settingItemText, settingsItems);
    }

    private void showChangeAvatarDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_avatar, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        dialog.setOnDismissListener(dialogInterface -> Log.d("DialogDebug", "Dialog dismissed safely"));

        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.y = -((int)(Resources.getSystem().getDisplayMetrics().heightPixels * 0.05));
            window.setAttributes(params);
        }

        avatarImageView = dialogView.findViewById(R.id.changeAvatarImageView);
        ImageUtils.loadAvatar(SettingsActivity.this, mUser, avatarImageView, coffeeChatUser, LOG_TAG);
        ImageView cameraIcon = dialogView.findViewById(R.id.changeAvatarCameraIcon);
        ImageView galleryIcon = dialogView.findViewById(R.id.changeAvatarGalleryIcon);

        Button cancelButton = dialogView.findViewById(R.id.changeAvatarCancelButton);
        Button changeButton = dialogView.findViewById(R.id.changeAvatarButton);

        cancelButton.setOnClickListener(v -> {
            if (dialog.isShowing() && !SettingsActivity.this.isFinishing()) {
                dialog.dismiss();
            }
        });

        changeButton.setOnClickListener(v -> {
            ImageUtils.uploadImage(SettingsActivity.this, avatarUri, mDatabase, mUser, LOG_TAG);
            coffeeChatUser.setAvatarUri(avatarUri);
            coffeeChatUser.setAvatarPicture(avatarPicture);
            if (dialog.isShowing() && !SettingsActivity.this.isFinishing()) {
                dialog.dismiss();
            }
        });

        cameraIcon.setOnClickListener(v -> {
            CameraOutput output = ImageUtils.openCameraWithPermission(SettingsActivity.this, mUser, takePictureLauncher, requestCameraPermissionLauncher, LOG_TAG);
            if (output != null) {
                avatarFile = output.photoFile;
                avatarUri = output.photoUri;
            }
        });

        galleryIcon.setOnClickListener(v -> ImageUtils.openGallery(openGalleryLauncher));

        if (!SettingsActivity.this.isFinishing() && !SettingsActivity.this.isDestroyed()) {
            dialog.show();
        }
    }

    private void showDeleteAccountDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_delete_account, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        dialog.setOnDismissListener(dialogInterface -> Log.d("DialogDebug", "Dialog dismissed safely"));

        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.y = -((int)(Resources.getSystem().getDisplayMetrics().heightPixels * 0.05));
            window.setAttributes(params);
        }

        Button cancelButton = dialogView.findViewById(R.id.accountDeletionCancelButton);
        Button deleteButton = dialogView.findViewById(R.id.accountDeleteButton);

        cancelButton.setOnClickListener(v -> {
            if (dialog.isShowing() && !SettingsActivity.this.isFinishing()) {
                dialog.dismiss();
            }
        });
        deleteButton.setOnClickListener(v -> {
            CoffeeChatUser.deleteCoffeeChatUserInstance();
            FirebaseUtils.deleteAccount(LOG_TAG, SettingsActivity.this);
            if (dialog.isShowing() && !SettingsActivity.this.isFinishing()) {
                dialog.dismiss();
            }
        });

        if (!SettingsActivity.this.isFinishing() && !SettingsActivity.this.isDestroyed()) {
            dialog.show();
        }
    }

    private void showChangeUsernameDialog() {

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_username, null);
        Button cancelButton = dialogView.findViewById(R.id.changeUsernameCancelButton);
        Button okButton = dialogView.findViewById(R.id.changeUsernameButton);
        EditText changeUsernameEditText = dialogView.findViewById(R.id.changeUsernameEditText);

        String uid = mUser.getUid();
        FirebaseUtils.readFieldFromFirestore(mDatabase, "users", uid, "username", new FirestoreReadFieldCallback() {
            public void onFieldRetrieved(Object value) {
                if (value != null) {
                    String userName = value.toString();
                    Log.d(LOG_TAG, "Username exists:" + userName);
                    changeUsernameEditText.setHint(userName);
                }
            }
            public void onError(Exception e){
                Log.e(LOG_TAG, "Error fetching field", e);
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        dialog.setOnDismissListener(dialogInterface -> Log.d("DialogDebug", "Dialog dismissed safely"));

        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.y = -((int)(Resources.getSystem().getDisplayMetrics().heightPixels * 0.05));
            window.setAttributes(params);
        }

        cancelButton.setOnClickListener(v -> {
            if (dialog.isShowing() && !SettingsActivity.this.isFinishing()) {
                dialog.dismiss();
            }
        });
        okButton.setOnClickListener(v -> {
            try {
                String enteredUsername = changeUsernameEditText.getText().toString().trim();
                InputCheckerUtils.validateNotNullOrEmpty(enteredUsername, "New username");
                FirebaseUtils.readFieldFromFirestore(mDatabase, "users", uid, "username", new FirestoreReadFieldCallback() {
                    @Override
                    public void onFieldRetrieved(Object value) {
                        String userName = value.toString();
                        if(enteredUsername.equals(userName)) {
                            Log.d(LOG_TAG, "Username already taken: " + userName);
                            Toast.makeText(SettingsActivity.this, "Username already taken", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            HashMap<String, Object> userMap = new HashMap<>();
                            userMap.put("username", enteredUsername);
                            FirebaseUtils.updateCollectionOnFirestore(mDatabase, "users", uid, userMap, LOG_TAG, new FirebaseUtilsCallback() {
                                @Override
                                public void onSuccess() {
                                    Log.d(LOG_TAG, "Username changed");
                                    Toast.makeText(SettingsActivity.this, "Username changed!", Toast.LENGTH_SHORT).show();
                                    if (dialog.isShowing() && !SettingsActivity.this.isFinishing()) {
                                        dialog.dismiss();
                                    }
                                }
                                @Override
                                public void onFailure(Exception e) {
                                    Log.e(LOG_TAG, "Failed to change username:" + e.getMessage());
                                    Toast.makeText(SettingsActivity.this, "Failed to change username", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                    @Override
                    public void onError(Exception e) {
                        Log.e(LOG_TAG, "Failed to get Field:" + e.getMessage());
                    }
                });

            } catch (Exception e) {
                Log.e(LOG_TAG, "Exception: " + e.getMessage());
                Toast.makeText(SettingsActivity.this, "Username update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }

        });
        if (!SettingsActivity.this.isFinishing() && !SettingsActivity.this.isDestroyed()) {
            dialog.show();
        }
    }

    private void showChangePasswordDialog() {

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        Button cancelButton = dialogView.findViewById(R.id.changePasswordCancelButton);
        Button okButton = dialogView.findViewById(R.id.changePasswordButton);
        EditText changePasswordEditText = dialogView.findViewById(R.id.changePasswordEditText);
        EditText changePasswordOldEditText = dialogView.findViewById(R.id.changePasswordOldEditText);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        dialog.setOnDismissListener(dialogInterface -> Log.d("DialogDebug", "Dialog dismissed safely"));

        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.y = -((int)(Resources.getSystem().getDisplayMetrics().heightPixels * 0.05));
            window.setAttributes(params);
        }

        cancelButton.setOnClickListener(v -> {
            if (dialog.isShowing() && !SettingsActivity.this.isFinishing()) {
                dialog.dismiss();
            }
        });
        okButton.setOnClickListener(v -> {
            try {
                String enteredPassword = changePasswordEditText.getText().toString().trim();
                String oldPassword = changePasswordOldEditText.getText().toString().trim();
                InputCheckerUtils.validateNotNullOrEmpty(enteredPassword, "Entered password");
                InputCheckerUtils.validateNotNullOrEmpty(oldPassword, "Old password");
                mUser = FirebaseUtils.getFirebaseUser(mAuth,SettingsActivity.this, LOG_TAG);
                if (mUser != null) {
                    AuthCredential credential = EmailAuthProvider
                            .getCredential(Objects.requireNonNull(mUser.getEmail()), oldPassword);
                    mUser.reauthenticate(credential)
                            .addOnCompleteListener(authTask -> {
                                if (authTask.isSuccessful()) {
                                    mUser.updatePassword(enteredPassword)
                                            .addOnCompleteListener(updateTask -> {
                                                if (updateTask.isSuccessful()) {
                                                    Log.d(LOG_TAG, "Password updated successfully.");
                                                    Toast.makeText(SettingsActivity.this, "Password changed!", Toast.LENGTH_SHORT).show();
                                                    if (dialog.isShowing() && !SettingsActivity.this.isFinishing()) {
                                                        dialog.dismiss();
                                                    }
                                                } else {
                                                    Log.e(LOG_TAG, "Password update failed: " + Objects.requireNonNull(updateTask.getException()).getMessage());
                                                    Toast.makeText(SettingsActivity.this, "Password update failed: " + updateTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                                }
                                            });
                                } else {
                                    Log.e(LOG_TAG, "Re-authentication failed: " + Objects.requireNonNull(authTask.getException()).getMessage());

                                }
                            });
                }

            } catch (Exception e) {
                Log.e(LOG_TAG, "Exception: " + e.getMessage());
                Toast.makeText(SettingsActivity.this, "Password update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }

        });
        if (!SettingsActivity.this.isFinishing() && !SettingsActivity.this.isDestroyed()) {
            dialog.show();
        }
    }

    private void showChangeEmailDialog() {

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_email, null);
        Button cancelButton = dialogView.findViewById(R.id.changeEmailCancelButton);
        Button okButton = dialogView.findViewById(R.id.changeEmailButton);
        EditText changeEmailEditText = dialogView.findViewById(R.id.changeEmailEditText);
        EditText changeEmailPasswordEditText = dialogView.findViewById(R.id.changeEmailPasswordEditText);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        dialog.setOnDismissListener(dialogInterface -> Log.d("DialogDebug", "Dialog dismissed safely"));

        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.y = -((int)(Resources.getSystem().getDisplayMetrics().heightPixels * 0.05));
            window.setAttributes(params);
        }

        cancelButton.setOnClickListener(v -> {
            if (dialog.isShowing() && !SettingsActivity.this.isFinishing()) {
                dialog.dismiss();
            }
        });

        okButton.setOnClickListener(v -> {
            try {
                String enteredEmail = changeEmailEditText.getText().toString().trim();
                String password = changeEmailPasswordEditText.getText().toString().trim();

                String oldEmail = mUser.getEmail();

                InputCheckerUtils.validateNotNullOrEmpty(enteredEmail, "New e-mail");
                InputCheckerUtils.validateNotNullOrEmpty(password, "Password");


                if(enteredEmail.equals(oldEmail)) {
                    Log.d(LOG_TAG, "This is your old e-mail.");
                    Toast.makeText(SettingsActivity.this, "This your old e-mail", Toast.LENGTH_SHORT).show();
                }
                else {
                    assert oldEmail != null;
                    AuthCredential credential = EmailAuthProvider.getCredential(oldEmail, password);
                    mUser.reauthenticate(credential)
                            .addOnCompleteListener(authTask -> {
                                if (authTask.isSuccessful()) {
                                    mUser.verifyBeforeUpdateEmail(enteredEmail)
                                            .addOnCompleteListener(updateTask -> {
                                                if (updateTask.isSuccessful()) {
                                                    FirebaseUtils.setEmailChangeRequested(SettingsActivity.this, true);
                                                    Log.d(LOG_TAG, "Verification email sent to new address. Checking the change in the background.");
                                                    Toast.makeText(SettingsActivity.this, "A verification link has been sent to your new email. Please verify to complete the update.", Toast.LENGTH_LONG).show();
                                                    if (dialog.isShowing() && !SettingsActivity.this.isFinishing()) {
                                                        dialog.dismiss();
                                                    }

                                                } else {
                                                    Log.e(LOG_TAG, "Failed to send verification email: " + Objects.requireNonNull(updateTask.getException()).getMessage());
                                                    Toast.makeText(SettingsActivity.this, "Failed to send verification email", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                } else {
                                    Toast.makeText(SettingsActivity.this, "Re-authentication failed.", Toast.LENGTH_SHORT).show();
                                }
                            });
                }

            } catch (Exception e) {
                Log.e(LOG_TAG, "Exception: " + e.getMessage());
                Toast.makeText(SettingsActivity.this, "E-mail update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }

        });

        if (!SettingsActivity.this.isFinishing() && !SettingsActivity.this.isDestroyed()) {
            dialog.show();
        }
    }

    public void backToChatListIconOnClick(View view) {
        FirebaseUtils.checkAndUpdateEmailIfNeeded(SettingsActivity.this, mDatabase, mAuth, LOG_TAG);
        NavigationUtils.moveToActivity(this, ChatListActivity.class);
    }

}