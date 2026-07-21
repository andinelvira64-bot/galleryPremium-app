package com.elvira.gallery;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Stores the 4 viewer effect toggles. All default to ON, matching the
 * reference screenshot. No settings screen toggle UI is needed elsewhere;
 * this is the single source of truth read by ViewerActivity.
 */
public class SettingsPrefs {

    private static final String PREFS_NAME = "gallery_effects";

    public static final String KEY_AUTO_COLORING = "auto_coloring";
    public static final String KEY_BLUR = "enable_blur";
    public static final String KEY_GLOW = "enable_glow";
    public static final String KEY_PARALLAX = "enable_parallax";

    private final SharedPreferences prefs;

    public SettingsPrefs(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isAutoColoringEnabled() {
        return prefs.getBoolean(KEY_AUTO_COLORING, true);
    }

    public boolean isBlurEnabled() {
        return prefs.getBoolean(KEY_BLUR, true);
    }

    public boolean isGlowEnabled() {
        return prefs.getBoolean(KEY_GLOW, true);
    }

    public boolean isParallaxEnabled() {
        return prefs.getBoolean(KEY_PARALLAX, true);
    }

    public void setAutoColoring(boolean value) {
        prefs.edit().putBoolean(KEY_AUTO_COLORING, value).apply();
    }

    public void setBlur(boolean value) {
        prefs.edit().putBoolean(KEY_BLUR, value).apply();
    }

    public void setGlow(boolean value) {
        prefs.edit().putBoolean(KEY_GLOW, value).apply();
    }

    public void setParallax(boolean value) {
        prefs.edit().putBoolean(KEY_PARALLAX, value).apply();
    }
}
