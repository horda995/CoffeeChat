package com.coffeechat;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

public class UserDiffCallback extends DiffUtil.Callback {
    private final List<OtherUser> oldList;
    private final List<OtherUser> newList;

    public UserDiffCallback(List<OtherUser> oldList, List<OtherUser> newList) {
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
        return oldList.get(oldItemPosition).getUserName()
                .equals(newList.get(newItemPosition).getUserName());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
    }
}