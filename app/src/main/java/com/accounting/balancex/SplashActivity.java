package com.accounting.balancex;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Find views
        TextView appName = findViewById(R.id.app_name);
        TextView appSlogan = findViewById(R.id.app_slogan);

        // Load animations
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideOutLeft = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);
        Animation sloganFadeIn = AnimationUtils.loadAnimation(this, R.anim.slogan_fade_in);

        // Show app name with fade-in
        appName.setVisibility(TextView.VISIBLE);
        appName.startAnimation(fadeIn);

        // After fade-in, slide the app name left and show slogan
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            appName.startAnimation(slideOutLeft);

            // Once slide-out finishes, hide app name to prevent overlapping
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                appName.setVisibility(TextView.INVISIBLE);
                appSlogan.setVisibility(TextView.VISIBLE);
                appSlogan.startAnimation(sloganFadeIn);
            }, 50); // Delay slightly to match slide-out duration

        }, 1000); // Delay before sliding (same as fade-in duration)

        // Move to MainActivity after animations
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing()) { // Only start MainActivity if SplashActivity is still active
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }
        }, 3000); // Your splash delay
        // Total animation time

        // Find the version TextView
        TextView versionView = findViewById(R.id.version_view);

        // Get the app version dynamically
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            versionView.setText("Version: " + version);
        } catch (PackageManager.NameNotFoundException e) {
            versionView.setText("Version: Unknown");
            e.printStackTrace();
        }
    }
}
