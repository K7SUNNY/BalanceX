package com.accounting.balancex;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private long startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        // Find views
        TextView title = findViewById(R.id.textView);
        TextView tagline = findViewById(R.id.textView2);

        // Load animations
        Animation fadeScale = AnimationUtils.loadAnimation(this, R.anim.fade_scale);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);

        // Start animations
        title.startAnimation(fadeScale);
        tagline.startAnimation(slideUp);

        // Start measuring load time
        startTime = System.currentTimeMillis();

        // Post a runnable to run once UI is fully drawn
        getWindow().getDecorView().post(() -> {
            long elapsedTime = System.currentTimeMillis() - startTime;
            long minSplashTime = 1500; // Minimum splash duration (1.5 sec)
            long delay = Math.max(0, minSplashTime - elapsedTime);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }, delay);
        });
    }
}
