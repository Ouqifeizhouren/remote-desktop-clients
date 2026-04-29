package com.iiordanov.bVNC.input;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodManager;

import com.iiordanov.bVNC.Constants;
import com.iiordanov.bVNC.Utils;

public class AccessibilityShortcutService extends AccessibilityService {

    private final SparseBooleanArray capturedKeyCodes = new SparseBooleanArray();
    private final SparseBooleanArray pressedModifierKeys = new SparseBooleanArray();

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
        pressedModifierKeys.clear();
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

        int keyCode = event.getKeyCode();
        int action = event.getAction();

        updateModifierPressedState(keyCode, action);

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
        AccessibilityShortcutKeyDispatcher.dispatch(normalizedEvent);
        // Consume captured events so Android global shortcut handlers do not also trigger.
        return true;
    }

    private KeyEvent normalizeEventForRemote(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_APP_SWITCH && isAltPressed(event)) {
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

        if (keyCode == KeyEvent.KEYCODE_APP_SWITCH || (keyCode >= KeyEvent.KEYCODE_F1 && keyCode <= KeyEvent.KEYCODE_F12)) {
            return true;
        }

        if (isCharacterAreaKey(keyCode)) {
            return true;
        }

        if (isInputMethodAcceptingText()) {
            return false;
        }

        if (!isAnyShortcutModifierPressed(event)) {
            return false;
        }

        return isNonPrintingShortcutCompanionKey(keyCode);
    }

    private boolean isAnyShortcutModifierPressed(KeyEvent event) {
        return event.isCtrlPressed()
                || event.isAltPressed()
                || event.isMetaPressed()
                || event.isFunctionPressed()
                || pressedModifierKeys.get(KeyEvent.KEYCODE_CTRL_LEFT, false)
                || pressedModifierKeys.get(KeyEvent.KEYCODE_CTRL_RIGHT, false)
                || pressedModifierKeys.get(KeyEvent.KEYCODE_ALT_LEFT, false)
                || pressedModifierKeys.get(KeyEvent.KEYCODE_ALT_RIGHT, false)
                || pressedModifierKeys.get(KeyEvent.KEYCODE_META_LEFT, false)
                || pressedModifierKeys.get(KeyEvent.KEYCODE_META_RIGHT, false)
                || pressedModifierKeys.get(KeyEvent.KEYCODE_FUNCTION, false);
    }

    private boolean isNonPrintingShortcutCompanionKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_TAB
                || keyCode == KeyEvent.KEYCODE_ESCAPE
                || keyCode == KeyEvent.KEYCODE_MOVE_HOME
                || keyCode == KeyEvent.KEYCODE_MOVE_END
                || keyCode == KeyEvent.KEYCODE_PAGE_UP
                || keyCode == KeyEvent.KEYCODE_PAGE_DOWN
                || keyCode == KeyEvent.KEYCODE_INSERT
                || keyCode == KeyEvent.KEYCODE_FORWARD_DEL
                || keyCode == KeyEvent.KEYCODE_DEL
                || keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_DPAD_UP
                || keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                || keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT;
    }

    private boolean isCharacterAreaKey(int keyCode) {
        return (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z)
                || (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9)
                || keyCode == KeyEvent.KEYCODE_SPACE
                || keyCode == KeyEvent.KEYCODE_MINUS
                || keyCode == KeyEvent.KEYCODE_EQUALS
                || keyCode == KeyEvent.KEYCODE_LEFT_BRACKET
                || keyCode == KeyEvent.KEYCODE_RIGHT_BRACKET
                || keyCode == KeyEvent.KEYCODE_BACKSLASH
                || keyCode == KeyEvent.KEYCODE_SEMICOLON
                || keyCode == KeyEvent.KEYCODE_APOSTROPHE
                || keyCode == KeyEvent.KEYCODE_COMMA
                || keyCode == KeyEvent.KEYCODE_PERIOD
                || keyCode == KeyEvent.KEYCODE_SLASH
                || keyCode == KeyEvent.KEYCODE_GRAVE;
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

    private void updateModifierPressedState(int keyCode, int action) {
        if (!isModifierKeyCode(keyCode)) {
            return;
        }

        if (action == KeyEvent.ACTION_DOWN) {
            pressedModifierKeys.put(keyCode, true);
        } else if (action == KeyEvent.ACTION_UP) {
            pressedModifierKeys.delete(keyCode);
        }
    }

    private boolean isAltPressed(KeyEvent event) {
        return event.isAltPressed()
                || pressedModifierKeys.get(KeyEvent.KEYCODE_ALT_LEFT, false)
                || pressedModifierKeys.get(KeyEvent.KEYCODE_ALT_RIGHT, false);
    }

    private boolean isInputMethodAcceptingText() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        return imm != null && imm.isAcceptingText();
    }
}
