package com.accounting.balancex;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsManager.applyTheme(this);
        setContentView(R.layout.activity_about);

        setupWindowInsets();
        setupViews();
    }

    private void setupWindowInsets() {
        View root = findViewById(R.id.aboutRoot);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
                return windowInsets;
            });
        }
    }

    private void setupViews() {
        // Back navigation button
        View backButton = findViewById(R.id.btnBack);
        if (backButton == null) {
            backButton = findViewById(R.id.imageViewBack);
        }
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        // Version dynamic text
        TextView versionText = findViewById(R.id.versionText);
        if (versionText != null) {
            versionText.setText("Version " + getAppVersion());
        }

        // Share App Action
        MaterialButton btnShareApp = findViewById(R.id.btnShareApp);
        if (btnShareApp != null) {
            btnShareApp.setOnClickListener(v -> shareApp());
        }
    }

    private void shareApp() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "BalanceX - Private Accounting App");
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                "Track your income, expenses, and accounting records with BalanceX — 100% offline, private, and ad-free!");
        startActivity(Intent.createChooser(shareIntent, "Share BalanceX via"));
    }

    private String getAppVersion() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return packageInfo.versionName != null ? packageInfo.versionName : "1.0.0";
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0.0";
        }
    }
}
