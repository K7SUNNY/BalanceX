package com.k7sunny.balancex.util;

import android.util.Log;

import com.k7sunny.balancex.BuildConfig;

/**
 * Privacy-preserving logger.
 * Strips all debug/info logging in production release builds to prevent
 * financial metadata and sensitive ledger information from leaking into system logcat.
 */
public final class AppLogger {

    private AppLogger() {}

    public static void d(String tag, String message) {
        if (BuildConfig.DEBUG && message != null) {
            try {
                Log.d(tag, message);
            } catch (Throwable t) {
                System.out.println("[" + tag + "] " + message);
            }
        }
    }

    public static void i(String tag, String message) {
        if (BuildConfig.DEBUG && message != null) {
            try {
                Log.i(tag, message);
            } catch (Throwable t) {
                System.out.println("[" + tag + "] " + message);
            }
        }
    }

    public static void w(String tag, String message) {
        if (BuildConfig.DEBUG && message != null) {
            try {
                Log.w(tag, message);
            } catch (Throwable t) {
                System.err.println("[" + tag + "] " + message);
            }
        }
    }

    public static void w(String tag, String message, Throwable tr) {
        if (BuildConfig.DEBUG) {
            try {
                Log.w(tag, message, tr);
            } catch (Throwable t) {
                System.err.println("[" + tag + "] " + message);
                if (tr != null) tr.printStackTrace();
            }
        }
    }

    public static void e(String tag, String message) {
        if (BuildConfig.DEBUG && message != null) {
            try {
                Log.e(tag, message);
            } catch (Throwable t) {
                System.err.println("[" + tag + "] " + message);
            }
        }
    }

    public static void e(String tag, String message, Throwable tr) {
        if (BuildConfig.DEBUG) {
            try {
                Log.e(tag, message, tr);
            } catch (Throwable t) {
                System.err.println("[" + tag + "] " + message);
                if (tr != null) tr.printStackTrace();
            }
        }
    }
}
