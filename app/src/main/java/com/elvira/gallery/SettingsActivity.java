package com.elvira.gallery;

import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private SettingsPrefs prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = new SettingsPrefs(this);

        Switch switchAutoColoring = findViewById(R.id.switchAutoColoring);
        Switch switchBlur = findViewById(R.id.switchBlur);
        Switch switchGlow = findViewById(R.id.switchGlow);
        Switch switchParallax = findViewById(R.id.switchParallax);

        switchAutoColoring.setChecked(prefs.isAutoColoringEnabled());
        switchBlur.setChecked(prefs.isBlurEnabled());
        switchGlow.setChecked(prefs.isGlowEnabled());
        switchParallax.setChecked(prefs.isParallaxEnabled());

        switchAutoColoring.setOnCheckedChangeListener((CompoundButton b, boolean checked) -> prefs.setAutoColoring(checked));
        switchBlur.setOnCheckedChangeListener((CompoundButton b, boolean checked) -> prefs.setBlur(checked));
        switchGlow.setOnCheckedChangeListener((CompoundButton b, boolean checked) -> prefs.setGlow(checked));
        switchParallax.setOnCheckedChangeListener((CompoundButton b, boolean checked) -> prefs.setParallax(checked));
    }
}
