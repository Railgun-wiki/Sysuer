package com.sysu.edu.browser;

import android.content.Context;
import android.content.SharedPreferences;

public class BrowserPreference {

    private final Context context;
    private final SharedPreferences preference;

    public BrowserPreference(Context context) {
        this.context = context;
        preference = context.getSharedPreferences("browser", Context.MODE_PRIVATE);
    }

    public int getUA() {
        return preference.getInt("ua", 0);
    }

    public void setUA(int ua) {
        preference.edit().putInt("ua", ua).apply();
    }

    public boolean isPC() {
        return preference.getBoolean("pc", false);
    }

    public void setPC(boolean pc) {
        preference.edit().putBoolean("pc", pc).apply();
    }

    public boolean isImageBlocked() {
        return preference.getBoolean("image_blocked", false);
    }

    public void setImageBlocked(boolean imageBlocked) {
        preference.edit().putBoolean("image_blocked", imageBlocked).apply();
    }

    public boolean isJSEnabled() {
        return preference.getBoolean("javascript_enabled", true);
    }

    public void setJSEnabled(boolean jsEnabled) {
        preference.edit().putBoolean("javascript_enabled", jsEnabled).apply();
    }

     public boolean isSaveMobileDataMode() {
        return preference.getBoolean("save_mobile_data_mode", false);
    }

    public void setSaveMobileDataMode(boolean saveMobileDataMode) {
        preference.edit().putBoolean("save_mobile_data_mode", saveMobileDataMode).apply();
    }

    public Context getContext() {
        return context;
    }
}
