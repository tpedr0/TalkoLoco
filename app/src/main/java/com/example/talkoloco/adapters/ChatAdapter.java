package com.example.talkoloco.adapters;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.talkoloco.databinding.ItemContainerReceivedMessageBinding;
import com.example.talkoloco.databinding.ItemContainerSentMessageBinding;
import com.example.talkoloco.models.ChatMessages;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final Bitmap receiverProfileImage;
    private final List<ChatMessages> chatMessages;
    private final String sendId;

    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;

    public ChatAdapter(List<ChatMessages> chatMessages, Bitmap receiverProfileImage, String sendId) {
        this.receiverProfileImage = receiverProfileImage;
        this.chatMessages = chatMessages;
        this.sendId = sendId;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            return new SentMessageViewHolder(ItemContainerSentMessageBinding
                    .inflate(LayoutInflater.from(parent.getContext()), parent, false));
        } else {
            return new ReceiverMessageViewHolder(ItemContainerReceivedMessageBinding
                    .inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (chatMessages == null || position >= chatMessages.size()) return;

        ChatMessages message = chatMessages.get(position);
        if (message == null) return;

        try {
            if (getItemViewType(position) == VIEW_TYPE_SENT) {
                ((SentMessageViewHolder) holder).setData(message);
            } else {
                ((ReceiverMessageViewHolder) holder).setData(message, receiverProfileImage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages != null ? chatMessages.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        try {
            if (chatMessages != null && position < chatMessages.size()) {
                ChatMessages message = chatMessages.get(position);
                if (message != null) {
                    String messageSenderId = message.getSenderId();
                    if (messageSenderId != null && sendId != null) {
                        return messageSenderId.equals(sendId) ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return VIEW_TYPE_RECEIVED; // Default to received if there's any issue
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerSentMessageBinding binding;

        public SentMessageViewHolder(ItemContainerSentMessageBinding itemContainerSentMessageBinding) {
            super(itemContainerSentMessageBinding.getRoot());
            binding = itemContainerSentMessageBinding;
        }

        void setData(ChatMessages chatMessages) {
            if (chatMessages != null) {
                binding.textMessage.setText(chatMessages.getMessage());
                binding.textDateTime.setText(chatMessages.getDateTime());
            }
        }
    }

    static class ReceiverMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerReceivedMessageBinding binding;

        public ReceiverMessageViewHolder(ItemContainerReceivedMessageBinding itemContainerReceivedMessageBinding) {
            super(itemContainerReceivedMessageBinding.getRoot());
            binding = itemContainerReceivedMessageBinding;
        }

        void setData(ChatMessages chatMessage, Bitmap receiverProfileImage) {
            if (chatMessage != null) {
                binding.textMessage.setText(chatMessage.getMessage());
                binding.textDateTime.setText(chatMessage.getDateTime());
            }

            if (receiverProfileImage != null && binding.imageProfile != null) {
                binding.imageProfile.setImageBitmap(receiverProfileImage);
            }
        }
    }
}