package com.k7sunny.balancex;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.k7sunny.balancex.security.AppLockActivity;
import com.k7sunny.balancex.security.AppLockManager;

public abstract class BaseSecureActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySecurityFlags();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applySecurityFlags();
        checkSessionLock();
    }

    private void applySecurityFlags() {
        if (SettingsManager.isFlagSecureEnabled(this)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    protected void checkSessionLock() {
        if (AppLockManager.getInstance().isLockRequired(this)) {
            Intent lockIntent = new Intent(this, AppLockActivity.class);
            lockIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(lockIntent);
        }
    }

    /**
     * Call before launching system activities (e.g. SAF, Camera, Contacts)
     * so that returning or canceling the picker does not trigger an app lock.
     */
    public void setLaunchingSystemPicker(boolean launching) {
        AppLockManager.getInstance().setLaunchingSystemPicker(launching);
    }
}
