package com.example.talkoloco.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageHandler {
    private static final String TAG = "ImageHandler";
    private static final int MAX_IMAGE_DIMENSION = 800;
    private static final int COMPRESSION_QUALITY = 75;

    /**
     * Processes and encodes an image URI to a Base64 string
     */
    public static String encodeImage(Context context, Uri imageUri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

        if (bitmap == null) {
            throw new IOException("Failed to decode image");
        }

        // Scale the image down if it's too large
        bitmap = scaleBitmap(bitmap);

        // Convert to Base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, baos);
        byte[] imageBytes = baos.toByteArray();

        bitmap.recycle();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    /**
     * Scales down the bitmap if it exceeds maximum dimensions
     */
    private static Bitmap scaleBitmap(Bitmap original) {
        int width = original.getWidth();
        int height = original.getHeight();

        if (width <= MAX_IMAGE_DIMENSION && height <= MAX_IMAGE_DIMENSION) {
            return original;
        }

        float ratio = Math.min(
                (float) MAX_IMAGE_DIMENSION / width,
                (float) MAX_IMAGE_DIMENSION / height
        );

        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
    }

    /**
     * Decodes a Base64 string back to a Bitmap
     */
    public static Bitmap decodeImage(String base64String) {
        try {
            byte[] imageBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Error decoding image", e);
            return null;
        }
    }

    /**
     * Validates if the image size is within acceptable limits
     */
    public static boolean isImageSizeValid(String base64String) {
        // Checking if encoded image is less than 1MB
        return base64String.length() * 3/4 < 1024 * 1024;
    }
}