package com.coffeechat;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;
import java.util.Objects;

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
        String oldUid = oldList.get(oldItemPosition).getUid();
        String newUid = newList.get(newItemPosition).getUid();
        return oldUid != null && oldUid.equals(newUid);
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return Objects.equals(oldList.get(oldItemPosition), newList.get(newItemPosition));
    }
}