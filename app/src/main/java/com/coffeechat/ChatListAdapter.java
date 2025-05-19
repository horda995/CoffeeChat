package com.coffeechat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    private final List<OtherUser> chatList;
    private final Context context;

    private ChatListAdapter.OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(OtherUser user);
    }
    private int position;

    public ChatListAdapter(Context context, List<OtherUser> chatList) {
        this.context = context;
        this.chatList = chatList;
    }
    public void setOnItemClickListener(ChatListAdapter.OnItemClickListener listener) {
        this.listener = listener;
    }
    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, messageTextView;
        ShapeableImageView avatarImageView;

        public ChatViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.chatListUserNameTextView);
            messageTextView = itemView.findViewById(R.id.chatListMessageTextView);
            avatarImageView = itemView.findViewById(R.id.chatlistUserAvatarImageView);
        }
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chatlist_list_item, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        OtherUser chat = chatList.get(position);
        holder.nameTextView.setText(chat.getUserName());
        holder.messageTextView.setText(chat.getLastMessage());

        if (chat.getAvatarUrl() != null && !chat.getAvatarUrl().isEmpty()) {
            Glide.with(context)
                    .load(chat.getAvatarUrl())
                    .placeholder(R.drawable.coffee_default_avatar)
                    .into(holder.avatarImageView);
        } else {
            holder.avatarImageView.setImageResource(R.drawable.coffee_default_avatar);
        }
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public void updateList(List<OtherUser> newList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new UserDiffCallback(chatList, newList));
        chatList.clear();
        chatList.addAll(newList);
        diffResult.dispatchUpdatesTo(this);
    }

    public void updateLastMessage(String chatId, String messageText) {
        for (int i = 0; i < chatList.size(); i++) {
            OtherUser user = chatList.get(i);
            if (user.getChatId().equals(chatId)) {
                user.setLastMessage(messageText);
                notifyItemChanged(i);
                break;
            }
        }
    }
}
