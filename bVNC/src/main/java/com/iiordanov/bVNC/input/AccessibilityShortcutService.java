package com.iiordanov.bVNC.input;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.view.inputmethod.InputMethodManager;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import com.iiordanov.bVNC.Constants;
import com.iiordanov.bVNC.Utils;

public class AccessibilityShortcutService extends AccessibilityService {

    private final SparseBooleanArray capturedKeyCodes = new SparseBooleanArray();

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
    public boolean onUnbind(Intent intent) {
        capturedKeyCodes.clear();
        return super.onUnbind(intent);
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (!Utils.querySharedPreferenceBoolean(this, Constants.captureShortcutKeysWithAccessibilityTag, true)) {
            return false;
        }
        if (!AccessibilityShortcutKeyDispatcher.hasCallback() || event == null) {
            return false;
        }
        if (isInputMethodAcceptingText()) {
            return false;
        }

        int keyCode = event.getKeyCode();
        int action = event.getAction();

        boolean isShortcutEvent = shouldCaptureWithAccessibility(event);
        boolean wasCapturedOnDown = capturedKeyCodes.get(keyCode, false);

        if (action == KeyEvent.ACTION_DOWN) {
            if (!isShortcutEvent) {
                return false;
            }
            capturedKeyCodes.put(keyCode, true);
        } else if (action == KeyEvent.ACTION_UP) {
            if (!isShortcutEvent && !wasCapturedOnDown) {
                return false;
            }
            capturedKeyCodes.delete(keyCode);
        } else if (!isShortcutEvent && !wasCapturedOnDown) {
            return false;
        }

        KeyEvent normalizedEvent = normalizeEventForRemote(event);
        return AccessibilityShortcutKeyDispatcher.dispatch(normalizedEvent);
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
        int keyCode = event.getKeyCode();

        if (isModifierKeyCode(keyCode)) {
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            return true;
        }
        if (keyCode >= KeyEvent.KEYCODE_F1 && keyCode <= KeyEvent.KEYCODE_F12) {
            return true;
        }

        // Do not capture general character typing here, even with modifier overlap,
        // otherwise keys may bypass remote IME composition and appear stuck on some ROMs.
        return false;
    }

    private boolean isModifierKeyCode(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_CTRL_LEFT
                || keyCode == KeyEvent.KEYCODE_CTRL_RIGHT
                || keyCode == KeyEvent.KEYCODE_ALT_LEFT
                || keyCode == KeyEvent.KEYCODE_ALT_RIGHT
                || keyCode == KeyEvent.KEYCODE_META_LEFT
                || keyCode == KeyEvent.KEYCODE_META_RIGHT
                || keyCode == KeyEvent.KEYCODE_FUNCTION;
    }

    private boolean isInputMethodAcceptingText() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        return imm != null && imm.isAcceptingText();
    }
}
