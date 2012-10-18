package com.poggled.android.phototagger.ui;

import android.app.ActionBar;
import android.os.Bundle;
import android.widget.GridView;

import com.poggled.android.phototagger.R;
//import android.util.Log;
//import android.view.View;

public class HomeGridActivity extends GoFotoFragmentActivity implements
		ActionBar.TabListener {

	GridView grid = null;
	ImageAdapter adapter = null;
	private static final String TAG = "ImageGridFragment";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super.setActiveTab(0);
		super.createBar();
		setContentView(R.layout.main);
		// ----------------------------------
		// setContentView(R.layout.grid);

		adapter = new ImageAdapter(HomeGridActivity.this, "grid");

		grid = (GridView) findViewById(R.id.gridView);
		grid.setAdapter(adapter);
		/*
		 * grid.setOnItemClickListener(new OnItemClickListener() {
		 * 
		 * @Override public void onItemClick(AdapterView<?> arg0, View arg1, int
		 * pos, long id) { Intent i = new Intent(GridActivity.this,
		 * GalleryActivity.class); i.putExtra("selectedIntex", pos);
		 * startActivity(i); } });
		 */
		// ----------------------------------
		// Check that the activity is using the layout version with
		// the fragment_container FrameLayout
		//
		// if (findViewById(R.id.files_frag) != null) {
		//
		// However, if we're being restored from a previous state,
		// // then we don't need to do anything and should return or else
		// // we could end up with overlapping fragments.
		// if (savedInstanceState != null) {
		// return;
		// }
		//
		// // Create an instance of FileListFragment
		// FileListFragment fileListFragment = new FileListFragment();
		//
		// // In case this activity was started with special instructions from
		// an Intent,
		// // pass the Intent's extras to the fragment as arguments
		// fileListFragment.setArguments(getIntent().getExtras());
		//
		// // Add the fragment to the 'fragment_container' FrameLayout
		// getSupportFragmentManager().beginTransaction()
		// .add(R.id.files_frag, fileListFragment).commit();
		// }
		/*
		 * final FragmentTransaction ft = getSupportFragmentManager()
		 * .beginTransaction(); if
		 * (getSupportFragmentManager().findFragmentByTag(TAG) == null) {
		 * ft.add(R.id.photos_frag, new HomeGridFragment(), TAG); } ft.commit();
		 */

	}

}