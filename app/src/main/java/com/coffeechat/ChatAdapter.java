package com.coffeechat;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import android.media.MediaPlayer;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private ChatViewHolder lastPlayingHolder;

    public void stopPlayback() {
        if (lastPlayingHolder != null) {
            lastPlayingHolder.releaseMediaPlayer();
            lastPlayingHolder = null;
        }
    }

    private final List<ChatMessages> messages = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ChatMessages message);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ChatMessages> newList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ChatMessagesDiffCallback(messages, newList));
        messages.clear();
        messages.addAll(newList);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.chat_item, parent, false);
        return new ChatViewHolder(view, listener, messages, this);
    }

    @Override
    public void onBindViewHolder(ChatViewHolder holder, int position) {
        ChatMessages message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        private final TextView usernameTextView;
        private final TextView messageTextView;
        private final ImageButton playPauseButton;
        private final ChatAdapter adapter;

        private MediaPlayer mediaPlayer = null;
        private boolean isPlaying = false;

        public void resetPlayButton() {
            if (playPauseButton != null) {
                playPauseButton.setImageResource(android.R.drawable.ic_media_play);
            }
        }
        public ChatViewHolder(View itemView, OnItemClickListener listener, List<ChatMessages> messages, ChatAdapter adapter) {
            super(itemView);
            this.adapter = adapter;

            usernameTextView = itemView.findViewById(R.id.chatUserNameTextViewLabel);
            messageTextView = itemView.findViewById(R.id.chatBubbleTextView);
            playPauseButton = itemView.findViewById(R.id.playPauseButton);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(messages.get(position));
                }
            });
        }

        public void bind(ChatMessages message) {
            String currentUserUid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("users")
                    .document(message.getSentByUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            usernameTextView.setText(username);

                            String soundUrl = message.getSoundUrl();

                            if (message.getMessageText() == null || message.getMessageText().isEmpty()) {
                                if (soundUrl != null && !soundUrl.isEmpty()) {

                                    messageTextView.setVisibility(View.INVISIBLE);
                                    playPauseButton.setVisibility(View.VISIBLE);
                                    playPauseButton.setImageResource(android.R.drawable.ic_media_play);

                                    playPauseButton.setOnClickListener(v -> {
                                        if (isPlaying) {
                                            mediaPlayer.pause();
                                            playPauseButton.setImageResource(android.R.drawable.ic_media_play);
                                            isPlaying = false;
                                        } else {
                                            if (adapter.lastPlayingHolder != null && adapter.lastPlayingHolder != this) {
                                                adapter.lastPlayingHolder.resetPlayButton();
                                                adapter.stopPlayback();
                                            }


                                            if (mediaPlayer == null) {
                                                mediaPlayer = new MediaPlayer();
                                                try {
                                                    mediaPlayer.setDataSource(soundUrl);
                                                    mediaPlayer.prepareAsync();
                                                    mediaPlayer.setOnPreparedListener(mp -> {
                                                        mp.start();
                                                        playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
                                                        isPlaying = true;
                                                    });
                                                    mediaPlayer.setOnCompletionListener(mp -> {
                                                        playPauseButton.setImageResource(android.R.drawable.ic_media_play);
                                                        isPlaying = false;
                                                        releaseMediaPlayer();
                                                    });
                                                } catch (Exception e) {
                                                    Log.e("AudioPlayback", "Error playing audio", e);
                                                }
                                            } else {
                                                mediaPlayer.start();
                                                playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
                                                isPlaying = true;
                                            }

                                            adapter.lastPlayingHolder = this;
                                        }
                                    });
                                } else {
                                    playPauseButton.setVisibility(View.GONE);
                                }
                            } else {
                                messageTextView.setText(message.getMessageText());
                                playPauseButton.setVisibility(View.GONE);
                            }

                            DisplayMetrics metrics = itemView.getResources().getDisplayMetrics();
                            int maxWidth = (int) (metrics.widthPixels * 0.6);
                            messageTextView.setMaxWidth(maxWidth);

                            ConstraintLayout constraintLayout = itemView.findViewById(R.id.chatConstraintLayout);
                            ConstraintSet constraintSet = new ConstraintSet();
                            constraintSet.clone(constraintLayout);

                            if (message.getSentByUid().equals(currentUserUid)) {
                                constraintSet.clear(R.id.chatBubbleTextView, ConstraintSet.START);
                                constraintSet.connect(R.id.chatBubbleTextView, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);

                                constraintSet.clear(R.id.chatUserNameTextViewLabel, ConstraintSet.START);
                                constraintSet.connect(R.id.chatUserNameTextViewLabel, ConstraintSet.END, R.id.chatBubbleTextView, ConstraintSet.END);

                                constraintSet.clear(R.id.playPauseButton, ConstraintSet.START);
                                constraintSet.connect(R.id.playPauseButton, ConstraintSet.END, R.id.chatBubbleTextView, ConstraintSet.END);
                            } else {
                                constraintSet.clear(R.id.chatBubbleTextView, ConstraintSet.END);
                                constraintSet.connect(R.id.chatBubbleTextView, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);

                                constraintSet.clear(R.id.chatUserNameTextViewLabel, ConstraintSet.END);
                                constraintSet.connect(R.id.chatUserNameTextViewLabel, ConstraintSet.START, R.id.chatBubbleTextView, ConstraintSet.START);

                                constraintSet.clear(R.id.playPauseButton, ConstraintSet.END);
                                constraintSet.connect(R.id.playPauseButton, ConstraintSet.START, R.id.chatBubbleTextView, ConstraintSet.START);
                            }

                            constraintSet.applyTo(constraintLayout);
                        }
                    })
                    .addOnFailureListener(e -> Log.e("FirebaseDB", "Error", e));
        }

        private void releaseMediaPlayer() {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
                isPlaying = false;
            }
        }
    }
}
