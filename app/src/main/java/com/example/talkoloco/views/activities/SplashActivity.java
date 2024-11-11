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

public class SplashActivity extends AppCompatActivity {
    private ActivitySplashPortraitBinding portraitBinding;
    private ActivitySplashLandscapeBinding landscapeBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // gets current orientation
        int orientation = getResources().getConfiguration().orientation;

        // sets appropriate layout and binding based on orientation
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

    private final Animator.AnimatorListener animatorListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {}

        @Override
        public void onAnimationEnd(Animator animation) {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }

        @Override
        public void onAnimationCancel(Animator animation) {}

        @Override
        public void onAnimationRepeat(Animator animation) {}
    };

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // handles orientation change and sets appropriate layout
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        portraitBinding = null;
        landscapeBinding = null;
    }
}
