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
    /**
     * Constructs a new UsersAdapter.
     *
     * @param users List of User objects to be displayed
     * @param userListener Listener for handling user click events
     * @param context Application context for resource access
     */
    public UsersAdapter(List<User> users, UserListener userListener, Context context) {
        this.users = users;
        this.userListener = userListener;
        this.context = context;  // Store the context for later use
    }

    /**
     * Creates a new ViewHolder when needed by the RecyclerView.
     *
     * @param parent The ViewGroup into which the new View will be added
     * @param viewType The view type of the new View
     * @return A new UserViewHolder that holds a View of the given view type
     */
    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContainerUserBinding itemContainerUserBinding = ItemContainerUserBinding
                .inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new UserViewHolder(itemContainerUserBinding);
    }

    /**
     * Updates the contents of the ViewHolder to reflect the item at the given position.
     *
     * @param holder The ViewHolder to update
     * @param position The position of the item within the adapter's data set
     */
    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.setUserData(users.get(position), context);  // Pass context to ViewHolder
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of users in the list
     */
    @Override
    public int getItemCount() {
        return users.size();
    }

    /**
     * ViewHolder class for user items in the RecyclerView.
     * Handles the display of user information and profile images.
     */
    class UserViewHolder extends RecyclerView.ViewHolder {
        ItemContainerUserBinding binding;


        /**
         * Constructs a new UserViewHolder.
         *
         * @param itemContainerUserBinding The view binding for the user item layout
         */
        public UserViewHolder(ItemContainerUserBinding itemContainerUserBinding) {
            super(itemContainerUserBinding.getRoot());
            binding = itemContainerUserBinding;
        }

        /**
         * Sets the user data in the ViewHolder.
         * Updates the name, profile image, and click listener for the user item.
         *
         * @param user The User object containing the data to display
         * @param context The context used for resource access
         */
        void setUserData(User user, Context context) {
            binding.textName.setText(user.name);
            binding.imageProfile.setImageBitmap(getUserImage(user.image, context));  // Pass context here

            binding.getRoot().setOnClickListener(v -> userListener.onUserClicked(user));
        }

        /**
         * Converts a Base64 encoded string to a Bitmap for the user's profile image.
         * Returns a default profile image if the encoded string is null or empty.
         *
         * @param encodedImage Base64 encoded string of the user's profile image
         * @param context The context used for accessing default image resource
         * @return Bitmap of the user's profile image or default image
         */
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
