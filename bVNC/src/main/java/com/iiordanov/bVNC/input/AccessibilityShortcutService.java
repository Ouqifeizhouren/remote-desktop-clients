package com.iiordanov.bVNC.input;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
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
    protected void onServiceConnected() {
        AccessibilityServiceInfo serviceInfo = getServiceInfo();
        if (serviceInfo == null) {
            return;
        }
        serviceInfo.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        setServiceInfo(serviceInfo);
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (!Utils.querySharedPreferenceBoolean(this, Constants.captureShortcutKeysWithAccessibilityTag, true)) {
            return false;
        }
        if (!shouldCaptureWithAccessibility(event)) {
            return false;
        }
        if (!AccessibilityShortcutKeyDispatcher.hasCallback()) {
            return false;
        }
        KeyEvent normalizedEvent = normalizeEventForRemote(event);
        AccessibilityShortcutKeyDispatcher.dispatch(normalizedEvent);
        // Important: consume matched shortcut keys whenever an active remote session is listening,
        // otherwise Android may still execute global shortcuts (e.g. Alt+Tab) on some ROMs.
        return true;
    }

    private KeyEvent normalizeEventForRemote(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_APP_SWITCH && event.isAltPressed()) {
            int metaState = event.getMetaState();
            if ((metaState & (KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON | KeyEvent.META_ALT_RIGHT_ON)) == 0) {
                metaState |= KeyEvent.META_ALT_ON;
            }
            return new KeyEvent(
                    event.getDownTime(),
                    event.getEventTime(),
                    event.getAction(),
                    KeyEvent.KEYCODE_TAB,
                    event.getRepeatCount(),
                    metaState,
                    event.getDeviceId(),
                    event.getScanCode(),
                    event.getFlags(),
                    event.getSource()
            );
        }
        return new KeyEvent(event);
    }

    private boolean shouldCaptureWithAccessibility(KeyEvent event) {
        if (event == null) {
            return false;
        }
        if (event.isCtrlPressed() || event.isAltPressed() || event.isMetaPressed() || event.isFunctionPressed()) {
            return true;
        }
        int keyCode = event.getKeyCode();
        if (keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            return true;
        }
        return keyCode >= KeyEvent.KEYCODE_F1 && keyCode <= KeyEvent.KEYCODE_F12;
    }
}
