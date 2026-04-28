package com.iiordanov.bVNC;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.undatech.remoteClientUi.R;

public class GlobalPreferencesFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        getPreferenceManager().setSharedPreferencesName(Constants.generalSettingsTag);
        setPreferencesFromResource(R.xml.global_preferences, s);
        if (Utils.isVnc(getContext())) {
            addPreferencesFromResource(R.xml.global_preferences_vnc);
        } else if (Utils.isRdp(getContext())) {
            addPreferencesFromResource(R.xml.global_preferences_rdp);
        } else if (Utils.isSpice(getContext())) {
            addPreferencesFromResource(R.xml.global_preferences_spice);
        }
        setupAccessibilityShortcutPreferences();
    }

    private void setupAccessibilityShortcutPreferences() {
        Preference accessibilitySettings = findPreference("accessibilityShortcutSettings");
        if (accessibilitySettings == null) {
            return;
        }
        accessibilitySettings.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            return true;
        });
    }
}
