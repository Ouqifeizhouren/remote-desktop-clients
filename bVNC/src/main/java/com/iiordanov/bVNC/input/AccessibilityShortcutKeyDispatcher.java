package com.iiordanov.bVNC.input;

import android.view.KeyEvent;

public final class AccessibilityShortcutKeyDispatcher {
    public interface Callback {
        boolean onAccessibilityKeyEvent(KeyEvent event);
    }

    private static Callback callback;

    private AccessibilityShortcutKeyDispatcher() {
    }

    public static synchronized void registerCallback(Callback eventCallback) {
        callback = eventCallback;
    }

    public static synchronized void unregisterCallback() {
        callback = null;
    }

    public static synchronized boolean hasCallback() {
        return callback != null;
    }

    public static synchronized boolean dispatch(KeyEvent event) {
        if (callback == null || event == null) {
            return false;
        }
        return callback.onAccessibilityKeyEvent(event);
    }
}
