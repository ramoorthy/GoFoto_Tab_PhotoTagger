/**
 * PhotoTagger
 * SettingsActivity.java
 * 
 * @author Jason Harris on Aug 22, 2012
 * @copyright 2012 Poggled, Inc. All rights reserved
 * 
 * Typical Usage: For the user to select settings to be used throughout
 * the application.
 * 
 */
package com.poggled.android.phototagger.ui.prefs;

import android.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import com.poggled.android.phototagger.R;
import com.poggled.android.phototagger.ui.HomeGridActivity;
import com.poggled.android.phototagger.util.Utils;

/**
 * This class is designed to use a multi-pane Honeycomb+ style layout for preferences.  The class does 
 * not currently support versions of android below Honeycomb.
 */
public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener{
    
    /* 
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
        
        // Set the initial state of the watermark preference
        Preference pref = findPreference(getString(R.string.pref_watermark_key));
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (pref != null) pref.setEnabled(!sharedPref.getBoolean(getString(R.string.pref_frame_enabled), false));
        
        if (Utils.hasActionBar()) {
            final ActionBar actionBar = getActionBar();

            // Enable "up" navigation on ActionBar icon and hide title text
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayUseLogoEnabled(true);
        }
    }

    public void onSharedPreferenceChanged(
            SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_frame_enabled))) {
            Preference pref = findPreference(getString(R.string.pref_watermark_key));
            if (pref != null) pref.setEnabled(!sharedPreferences.getBoolean(key, false));
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }
    
    /* 
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Home or "up" navigation
                final Intent intent = new Intent(this, HomeGridActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
//TODO: The following two methods are used for a two-pane preference panel using preference headers.  We
//  may want to add headers back in as preferences grow.
    
//    @Override
//    public void onBuildHeaders(List<Header> target) {
//        loadHeadersFromResource(R.xml.preference_headers, target);
//    }
//    public static class SettingsFragment extends PreferenceFragment {
//        
//        @Override
//        public void onCreate(Bundle savedInstanceState) {
//            super.onCreate(savedInstanceState);
//            addPreferencesFromResource(R.xml.prefs);
//            String settings = getArguments().getString(getActivity().getResources().getString(R.string.pref_header_intent_extra_key));
//            if (getActivity().getResources().getString(R.string.pref_gallery_header_title).equals(settings)) {
//                //addPreferencesFromResource(R.xml.prefs_whatever);
//                // Launch gallery select activity
//                //addPreferencesFromResource(R.xml.prefs_gallery);
//                
//            } else if (getActivity().getResources().getString(R.string.pref_watermark_header_title).equals(settings)) {
//                //addPreferencesFromResource(R.xml.prefs_watermark);
//            } else if (getActivity().getResources().getString(R.string.pref_frames_header_title).equals(settings)) {
//                //addPreferencesFromResource(R.xml.prefs_frame);
//            }
//        }
//
//        
//    }
}
