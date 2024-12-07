package com.example.talkoloco.adapters;

import android.graphics.Bitmap;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.talkoloco.databinding.ItemContainerReceivedMessageBinding;
import com.example.talkoloco.databinding.ItemContainerSentMessageBinding;
import com.example.talkoloco.models.ChatMessages;
import com.example.talkoloco.utils.ImageHandler;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final Bitmap receiverProfileImage;
    private final List<ChatMessages> chatMessages;
    private final String sendId;

    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;

    /**
     * Constructs a new ChatAdapter.
     *
     * @param chatMessages List of chat messages to display
     * @param receiverProfileImage Profile image of the message receiver
     * @param sendId ID of the message sender
     */
    public ChatAdapter(List<ChatMessages> chatMessages, Bitmap receiverProfileImage, String sendId) {
        this.receiverProfileImage = receiverProfileImage;
        this.chatMessages = chatMessages;
        this.sendId = sendId;
    }

    /**
     * Creates appropriate ViewHolder based on the message type (sent or received).
     *
     * @param parent The ViewGroup into which the new View will be added
     * @param viewType The view type of the new View (VIEW_TYPE_SENT or VIEW_TYPE_RECEIVED)
     * @return ViewHolder for either sent or received message
     */
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

    /**
     * Binds the chat message data to the appropriate ViewHolder based on position.
     *
     * @param holder The ViewHolder to bind data to
     * @param position The position of the item in the list
     */
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

    /**
     * Returns the total number of chat messages in the adapter.
     *
     * @return The number of chat messages
     */
    @Override
    public int getItemCount() {
        return chatMessages != null ? chatMessages.size() : 0;
    }

    /**
     * Determines the view type (sent or received) for the message at the given position.
     *
     * @param position The position of the item in the list
     * @return VIEW_TYPE_SENT if the message was sent, VIEW_TYPE_RECEIVED if received
     */
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

        /**
         * Constructor for SentMessageViewHolder.
         *
         * @param itemContainerSentMessageBinding Binding object for sent message layout
         */
        public SentMessageViewHolder(ItemContainerSentMessageBinding itemContainerSentMessageBinding) {
            super(itemContainerSentMessageBinding.getRoot());
            binding = itemContainerSentMessageBinding;
        }

        /**
         * Binds sent message data to the view.
         *
         * @param chatMessages The chat message data
         */
        void setData(ChatMessages chatMessages) {
            if (chatMessages != null) {
                if(chatMessages.getMessage().length() > 1000){
                    Bitmap imageBitmap = ImageHandler.decodeImage(chatMessages.getMessage());

                    binding.textMessage.setVisibility(View.GONE);
                    binding.imageMessage.setImageBitmap(imageBitmap);
                    binding.textDateTime.setText(chatMessages.getDateTime());
                }else {
                    binding.textMessage.setText(chatMessages.getMessage());
                    binding.textDateTime.setText(chatMessages.getDateTime());
                }
            }
        }
    }

    static class ReceiverMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerReceivedMessageBinding binding;

        /**
         * Constructor for ReceiverMessageViewHolder.
         *
         * @param itemContainerReceivedMessageBinding Binding object for received message layout
         */
        public ReceiverMessageViewHolder(ItemContainerReceivedMessageBinding itemContainerReceivedMessageBinding) {
            super(itemContainerReceivedMessageBinding.getRoot());
            binding = itemContainerReceivedMessageBinding;
        }

        /**
         * Binds received message data to the view.
         *
         * @param chatMessage The chat message data
         * @param receiverProfileImage The receiver's profile image
         */
        void setData(ChatMessages chatMessage, Bitmap receiverProfileImage) {
            if (chatMessage != null) {
                if(chatMessage.getMessage().length() > 1000){
                    Bitmap imageBitmap = ImageHandler.decodeImage(chatMessage.getMessage());

                    binding.textMessage.setVisibility(View.GONE);
                    binding.imageMessage.setImageBitmap(imageBitmap);
                    binding.textDateTime.setText(chatMessage.getDateTime());
                }else {
                    binding.textMessage.setText(chatMessage.getMessage());
                    binding.textDateTime.setText(chatMessage.getDateTime());
                }
            }

            if (receiverProfileImage != null && binding.imageProfile != null) {
                binding.imageProfile.setImageBitmap(receiverProfileImage);
            }
        }
    }
}