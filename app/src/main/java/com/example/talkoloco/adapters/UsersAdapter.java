package com.example.talkoloco.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.talkoloco.R;
import com.example.talkoloco.databinding.ItemContainerUserBinding;
import com.example.talkoloco.listeners.UserListener;
import com.example.talkoloco.models.User;

import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    private final List<User> users;
    private final UserListener userListener;
    private final Context context;  // Add context here

    // Pass context in constructor
    public UsersAdapter(List<User> users, UserListener userListener, Context context) {
        this.users = users;
        this.userListener = userListener;
        this.context = context;  // Store the context for later use
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContainerUserBinding itemContainerUserBinding = ItemContainerUserBinding
                .inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new UserViewHolder(itemContainerUserBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.setUserData(users.get(position), context);  // Pass context to ViewHolder
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        ItemContainerUserBinding binding;

        public UserViewHolder(ItemContainerUserBinding itemContainerUserBinding) {
            super(itemContainerUserBinding.getRoot());
            binding = itemContainerUserBinding;
        }

        void setUserData(User user, Context context) {
            binding.textName.setText(user.name);
            binding.imageProfile.setImageBitmap(getUserImage(user.image, context));  // Pass context here

            binding.getRoot().setOnClickListener(v -> userListener.onUserClicked(user));
        }

        private Bitmap getUserImage(String encodedImage, Context context) {
            if (encodedImage == null || encodedImage.isEmpty()) {
                // Return a default image if encodedImage is null or empty
                Log.e("UsersAdapter", "Encoded image is null or empty");
                return BitmapFactory.decodeResource(context.getResources(), R.drawable.default_pfp);
            }else {
                // Decode the Base64 string
                byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            }
        }
    }
}
