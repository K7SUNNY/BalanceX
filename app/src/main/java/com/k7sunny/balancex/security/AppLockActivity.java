package com.k7sunny.balancex.security;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.k7sunny.balancex.R;
import com.k7sunny.balancex.SettingsManager;

public class AppLockActivity extends AppCompatActivity {

    private TextView textStatus;
    private boolean isPromptActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Lock screen always hides content in recents and prevents screenshots
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.activity_app_lock);

        textStatus = findViewById(R.id.textLockStatus);
        View btnUnlock = findViewById(R.id.btnUnlockApp);

        if (btnUnlock != null) {
            btnUnlock.setOnClickListener(v -> triggerAuthentication());
        }

        // Prevent bypassing lock by pressing back - minimize the app instead
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                moveTaskToBack(true);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!SettingsManager.isBiometricLockEnabled(this) || AppLockManager.getInstance().isSessionAuthenticated()) {
            finish();
            return;
        }
        triggerAuthentication();
    }

    private void triggerAuthentication() {
        if (isPromptActive) return;

        if (!BiometricLockHelper.canAuthenticate(this)) {
            if (textStatus != null) {
                textStatus.setText("Device lock (PIN/Biometric) is not configured on this device.");
                textStatus.setVisibility(View.VISIBLE);
            }
            return;
        }

        isPromptActive = true;
        if (textStatus != null) {
            textStatus.setVisibility(View.GONE);
        }

        BiometricLockHelper.promptAuthentication(
                this,
                "Unlock BalanceX",
                "Scan fingerprint or enter device PIN / password",
                new BiometricLockHelper.AuthCallback() {
                    @Override
                    public void onSuccess() {
                        isPromptActive = false;
                        AppLockManager.getInstance().setSessionAuthenticated(true);
                        finish();
                    }

                    @Override
                    public void onFailure(int errorCode, String errorMsg) {
                        isPromptActive = false;
                        if (textStatus != null) {
                            textStatus.setText(errorMsg);
                            textStatus.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onScanFailed() {
                        // Soft failure - dialog remains active
                        if (textStatus != null) {
                            textStatus.setText("Sensor did not recognize scan. Try again.");
                            textStatus.setVisibility(View.VISIBLE);
                        }
                    }
                }
        );
    }
}
