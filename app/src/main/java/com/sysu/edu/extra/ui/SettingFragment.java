package com.sysu.edu.extra.ui;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import com.sysu.edu.R;
import com.sysu.edu.preference.MenuPreference;
import com.sysu.edu.preference.Theme;

import java.util.Objects;

public class SettingFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);
        Theme theme = new Theme(requireActivity());
        theme.setTheme();
        ((MenuPreference) Objects.requireNonNull(findPreference("theme"))).setOnPreferenceChangeListener((_, _) -> {
            requireActivity().recreate();
            return true;
        });
        ((MenuPreference) Objects.requireNonNull(findPreference("icon_theme"))).setOnPreferenceChangeListener((_, newValue) -> {
            PackageManager pm = requireActivity().getPackageManager();
            String pkg = requireContext().getPackageName();
            pm.setComponentEnabledSetting(new ComponentName(requireActivity().getBaseContext(), pkg + ".MainActivity"), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            pm.setComponentEnabledSetting(new ComponentName(requireActivity().getBaseContext(), pkg + ".MainActivityLight"), (new int[]{1, 2, 2})[Integer.parseInt((String) newValue)], PackageManager.DONT_KILL_APP);
            pm.setComponentEnabledSetting(new ComponentName(requireActivity().getBaseContext(), pkg + ".MainActivityDark"), (new int[]{2, 1, 2})[Integer.parseInt((String) newValue)], PackageManager.DONT_KILL_APP);
            pm.setComponentEnabledSetting(new ComponentName(requireActivity().getBaseContext(), pkg + ".MainActivityDefault"), (new int[]{2, 2, 1})[Integer.parseInt((String) newValue)], PackageManager.DONT_KILL_APP);
            return true;
        });
        ((MenuPreference) Objects.requireNonNull(findPreference("language"))).setOnPreferenceChangeListener((_, _) -> {
                    requireActivity().recreate();
                    return true;
                }
        );
        ((MenuPreference) Objects.requireNonNull(findPreference("fontSize"))).setOnPreferenceChangeListener((_, _) -> {
                    requireActivity().recreate();
                    return true;
                }
        );
    }
}