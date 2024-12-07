package com.example.talkoloco.views.activities;

import android.animation.Animator;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.talkoloco.R;
import com.example.talkoloco.databinding.ActivitySplashPortraitBinding;
import com.example.talkoloco.databinding.ActivitySplashLandscapeBinding;

/**
 * Entry point activity that displays a splash screen animation.
 * Handles both portrait and landscape orientations with different layouts and animations.
 * Automatically transitions to MainActivity after the animation completes.
 */
public class SplashActivity extends AppCompatActivity {
    // View binding objects for different orientations
    private ActivitySplashPortraitBinding portraitBinding;
    private ActivitySplashLandscapeBinding landscapeBinding;

    /**
     * Initializes the activity and sets up the appropriate layout and animation
     * based on the current screen orientation.
     *
     * @param savedInstanceState If non-null, this activity is being re-initialized after previously being shut down
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Determine current screen orientation
        int orientation = getResources().getConfiguration().orientation;

        // Configure layout and animation based on orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            landscapeBinding = ActivitySplashLandscapeBinding.inflate(getLayoutInflater());
            setContentView(landscapeBinding.getRoot());

            // sets the splash screen animation view for landscape
            landscapeBinding.splashscreenLandscape.setScaleType(com.airbnb.lottie.LottieAnimationView.ScaleType.CENTER_CROP);
            landscapeBinding.splashscreenLandscape.setAnimation(R.raw.splashscreen_landscape);
            landscapeBinding.splashscreenLandscape.playAnimation();
            landscapeBinding.splashscreenLandscape.addAnimatorListener(animatorListener);
        } else {
            portraitBinding = ActivitySplashPortraitBinding.inflate(getLayoutInflater());
            setContentView(portraitBinding.getRoot());

            // setss the splash screen animation view for portrait
            portraitBinding.splashscreenPortrait.setScaleType(com.airbnb.lottie.LottieAnimationView.ScaleType.CENTER_CROP);
            portraitBinding.splashscreenPortrait.setAnimation(R.raw.splashscreen_portrait);
            portraitBinding.splashscreenPortrait.playAnimation();
            portraitBinding.splashscreenPortrait.addAnimatorListener(animatorListener);
        }
    }


    /**
     * Animation listener that handles the transition to MainActivity
     * once the splash animation completes.
     */
    private final Animator.AnimatorListener animatorListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
            // Not used, but required by interface
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            // Start MainActivity and finish this activity when animation ends
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            // Not used, but required by interface
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
            // Not used, but required by interface
        }
    };

    /**
     * Handles runtime changes in screen orientation.
     * Reconfigures the layout and restarts the animation when orientation changes.
     *
     * @param newConfig The new device configuration
     */
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Reconfigure layout based on new orientation
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Clean up portrait binding if it exists
            if (portraitBinding != null) {
                portraitBinding = null;
            }
            landscapeBinding = ActivitySplashLandscapeBinding.inflate(getLayoutInflater());
            setContentView(landscapeBinding.getRoot());
            landscapeBinding.splashscreenLandscape.setScaleType(com.airbnb.lottie.LottieAnimationView.ScaleType.CENTER_CROP);
            landscapeBinding.splashscreenLandscape.setAnimation(R.raw.splashscreen_landscape);
            landscapeBinding.splashscreenLandscape.playAnimation();
            landscapeBinding.splashscreenLandscape.addAnimatorListener(animatorListener);
        } else {
            // Clean up landscape binding if it exists
            if (landscapeBinding != null) {
                landscapeBinding = null;
            }
            portraitBinding = ActivitySplashPortraitBinding.inflate(getLayoutInflater());
            setContentView(portraitBinding.getRoot());
            portraitBinding.splashscreenPortrait.setScaleType(com.airbnb.lottie.LottieAnimationView.ScaleType.CENTER_CROP);
            portraitBinding.splashscreenPortrait.setAnimation(R.raw.splashscreen_portrait);
            portraitBinding.splashscreenPortrait.playAnimation();
            portraitBinding.splashscreenPortrait.addAnimatorListener(animatorListener);
        }
    }

    /**
     * Cleans up resources when the activity is destroyed.
     * Prevents memory leaks by nullifying view bindings.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        portraitBinding = null;
        landscapeBinding = null; // Prevents memory leaks
    }
}
