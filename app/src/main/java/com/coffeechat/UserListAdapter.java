package com.coffeechat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.UserViewHolder> {

    private final List<OtherUser> users = new ArrayList<>();

    private OnItemClickListener listener;
    public interface OnItemClickListener {
        void onItemClick(OtherUser user);
    }
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }


    public void submitList(List<OtherUser> newList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new UserDiffCallback(users, newList));
        users.clear();
        users.addAll(newList);
        diffResult.dispatchUpdatesTo(this);
    }


    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_view, parent, false);
        return new UserViewHolder(view, listener, users);
    }

    @Override
    public void onBindViewHolder(UserViewHolder holder, int position) {
        OtherUser user = users.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView usernameTextView;
        private final ImageView avatarImageView;

        public UserViewHolder(View itemView, OnItemClickListener listener, List<OtherUser> users) {
            super(itemView);
            usernameTextView = itemView.findViewById(R.id.otherUserNameTextView);
            avatarImageView = itemView.findViewById(R.id.othersAvatarImageView);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(users.get(position));
                }
            });
        }

        public void bind(OtherUser user) {
            usernameTextView.setText(user.getUserName());
            Glide.with(itemView.getContext())
                    .load(user.getAvatarUrl())
                    .placeholder(R.drawable.coffee_default_avatar)
                    .into(avatarImageView);
        }

    }
}