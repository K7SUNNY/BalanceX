package com.k7sunny.balancex.security;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.Executor;

public class BiometricLockHelper {

    public interface AuthCallback {
        void onSuccess();
        default void onFailure(String errorMsg) {}
        default void onFailure(int errorCode, String errorMsg) {
            onFailure(errorMsg);
        }
        default void onScanFailed() {}
    }

    public static boolean canAuthenticate(Context context) {
        if (context == null) return false;
        try {
            BiometricManager manager = BiometricManager.from(context);
            int canAuth = manager.canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG |
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            );
            return canAuth == BiometricManager.BIOMETRIC_SUCCESS;
        } catch (Exception e) {
            return false;
        }
    }

    public static void promptAuthentication(
            FragmentActivity activity,
            String title,
            String subtitle,
            AuthCallback callback
    ) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(activity);
        BiometricPrompt biometricPrompt = new BiometricPrompt(activity, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                String msg = errString.toString();
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    msg = "Authentication canceled";
                } else if (errorCode == BiometricPrompt.ERROR_LOCKOUT) {
                    msg = "Too many attempts. Sensor temporarily locked.";
                } else if (errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT) {
                    msg = "Biometrics locked. Please unlock using your device PIN or password.";
                }
                if (callback != null) {
                    callback.onFailure(errorCode, msg);
                }
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                // A soft scan failure does not terminate authentication.
                // The system dialog remains open for the user to try again.
                if (callback != null) {
                    callback.onScanFailed();
                }
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG |
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build();

        biometricPrompt.authenticate(promptInfo);
    }
}
