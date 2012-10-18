/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.poggled.android.phototagger.ui;

import java.util.ArrayList;
import java.util.Calendar;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.Toast;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;
import com.google.android.apps.analytics.easytracking.EasyTracker;
import com.poggled.android.phototagger.BuildConfig;
import com.poggled.android.phototagger.PhotoTaggerApplication;
import com.poggled.android.phototagger.R;
import com.poggled.android.phototagger.provider.Images;
import com.poggled.android.phototagger.provider.PoggledService;
import com.poggled.android.phototagger.ui.prefs.SettingsActivity;
import com.poggled.android.phototagger.util.DiskLruCache;
import com.poggled.android.phototagger.util.FacebookConnector;
import com.poggled.android.phototagger.util.ImageCache;
import com.poggled.android.phototagger.util.ImageCache.ImageCacheParams;
import com.poggled.android.phototagger.util.ImageLoader;
import com.poggled.android.phototagger.util.ImageResizer;
import com.poggled.android.phototagger.util.ImageWorker;
import com.poggled.android.phototagger.util.Utils;

public class PhotoDetailActivity extends GoFotoFragmentActivity implements
		OnClickListener {

	private static final String IMAGE_CACHE_DIR = "images";
	// public static final String EXTRA_IMAGE = "extra_image";

	// private static final int DIALOG_FACEBOOK_AUTH = 1;

	/** State held between configuration changes. */
	private State mState;

	private ImagePagerAdapter mAdapter;
	private ImageResizer mImageWorker;
	private ViewPager mPager;

	private Button mFacebookSignIn;

	private FacebookConnector mFacebookConnector;
	// private ProgressDialog mProgressDialog;
	private boolean mCreateAccount = true;

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.photo_detail_activity);

		mState = (State) getLastCustomNonConfigurationInstance();
		final boolean previousState = mState != null;

		if (previousState) {
			mState.mRegisterTask.attach(this);
		} else {
			mState = new State();
		}
		// Fetch screen height and width, to use as our max size when loading
		// images as this
		// activity runs full screen
		final DisplayMetrics displaymetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		final int height = displaymetrics.heightPixels;
		final int width = displaymetrics.widthPixels;
		final int longest = height > width ? height : width;

		if (BuildConfig.DEBUG) {
			Log.d(getClass().getName(), "longest display size is " + longest);
			Log.d(getClass().getName(), "width is " + width);
			Log.d(getClass().getName(), "height is " + height);
		}

		ImageCacheParams cacheParams = new ImageCacheParams(IMAGE_CACHE_DIR);

		// Allocate a fourth of the per-app memory limit to the bitmap memory
		// cache. This value
		// should be chosen carefully based on a number of factors.
		cacheParams.memCacheSize = 1024 * 1024 * Utils.getMemoryClass(this) / 4;

		// The ImageWorker takes care of loading images into our ImageView
		// children asynchronously
		mImageWorker = new ImageLoader(this, width, height);
		mImageWorker.setAdapter(new Images(this));
		mImageWorker.setImageCache(ImageCache.findOrCreateCache(this,
				cacheParams));
		mImageWorker.setImageFadeIn(true);

		// Set up ViewPager and backing adapter
		mAdapter = new ImagePagerAdapter(getSupportFragmentManager(),
				mImageWorker.getAdapter().getSize());
		mPager = (ViewPager) findViewById(R.id.pager);
		mPager.setAdapter(mAdapter);
		mPager.setPageMargin((int) getResources().getDimension(
				R.dimen.image_detail_pager_margin));

		mFacebookSignIn = (Button) findViewById(R.id.btnFacebookSignIn);
		mFacebookSignIn.setOnClickListener(this);

		// Enable some additional newer visibility and ActionBar features to
		// create a more immersive
		// photo viewing experience
		if (Utils.hasActionBar()) {
			final ActionBar actionBar = getActionBar();

			// Enable "up" navigation on ActionBar icon and hide title text
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setDisplayUseLogoEnabled(true);
		}

		// Set the current item based on the extra passed in to this activity
		final int extraCurrentItem = getIntent().getIntExtra("extra_image", -1);
		if (extraCurrentItem != -1) {
			mPager.setCurrentItem(extraCurrentItem);
		}

		mFacebookConnector = PhotoTaggerApplication.getFacebookConnector();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.FragmentActivity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (mState.mRegisterTask != null)
			mState.mRegisterTask.detach();
		// SessionEvents.removeAuthListener(mFbAuthListener);
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance() {
		// Need to call the super implementation to make sure tracking is
		// handled correctly.
		// We won't keep track of the return object since we are going to
		// overwrite it.
		super.onRetainCustomNonConfigurationInstance();
		// Clear any strong references to this Activity, we'll reattach to
		// handle events on the other side.
		if (mState.mRegisterTask != null)
			mState.mRegisterTask.detach();
		return mState;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refresh:
			Images img = new Images(this);
			mImageWorker.setAdapter(img);
			mAdapter.setSize(img.getSize());
			mAdapter.notifyDataSetChanged();
			return true;
		case android.R.id.home:
			// Home or "up" navigation
			final Intent intent = new Intent(this, HomeGridActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		case R.id.clear_cache:
			final ImageCache cache = mImageWorker.getImageCache();
			if (cache != null) {
				mImageWorker.getImageCache().clearCaches();
				DiskLruCache.clearCache(this, IMAGE_CACHE_DIR);
				Toast.makeText(this, R.string.clear_cache_complete,
						Toast.LENGTH_SHORT).show();
			}
			return true;
		case R.id.send_email:
			final String path = String.valueOf(mImageWorker.getAdapter()
					.getItem(mPager.getCurrentItem()));
			EasyTracker.getTracker()
					.trackEvent("Photo Share", "Email", path, 0);
			final Intent emailIntent = new Intent(Intent.ACTION_SEND);
			emailIntent.setType("plain/html");
			emailIntent.putExtra(Intent.EXTRA_SUBJECT,
					getString(R.string.email_subject, Calendar.getInstance()));
			emailIntent.putExtra(Intent.EXTRA_STREAM,
					Uri.parse("file://" + path));
			startActivity(emailIntent);
			break;
		case R.id.settings:
			final Intent i = new Intent(this, SettingsActivity.class);
			startActivity(i);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateDialog(int, android.os.Bundle)
	 */
	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		Dialog dialog;
		if (id == 1) {
			dialog = mFacebookConnector.getCustomDialogAuth(this);
		} else {
			dialog = super.onCreateDialog(id, args);
		}
		/*
		 * switch(id) { case DIALOG_FACEBOOK_AUTH: dialog =
		 * mFacebookConnector.getCustomDialogAuth(this); break; default: dialog
		 * = super.onCreateDialog(id, args); }
		 */
		return dialog;

	}

	/**
	 * Called by the ViewPager child fragments to load images via the one
	 * ImageWorker
	 * 
	 * @return
	 */
	public ImageWorker getImageWorker() {
		return mImageWorker;
	}

	/**
	 * The main adapter that backs the ViewPager. A subclass of
	 * FragmentStatePagerAdapter as there could be a large number of items in
	 * the ViewPager and we don't want to retain them all in memory at once but
	 * create/destroy them on the fly.
	 */
	private class ImagePagerAdapter extends FragmentStatePagerAdapter {
		private int mSize;

		public ImagePagerAdapter(FragmentManager fm, int size) {
			super(fm);
			mSize = size;
		}

		@Override
		public int getCount() {
			return mSize;
		}

		@Override
		public Fragment getItem(int position) {
			return PhotoDetailFragment.newInstance(position);
		}

		// TODO: write getItemPosition to return actual position so that updates
		// from a file observer will
		// correctly update position for dataset updates
		// public int getItemPosition(Object item) {
		// MyFragment fragment = (MyFragment)item;
		// String title = fragment.getTitle();
		// int position = titles.indexOf(title);
		//
		// if (position >= 0) {
		// return position;
		// } else {
		// return POSITION_NONE;
		// }
		// }

		@Override
		public int getItemPosition(Object object) {
			return POSITION_NONE;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			final PhotoDetailFragment fragment = (PhotoDetailFragment) object;
			// As the item gets destroyed we try and cancel any existing work.
			fragment.cancelWork();
			super.destroyItem(container, position, object);
		}

		public void setSize(int size) {
			mSize = size;
		}
	}

	/**
	 * Set on the ImageView in the ViewPager children fragments, to launch the
	 * facebook dialog or tagging view when the ImageView is touched.
	 */
	@SuppressLint("NewApi")
	public void onClick(View v) {
		/*
		 * Only call authorize if the access_token has expired.
		 */
		if (!mFacebookConnector.getFacebook().isSessionValid()) {

			// TODO: refactor dialog to a dialog fragment so we can remove
			// manifest that restrict re-layout on config changes

			// removed since dialog was not formatting /retaining state on
			// orientation changes
			// showDialog(DIALOG_FACEBOOK_AUTH);

			Dialog authDialog = mFacebookConnector.getCustomDialogAuth(this,
					new DialogListener() {
						public void onComplete(Bundle values) {
							// getFacebookUserInfo();
							if (mCreateAccount) {
								registerPoggledAccount(mFacebookConnector
										.getFacebookToken());
							}
							// proceed to photo tagging view
							launchTagPhotoActivity();
						}

						public void onFacebookError(FacebookError e) {

							Toast.makeText(
									PhotoDetailActivity.this,
									"Unable to connect to Facebook.  Error: "
											+ e.toString(), Toast.LENGTH_LONG)
									.show();
						}

						public void onError(DialogError e) {
							Toast.makeText(
									PhotoDetailActivity.this,
									"Unable to connect to Facebook.  Error: "
											+ e.toString(), Toast.LENGTH_LONG)
									.show();

						}

						public void onCancel() {
							// we really don't need to bug the user if they
							// cancel the dialog
						}

					});

			authDialog.show();
			CheckedTextView chkBox = (CheckedTextView) authDialog
					.findViewById(R.id.checktxt_create_account);
			chkBox.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					final CheckedTextView check = (CheckedTextView) v;
					check.toggle();
					mCreateAccount = check.isChecked();

				}
			});

		} else {
			// proceed to photo tagging view
			launchTagPhotoActivity();
		}

	}

	private void registerPoggledAccount(String oauthToken) {

		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		NameValuePair format = new BasicNameValuePair("format",
				getString(R.string.response_format));
		params.add(format);
		NameValuePair token = new BasicNameValuePair("facebook_token",
				oauthToken);
		params.add(token);
		NameValuePair accountSource = new BasicNameValuePair(
				"source_shortname", getString(R.string.register_account_source));
		params.add(accountSource);
		// TODO: hardcoded brand source until this can be populated from gallery
		// object
		// NameValuePair brandSource = new
		// BasicNameValuePair("source_brand_shortname", "ruthschris");
		// params.add(brandSource);

		mState.mRegisterTask = new RegisterTask(this);
		mState.mRegisterTask.execute(params);

	}

	// TODO: refactor to use a loader

	private static class RegisterTask extends
			AsyncTask<ArrayList<NameValuePair>, Void, Boolean> {
		private PhotoDetailActivity mActivity;
		private Exception mException;

		public RegisterTask(PhotoDetailActivity activity) {
			mActivity = activity;
		}

		public void attach(PhotoDetailActivity activity) {
			mActivity = activity;
		}

		public void detach() {
			mActivity = null;
		}

		@Override
		protected Boolean doInBackground(ArrayList<NameValuePair>... values) {
			ArrayList<NameValuePair> params = values[0];

			Boolean response = false;

			try {
				PoggledService service = new PoggledService(mActivity);
				response = service.register(params);
			} catch (Exception e) {
				mException = e;
			}
			return response;
		}

		@Override
		protected void onPostExecute(Boolean success) {
			if (mActivity == null && !isCancelled()) {
				if (BuildConfig.DEBUG)
					Log.w(getClass().getName(),
							"onPostExecute() skipped -- no activity");
			} else {
				if (mException != null) {
					if (BuildConfig.DEBUG)
						Log.e(getClass().getName(),
								"Problem while connecting to web service",
								mException);
				}
				if (mException == null && success) {
					Toast.makeText(mActivity,
							"Poggled account created successfully.",
							Toast.LENGTH_LONG).show();
				} else {

					Toast.makeText(
							mActivity,
							"There was a problem creating your Poggled account. Visit poggled.com to complete.",
							Toast.LENGTH_LONG).show();
				}

			}
		}
	}

	private void launchTagPhotoActivity() {
		final Intent i = new Intent(this, TagPhotoActivity.class);
		i.putExtra(
				TagPhotoFragment.IMAGE_DATA_EXTRA,
				String.valueOf(getImageWorker().getAdapter().getItem(
						mPager.getCurrentItem())));
		startActivity(i);
	}

	/**
	 * State specific to {@link LoginPage} that is held between configuration
	 * changes. Any strong {@link Activity} references <strong>must</strong> be
	 * cleared before {@link #onRetainNonConfigurationInstance()}, and this
	 * class should remain {@code static class}.
	 */
	private static class State {
		public RegisterTask mRegisterTask;
	}

}
