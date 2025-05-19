package com.coffeechat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class PeopleActivity extends AppCompatActivity {

    private static final String LOG_TAG = PeopleActivity.class.getName();
    private UserListAdapter adapter;
    private UserViewModel userViewModel;
    private RecyclerView peopleRecyclerView;
    private EditText findPeopleEditText;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private FirebaseFirestore mDatabase;

    CoffeeChatUser coffeeChatUser = CoffeeChatUser.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_people);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.peopleMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        findPeopleEditText = findViewById(R.id.findPeopleEditText);

        findPeopleEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                userViewModel.filterUsersByUsername(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });


        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        adapter = new UserListAdapter();
        adapter.setOnItemClickListener(user -> {
            Log.d("username: ", user.getUserName());
            Log.d("avatar:", user.getAvatarUrl());
            NavigationUtils.moveToChatActivity(PeopleActivity.this, user.getUserName(), user.getAvatarUrl(), user.getUid());
        });
        peopleRecyclerView = findViewById(R.id.peopleRecyclerView);
        peopleRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        peopleRecyclerView.setAdapter(adapter);
    }

    protected void onStart() {
        super.onStart();
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        FirebaseUtils.checkLogin(PeopleActivity.this);
        mDatabase = FirebaseFirestore.getInstance();
        FirebaseUtils.checkAndUpdateEmailIfNeeded(PeopleActivity.this, mDatabase, mAuth, LOG_TAG);
        userViewModel.getUserList().observe(this, users -> adapter.submitList(users));
        userViewModel.getFilteredUsers().observe(this, users -> {
            adapter.submitList(users);
        });
        userViewModel.startListeningForUsers(mDatabase, coffeeChatUser.getUsername());
    }

    public void backToChatListIconPeopleOnClick(View view) {
        FirebaseUtils.checkAndUpdateEmailIfNeeded(PeopleActivity.this, mDatabase, mAuth, LOG_TAG);
        NavigationUtils.moveToActivity(PeopleActivity.this, ChatListActivity.class);
    }
}