package com.coffeechat;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.security.SecureRandom;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class FirebaseUtils {

    public static void checkAndUpdateEmailIfNeeded(Context context, FirebaseFirestore db, FirebaseAuth auth, String LOG_TAG) {
        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        boolean isRequested = prefs.getBoolean("isEmailChangeRequested", false);

        Log.d(LOG_TAG, "Running checkAndUpdateEmailIfNeeded() - isRequested: " + isRequested);
        if(isRequested) {
            FirebaseUser user = auth.getCurrentUser();
            if (user == null) {
                Log.d(LOG_TAG, "User is null or email change not requested. Signing out if needed.");
                FirebaseAuth.getInstance().signOut();
                NavigationUtils.moveToLoginActivity(context);
                return;
            }

            user.reload().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Exception e = task.getException();
                    if (e instanceof FirebaseAuthInvalidUserException) {
                        Log.d(LOG_TAG, "User is no longer valid, signing out...");
                        FirebaseAuth.getInstance().signOut();
                        NavigationUtils.moveToLoginActivity(context);
                    } else {
                        Log.e(LOG_TAG, "Failed to reload user", e);
                    }
                    return;
                }

                String newEmail = user.getEmail();
                String uid = user.getUid();

                readFieldFromFirestore(db, "users", uid, "email", new FirestoreReadFieldCallback() {
                    @Override
                    public void onFieldRetrieved(Object value) {
                        if (value != null && !Objects.equals(newEmail, value.toString())) {
                            HashMap<String, Object> userMap = new HashMap<>();
                            userMap.put("email", newEmail);
                            updateCollectionOnFirestore(db, "users", uid, userMap, LOG_TAG, new FirebaseUtilsCallback() {
                                @Override
                                public void onSuccess() {
                                    Log.d(LOG_TAG, "Email updated in Firestore");
                                    prefs.edit().putBoolean("isEmailChangeRequested", false).apply();
                                    Toast.makeText(context, "Email successfully updated!", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Log.e(LOG_TAG, "Failed to update email in Firestore", e);
                                }
                            });
                        } else {
                            Log.d(LOG_TAG, "Emails match or couldn't fetch Firestore email");
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(LOG_TAG, "Error reading email field", e);
                    }
                });
            });
        }
    }

    public static void setEmailChangeRequested(Activity activity, boolean value) {
        SharedPreferences prefs = activity.getSharedPreferences("AppPrefs", Activity.MODE_PRIVATE);
        prefs.edit().putBoolean("isEmailChangeRequested", value).apply();
    }

    public static FirebaseUser getFirebaseUser(FirebaseAuth auth, Activity activity, String LOG_TAG) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            if (!(activity instanceof LoginActivity)) {
                Log.d(LOG_TAG, "Unauthorized, moved to login screen.");
                NavigationUtils.moveToLoginActivity(activity);
            }
        }
        return currentUser;
    }

    public static void checkLogin(Activity activity) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            currentUser.getIdToken(true) // Force refresh the token
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Token refreshed, now you can safely proceed to check Firestore or continue
                            // Optionally re-check if their Firestore data exists
                            final String uid = currentUser.getUid();
                            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (!documentSnapshot.exists()) {
                                            // Firestore user record was deleted – force sign out
                                            FirebaseAuth.getInstance().signOut();
                                            NavigationUtils.moveToLoginActivity(activity);
                                        }
                                    })
                                    .addOnFailureListener(e -> Log.e("AuthCheck", "Error checking Firestore user record", e));

                        } else {
                            FirebaseAuth.getInstance().signOut();
                            NavigationUtils.moveToLoginActivity(activity);
                        }
                    });
        } else {
            NavigationUtils.moveToLoginActivity(activity);
            activity.finish();
        }
    }


    public static void readFieldFromFirestore(FirebaseFirestore db, String collectionPath, String documentID, String fieldName, FirestoreReadFieldCallback callback) {

        db.collection(collectionPath).document(documentID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Object fieldValue = documentSnapshot.get(fieldName);
                        if(fieldValue != null) {
                            callback.onFieldRetrieved(fieldValue);
                        } else {
                            Log.e("readFieldFromFirestore: ", "The document exists but the field does not" );
                            callback.onError(new Exception("Field does not exist"));
                        }
                    } else {
                        callback.onError(new Exception("Document does not exist"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public static void checkIfAlreadyExistsInFirestore(Activity activity, FirebaseFirestore db, String collectionPath, String fieldName, String fieldValue, String LOG_TAG, FirebaseUtilsCallback callback) {
        db.collection(collectionPath)
                .whereEqualTo(fieldName, fieldValue)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Toast.makeText(activity, "This " + fieldName + "is already taken", Toast.LENGTH_SHORT).show();
                    }
                    else  {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(activity, "Error checking " + fieldName, Toast.LENGTH_LONG).show();
                    Log.e(LOG_TAG, "Firestore query failed: " + e.getMessage());
                });
    }

    public static void performLogin(Activity activity, FirebaseAuth auth, FirebaseFirestore db, String email, String password, String LOG_TAG, LoginCallback callback) {

        InputCheckerUtils.validateNotNullOrEmpty(email, "E-mail");
        InputCheckerUtils.validateNotNullOrEmpty(password, "Password");

        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(activity, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = auth.getCurrentUser();
                Log.d(LOG_TAG, "Login was successful! " + Objects.requireNonNull(user).getEmail());
                readFieldFromFirestore(db, "users", user.getUid(), "username", new FirestoreReadFieldCallback() {
                    @Override
                    public void onFieldRetrieved(Object value) {
                        if (value != null) {
                            String userName = value.toString();
                            Log.d(LOG_TAG, "Username exists, moving on to ChatlistActivity. Username: " + userName);
                            NavigationUtils.moveToActivity(activity, ChatListActivity.class);
                        }
                        else {
                            Log.d(LOG_TAG, "Username does not exist moving on to SetUserNameActivity");
                            NavigationUtils.moveToActivity(activity, SetUsernameActivity.class);
                        }
                    }
                    @Override
                    public void onError(Exception e) {
                        Log.e("Firestore", "Error fetching field", e);
                    }
                });
                callback.onSuccess(user);

            } else {
                Exception e = task.getException();
                Log.d(LOG_TAG, "Login was unsuccessful!");
                Toast.makeText(activity, "Login failed: " + Objects.requireNonNull(e).getMessage(), Toast.LENGTH_LONG).show();
                callback.onFailure(e);
            }
        });
    }



    public static void addCollectionToFirestore(FirebaseFirestore db, String collectionPath, String documentID, HashMap<String, Object> data, String LOG_TAG, FirebaseUtilsCallback callback) {

        db.collection(collectionPath).document(documentID)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Log.d(LOG_TAG, "Data successfully added to Firestore");
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(LOG_TAG, "Error saving data to Firestore: " + e.getMessage());
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    public static void firebaseRegistration(String email, String password, String confirmPassword, Activity activity, String LOG_TAG, CoffeeChatUser coffeeChatUser) {

        FirebaseAuth auth = FirebaseAuth.getInstance();

        InputCheckerUtils.validateNotNullOrEmpty(email, "E-mail");
        InputCheckerUtils.validateNotNullOrEmpty(password, "Password");
        InputCheckerUtils.validateNotNullOrEmpty(confirmPassword, "Confirmed password");

        if (!password.equals(confirmPassword)) {
            Log.e(LOG_TAG, "Password mismatch!");
            Toast.makeText(activity, "Passwords do not match!", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(activity, task -> {
            if (task.isSuccessful()) {
                Log.d(LOG_TAG, "Account creation is successful");

                FirebaseUser user = auth.getCurrentUser();
                if (user != null) {

                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    String uid = user.getUid();
                    String usersCollectionPath = "users";
                    HashMap<String, Object> userMap = new HashMap<>();
                    userMap.put("email", email);
                    userMap.put("uid", uid);
                    addCollectionToFirestore(db, usersCollectionPath, uid, userMap, LOG_TAG, new FirebaseUtilsCallback() {
                        @Override
                        public void onSuccess() {
                            // Move to another activity
                            coffeeChatUser.setEmail(email);
                            coffeeChatUser.setUid(uid);
                            Log.d(LOG_TAG, "User created.");
                            NavigationUtils.moveToActivity(activity, SetUsernameActivity.class);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(LOG_TAG, "Failed to create user:" + e.getMessage());
                            Toast.makeText(activity, "Failed to create user", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                Log.e(LOG_TAG, "Account creation failed: " + Objects.requireNonNull(task.getException()).getMessage());
                Toast.makeText(activity, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    public static void updateCollectionOnFirestore(FirebaseFirestore db, String collectionPath, String documentID, HashMap<String, Object> data, String LOG_TAG, FirebaseUtilsCallback callback) {

        db.collection(collectionPath).document(documentID)
                .update(data)
                .addOnSuccessListener(aVoid -> {
                    Log.d(LOG_TAG, "Data successfully added to Firestore");
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(LOG_TAG, "Error saving data to Firestore: " + e.getMessage());
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    public static void deleteAccount(String LOG_TAG, Activity activity) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String collectionPath = "users";
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.e(LOG_TAG, "Unauthorized, moved to login screen.");
            NavigationUtils.moveToLoginActivity(activity);
        }
        else {
            String uid = currentUser.getUid();
            db.collection(collectionPath).document(uid)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(LOG_TAG, "User data successfully deleted from Firestore");
                        //delete FirebaseAuth account
                        currentUser.delete()
                                .addOnSuccessListener(bVoid -> {
                                    Log.d(LOG_TAG, "Account successfully deleted.");
                                    NavigationUtils.moveToLoginActivity(activity);
                                })
                                .addOnFailureListener(e -> currentUser.delete());
                    })
                    .addOnFailureListener(e -> Log.e(LOG_TAG, "Error deleting data from Firestore: " + e.getMessage()));
        }
    }

    public static void addChatToUserChatList(FirebaseFirestore db, ArrayList<String> userIds, String chatId, Runnable onComplete) {
        AtomicInteger completed = new AtomicInteger(0);
        int total = userIds.size();

        for (String uid : userIds) {
            DocumentReference userRef = db.collection("users").document(uid);
            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    userRef.update("chatlist", FieldValue.arrayUnion(chatId))
                            .addOnSuccessListener(aVoid -> {
                                Log.d("FirestoreUpdate", "Chat ID added to chatlist for user: " + uid);
                                if (completed.incrementAndGet() == total && onComplete != null) {
                                    onComplete.run();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("FirestoreUpdate", "Failed to update chatlist for user: " + uid, e);
                                if (completed.incrementAndGet() == total && onComplete != null) {
                                    onComplete.run();
                                }
                            });
                } else {
                    Log.w("FirestoreUpdate", "User document not found for UID: " + uid + ". Skipping.");
                    if (completed.incrementAndGet() == total && onComplete != null) {
                        onComplete.run();
                    }
                }
            }).addOnFailureListener(e -> {
                Log.e("FirestoreUpdate", "Failed to retrieve document for user: " + uid, e);
                if (completed.incrementAndGet() == total && onComplete != null) {
                    onComplete.run();
                }
            });
        }
    }

    public static void addChatToChatCollection(FirebaseFirestore db, String chatId, ArrayList<String> userId, Runnable onComplete) {
        if (userId == null || chatId == null || chatId.isEmpty() || userId.isEmpty()) {
            Log.e("addChatToChatCollection", "Invalid input: users or chatId is null/empty");
            return;
        }

        DocumentReference chatRef = db.collection("chats").document(chatId);

        chatRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Log.d("addChatToChatCollection", "Chat with ID " + chatId + " already exists.");
                if (onComplete != null) onComplete.run();  // still trigger if chat exists
            } else {
                HashMap<String, Object> chatData = new HashMap<>();
                chatData.put("uid", userId);
                chatRef.set(chatData)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("addChatToChatCollection", "Chat created successfully with ID: " + chatId);
                            addChatToUserChatList(db, userId, chatId, onComplete);  // pass callback
                        })
                        .addOnFailureListener(e -> Log.e("addChatToChatCollection", "Failed to create chat", e));
            }
        }).addOnFailureListener(e -> Log.e("addChatToChatCollection", "Failed to check if chat exists", e));
    }

    public static void checkIfChatWithUserExists(FirebaseFirestore db, String uid, String partnerUid, FirebaseChatIdCallback callback) {
        db.collection("chats")
                .whereArrayContains("uid", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        //noinspection unchecked remelhetoleg string :)
                        List<String> users = (List<String>) document.get("uid");
                        if (users != null && users.contains(partnerUid)) {
                            callback.onChatIdFound(document.getId());
                            return;
                        }
                    }
                    callback.onChatNotFound();
                })
                .addOnFailureListener(callback::onError);
    }

    public static void addMessageToFirestore(FirebaseFirestore db, String chatId, String sentByUid, String messageText, String soundUrl, EditText chatInput) {
        addMessageWithUniqueId(db, chatId, sentByUid, messageText, soundUrl, 0, chatInput);
    }

    private static void addMessageWithUniqueId(FirebaseFirestore db, String chatId, String sentByUid, String messageText, String soundUrl, int attempt, EditText chatInput) {
        if (attempt >= 5) {
            Log.e("Firestore", "Failed to generate unique message ID after multiple attempts.");
            return;
        }

        String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        int LENGTH = 20;
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }

        String messageId = "msg_" + sb + "_id";

        DocumentReference messageRef = db.collection("messages").document(messageId);

        messageRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // If ID exists, try again
                addMessageWithUniqueId(db, chatId, sentByUid, messageText, soundUrl, attempt + 1, chatInput);
            } else {
                // ID is unique, proceed to add
                HashMap<String, Object> data = new HashMap<>();
                data.put("chatId", chatId);
                data.put("sentByUid", sentByUid);
                data.put("messageText", messageText);
                data.put("soundUrl", soundUrl);
                data.put("timestamp", FieldValue.serverTimestamp());
                chatInput.setText("");
                messageRef.set(data)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("Firestore", "Message added successfully with ID: " + messageId);
                        })
                        .addOnFailureListener(e -> Log.e("Firestore", "Failed to add message", e));
            }
        }).addOnFailureListener(e -> {
            Log.e("Firestore", "Failed to check for message ID existence", e);
        });
    }

}
