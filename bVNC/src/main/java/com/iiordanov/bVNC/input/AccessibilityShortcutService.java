package com.iiordanov.bVNC.input;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import com.iiordanov.bVNC.Constants;
import com.iiordanov.bVNC.Utils;

public class AccessibilityShortcutService extends AccessibilityService {

    private final SparseBooleanArray dispatchedKeyCodes = new SparseBooleanArray();
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
        dispatchedKeyCodes.clear();
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

        boolean shouldDispatch = shouldDispatchWithAccessibility(event);
        boolean wasDispatchedOnDown = dispatchedKeyCodes.get(keyCode, false);

        if (action == KeyEvent.ACTION_DOWN) {
            if (!shouldDispatch) {
                return false;
            }
            dispatchedKeyCodes.put(keyCode, true);
        } else if (action == KeyEvent.ACTION_UP) {
            if (!wasDispatchedOnDown) {
                return false;
            }
            dispatchedKeyCodes.delete(keyCode);
        } else if (!shouldDispatch && !wasDispatchedOnDown) {
            return false;
        }

        KeyEvent normalizedEvent = normalizeEventForRemote(event);
        AccessibilityShortcutKeyDispatcher.dispatch(normalizedEvent);

        // Consume every dispatched event to prevent duplicate/out-of-order delivery
        // from the framework key path, which can invert down/up state on some ROMs.
        return true;
    }

    private KeyEvent normalizeEventForRemote(KeyEvent event) {
        int metaState = event.getMetaState();
        if (isAltPressed(event)) metaState |= KeyEvent.META_ALT_ON;
        if (isCtrlPressed(event)) metaState |= KeyEvent.META_CTRL_ON;
        if (isShiftPressed(event)) metaState |= KeyEvent.META_SHIFT_ON;
        if (isMetaPressed(event)) metaState |= KeyEvent.META_META_ON;

        if (event.getKeyCode() == KeyEvent.KEYCODE_APP_SWITCH && (metaState & KeyEvent.META_ALT_ON) != 0) {
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
        return new KeyEvent(
                event.getDownTime(),
                event.getEventTime(),
                event.getAction(),
                event.getKeyCode(),
                event.getRepeatCount(),
                metaState,
                event.getDeviceId(),
                event.getScanCode(),
                event.getFlags(),
                event.getSource()
        );
    }

    private boolean shouldDispatchWithAccessibility(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            return false;
        }
        // Dispatch all keys to the remote system via the accessibility service.
        // This prevents the local Android OS from intercepting system shortcuts
        // like Win+D, Alt+Tab, etc., and allows the remote system to handle them.
        return true;
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

    private boolean isMetaPressed(KeyEvent event) {
        return event.isMetaPressed()
                || pressedModifierKeys.get(KeyEvent.KEYCODE_META_LEFT, false)
                || pressedModifierKeys.get(KeyEvent.KEYCODE_META_RIGHT, false);
    }

    private boolean isCtrlPressed(KeyEvent event) {
        return event.isCtrlPressed()
                || pressedModifierKeys.get(KeyEvent.KEYCODE_CTRL_LEFT, false)
                || pressedModifierKeys.get(KeyEvent.KEYCODE_CTRL_RIGHT, false);
    }

    private boolean isShiftPressed(KeyEvent event) {
        return event.isShiftPressed()
                || pressedModifierKeys.get(KeyEvent.KEYCODE_SHIFT_LEFT, false)
                || pressedModifierKeys.get(KeyEvent.KEYCODE_SHIFT_RIGHT, false);
    }
}
