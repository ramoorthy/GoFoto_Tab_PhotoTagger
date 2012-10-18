/**
 * PhotoTagger
 * WatermarkActivity.java
 * 
 * @author Jason Harris on Jul 18, 2012
 * @copyright 2012 Poggled, Inc. All rights reserved
 * 
 * Typical Usage: 
 * 
 */
package com.poggled.android.phototagger.ui.prefs;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;

import com.poggled.android.phototagger.R;
import com.poggled.android.phototagger.ui.GoFotoFragmentActivity;
import com.poggled.android.phototagger.ui.HomeGridActivity;
import com.poggled.android.phototagger.util.Utils;

/**
 * 
 */
public class WatermarkActivity extends GoFotoFragmentActivity {

private static final String TAG = "WatermarkFragment";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
        if (getSupportFragmentManager().findFragmentByTag(TAG) == null) {
            if (savedInstanceState != null) {
                return;
            }
            
            WatermarkFragment newFragment = new WatermarkFragment();
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            
            newFragment.setArguments(getIntent().getExtras());
            ft.add(android.R.id.content, newFragment, TAG);
            ft.commit();
        }
        
        if (Utils.hasActionBar()) {
            final ActionBar actionBar = getActionBar();

            // Enable "up" navigation on ActionBar icon and hide title text
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayUseLogoEnabled(true);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.watermarks_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Home or "up" navigation
                final Intent intent = new Intent(this, HomeGridActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case R.id.delete_all_watermark:
                WatermarkFragment frag = (WatermarkFragment) getSupportFragmentManager().findFragmentByTag(TAG);
                if(frag != null) {
                    frag.deleteAndReset();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
  
}
