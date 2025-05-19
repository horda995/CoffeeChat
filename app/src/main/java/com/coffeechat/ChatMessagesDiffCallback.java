package com.coffeechat;

import androidx.recyclerview.widget.DiffUtil;

import com.google.firebase.Timestamp;

import java.util.List;

public class ChatMessagesDiffCallback extends DiffUtil.Callback {
    private final List<ChatMessages> oldList;
    private final List<ChatMessages> newList;

    public ChatMessagesDiffCallback(List<ChatMessages> oldList, List<ChatMessages> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        ChatMessages oldItem = oldList.get(oldItemPosition);
        ChatMessages newItem = newList.get(newItemPosition);

        Timestamp oldTimestamp = oldItem.getTimestamp();
        Timestamp newTimestamp = newItem.getTimestamp();

        if (oldTimestamp == null || newTimestamp == null) return false;

        return oldTimestamp.equals(newTimestamp)
                && oldItem.getSentByUid().equals(newItem.getSentByUid());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
    }
}