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

/**
 * Utility class for handling image processing operations.
 * Provides functionality for encoding, decoding, scaling, and validating images.
 */
public class ImageHandler {
    private static final String TAG = "ImageHandler";
    // Maximum allowed dimension (width or height) for images
    private static final int MAX_IMAGE_DIMENSION = 800;
    // JPEG compression quality for processed images
    private static final int COMPRESSION_QUALITY = 75;

    /**
     * Processes and encodes an image URI to a Base64 string.
     * Handles image scaling and compression for optimal storage and transmission.
     *
     * @param context The context to use for content resolution
     * @param imageUri The URI of the image to process
     * @return Base64 encoded string of the processed image
     * @throws IOException if image processing fails
     */
    public static String encodeImage(Context context, Uri imageUri) throws IOException {
        // Open input stream from URI
        InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

        if (bitmap == null) {
            throw new IOException("Failed to decode image");
        }

        // Scale the image down if it's too large
        bitmap = scaleBitmap(bitmap);

        // Convert to Base64 with compression
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, baos);
        byte[] imageBytes = baos.toByteArray();

        // Clean up resources
        bitmap.recycle();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    /**
     * Scales down a bitmap if it exceeds maximum dimensions while maintaining aspect ratio.
     *
     * @param original The original bitmap to scale
     * @return Scaled bitmap if necessary, or original bitmap if already within limits
     */
    private static Bitmap scaleBitmap(Bitmap original) {
        int width = original.getWidth();
        int height = original.getHeight();

        // Return original if within size limits
        if (width <= MAX_IMAGE_DIMENSION && height <= MAX_IMAGE_DIMENSION) {
            return original;
        }

        // Calculate scaling ratio to fit within bounds
        float ratio = Math.min(
                (float) MAX_IMAGE_DIMENSION / width,
                (float) MAX_IMAGE_DIMENSION / height
        );

        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
    }

    /**
     * Decodes a Base64 encoded string back to a Bitmap.
     *
     * @param base64String The Base64 encoded image string
     * @return Decoded Bitmap, or null if decoding fails
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
     * Validates if an encoded image's size is within acceptable limits.
     * Current limit is set to 1MB after Base64 encoding.
     *
     * @param base64String The Base64 encoded image to check
     * @return boolean indicating if the image size is valid
     */
    public static boolean isImageSizeValid(String base64String) {
        // Checking if encoded image is less than 1MB
        return base64String.length() * 3/4 < 1024 * 1024;
    }
}