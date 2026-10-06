package com.accounting.balancex;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class ProfileHelper {

    private static final String TAG = "ProfileHelper";
    public static final String PREF_NAME = "UserProfile";

    public static final String KEY_USER_NAME = "userName";
    public static final String KEY_BIO = "bio";
    public static final String KEY_COMPANY_NAME = "companyName";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_PHONE = "phone";
    public static final String KEY_ADDRESS = "address";
    public static final String KEY_PROFILE_IMAGE_URI = "profileImageUri";
    public static final String KEY_INCLUDE_IN_PDF = "includeInPdf";

    public static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Loads the saved avatar into an ImageView freshly from disk to avoid Android ImageView URI caching.
     */
    public static void loadAvatar(Context context, ImageView imageView) {
        if (imageView == null || context == null) return;

        // Invalidate internal ImageView URI cache so Android won't skip updating
        imageView.setImageDrawable(null);
        imageView.setImageURI(null);

        SharedPreferences prefs = getPrefs(context);
        String uriString = prefs.getString(KEY_PROFILE_IMAGE_URI, "");
        if (uriString.isEmpty()) {
            imageView.setImageResource(R.drawable.ic_account);
            return;
        }

        try {
            Uri uri = Uri.parse(uriString);
            Bitmap bitmap = null;

            if ("file".equalsIgnoreCase(uri.getScheme())) {
                File file = new File(uri.getPath());
                if (file.exists() && file.length() > 0) {
                    bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                }
            }

            if (bitmap == null) {
                try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                    if (is != null) {
                        bitmap = BitmapFactory.decodeStream(is);
                    }
                }
            }

            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(R.drawable.ic_account);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading avatar image", e);
            imageView.setImageResource(R.drawable.ic_account);
        }
    }

    /**
     * Copies the sourceUri to app internal storage with a unique filename, updates SharedPreferences,
     * and returns the new persistent file URI.
     */
    public static Uri saveProfileImage(Context context, Uri sourceUri) {
        if (context == null || sourceUri == null) return null;
        try (InputStream is = context.getContentResolver().openInputStream(sourceUri)) {
            if (is == null) return null;

            cleanupOldAvatarFiles(context);

            File newFile = new File(context.getFilesDir(), "profile_avatar_" + System.currentTimeMillis() + ".jpg");
            try (OutputStream os = new FileOutputStream(newFile)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = is.read(buffer)) > 0) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }

            Uri internalUri = Uri.fromFile(newFile);
            getPrefs(context).edit()
                    .putString(KEY_PROFILE_IMAGE_URI, internalUri.toString())
                    .commit(); // Use synchronous commit so data is immediately saved

            return internalUri;
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy profile image to internal storage", e);
            return null;
        }
    }

    /**
     * Removes the profile image file and clears the URI preference.
     */
    public static void removeProfileImage(Context context) {
        if (context == null) return;
        cleanupOldAvatarFiles(context);
        getPrefs(context).edit()
                .remove(KEY_PROFILE_IMAGE_URI)
                .commit();
    }

    private static void cleanupOldAvatarFiles(Context context) {
        try {
            File filesDir = context.getFilesDir();
            File[] files = filesDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().startsWith("profile_avatar_") || f.getName().startsWith("profile_image")) {
                        f.delete();
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error cleaning old avatar files", e);
        }
    }
}
