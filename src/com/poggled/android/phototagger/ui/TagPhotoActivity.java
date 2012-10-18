package com.poggled.android.phototagger.ui;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;

import com.poggled.android.phototagger.util.Utils;
//import android.content.Intent;
//import android.net.Uri;
//import android.view.Menu;
//import android.view.MenuInflater;
//import android.view.MenuItem;
//import android.widget.Toast;
//import com.poggled.android.phototagger.R;
//import com.poggled.android.phototagger.ui.prefs.SettingsActivity;
//import com.poggled.android.phototagger.util.DiskLruCache;
//import com.poggled.android.phototagger.util.ImageCache;

/**
 * Simple FragmentActivity to hold the main {@link TagPhotoFragment}.
 */
public class TagPhotoActivity extends GoFotoFragmentActivity {
	private static final String TAG = "TagPhotoFragment";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getSupportFragmentManager().findFragmentByTag(TAG) == null) {
			if (savedInstanceState != null) {
				return;
			}
			TagPhotoFragment newFragment = new TagPhotoFragment();
			final FragmentTransaction ft = getSupportFragmentManager()
					.beginTransaction();

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

}
