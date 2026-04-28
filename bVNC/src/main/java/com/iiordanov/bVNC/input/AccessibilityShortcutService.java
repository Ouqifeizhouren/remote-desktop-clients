package com.iiordanov.bVNC.input;

import android.accessibilityservice.AccessibilityService;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import com.iiordanov.bVNC.Constants;
import com.iiordanov.bVNC.Utils;

public class AccessibilityShortcutService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // No-op. We only use this service to capture hardware keyboard shortcuts.
    }

    @Override
    public void onInterrupt() {
        // No-op.
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (!Utils.querySharedPreferenceBoolean(this, Constants.captureShortcutKeysWithAccessibilityTag, true)) {
            return false;
        }
        if (!shouldCaptureWithAccessibility(event)) {
            return false;
        }
        return AccessibilityShortcutKeyDispatcher.dispatch(new KeyEvent(event));
    }

    private boolean shouldCaptureWithAccessibility(KeyEvent event) {
        if (event == null) {
            return false;
        }
        if (event.isCtrlPressed() || event.isAltPressed() || event.isMetaPressed() || event.isFunctionPressed()) {
            return true;
        }
        int keyCode = event.getKeyCode();
        return keyCode >= KeyEvent.KEYCODE_F1 && keyCode <= KeyEvent.KEYCODE_F12;
    }
}
