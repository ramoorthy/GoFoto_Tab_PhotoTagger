/**
 * PhotoTagger
 * PhotoFragment.java
 * 
 * @author Jason Harris on Jun 19, 2012
 * @copyright 2012 Poggled, Inc. All rights reserved
 * 
 * Typical Usage: Called from Photo Detail fragment to display photos 
 * and allow the user to begin tagging.
 * 
 */
package com.poggled.android.phototagger.ui;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.FacebookError;
import com.facebook.android.Util;
import com.google.android.apps.analytics.easytracking.EasyTracker;
import com.poggled.android.phototagger.BuildConfig;
import com.poggled.android.phototagger.PhotoTaggerApplication;
import com.poggled.android.phototagger.R;
import com.poggled.android.phototagger.ui.prefs.SettingsActivity;
import com.poggled.android.phototagger.ui.widget.TagableImageView;
import com.poggled.android.phototagger.util.BaseRequestListener;
import com.poggled.android.phototagger.util.DiskLruCache;
import com.poggled.android.phototagger.util.FacebookConnector;
import com.poggled.android.phototagger.util.FileLoggerMessage;
import com.poggled.android.phototagger.util.ImageCache;
import com.poggled.android.phototagger.util.ImageCache.ImageCacheParams;
import com.poggled.android.phototagger.util.ImageFetcher;
import com.poggled.android.phototagger.util.ImageLoader;
import com.poggled.android.phototagger.util.ImageResizer;
import com.poggled.android.phototagger.util.ImageWorker;
import com.poggled.android.phototagger.util.SessionStore;
import com.poggled.android.phototagger.util.Utils;

public class TagPhotoFragment extends Fragment implements OnClickListener,
		OnTouchListener, OnItemClickListener {

	private static final String IMAGE_CACHE_DIR = "images";
	public static final String IMAGE_DATA_EXTRA = "pic";
	private TagableImageView mImageView;
	private RelativeLayout mTagFrame;
	private String mImagePath;
	private ImageResizer mImageWorker;

	private FacebookConnector mFacebookConnector;
	private ProgressDialog mProgressDialog;

	private PopupWindow mPopUp;

	private FriendsArrayAdapter mFriendsAdapter = null;

	private ArrayList<Tag> mTags = new ArrayList<Tag>();

	// Empty constructor, required as per Fragment docs
	public TagPhotoFragment() {
	}

	/*
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		setRetainInstance(true);

		mImagePath = getArguments() != null ? getArguments().getString("pic")
				: "";

		// Fetch screen height and width, to use as our max size when loading
		// images as this
		// activity runs full screen
		final DisplayMetrics displaymetrics = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay()
				.getMetrics(displaymetrics);
		final int height = displaymetrics.heightPixels;
		final int width = displaymetrics.widthPixels;

		// same directory as image details so we can hit the same disk cache
		ImageCacheParams cacheParams = new ImageCacheParams(IMAGE_CACHE_DIR);

		// We shouldn't need too much memory for this activity since it is only
		// one image
		cacheParams.memCacheSize = 1024 * 1024 * 4;

		// TODO: add a background image while loading. (Do not add a progress
		// indicator or it will force
		// the taggable image to continually redraw, wasting resources.)

		// The ImageWorker takes care of loading images into our ImageView
		// children asynchronously
		mImageWorker = new ImageLoader(getActivity(), width, height);
		mImageWorker.setImageCache(ImageCache.findOrCreateCache(getActivity(),
				cacheParams));
		mImageWorker.setImageFadeIn(false);

		mFacebookConnector = PhotoTaggerApplication.getFacebookConnector();

		final String id = SessionStore.restoreFacebookId(getActivity());
		if (id != null) {
			getFacebookFriends();
		} else {
			getFacebookUserInfo(false);
		}
		mFriendsAdapter = new FriendsArrayAdapter(getActivity(),
				R.id.txtFacebookName);

	}

	/*
	 * {@inheritDoc}
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View v = inflater
				.inflate(R.layout.tag_photo_fragment, container, false);

		mImageView = ((TagableImageView) v.findViewById(R.id.tagImageView));
		mTagFrame = (RelativeLayout) v.findViewById(R.id.frameTagImage);

		mImageView.setOnTouchListener(this);
		mImageWorker.loadImage(mImagePath, mImageView);

		((Button) v.findViewById(R.id.btnUpload)).setOnClickListener(this);

		return v;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.Fragment#onDestroy()
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		ImageWorker.cancelWork(mImageView);

		if (mPopUp != null) {
			mPopUp.dismiss();
			mPopUp = null;
		}

	}

	/*
	 * {@inheritDoc}
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mImageView.setOnClickListener(this);
	}

	/*
	 * {@inheritDoc}
	 */
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.main_menu, menu);
	}

	/*
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// Home or "up" navigation
			final Intent intent = new Intent(getActivity(),
					HomeGridActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		case R.id.clear_cache:
			final ImageCache cache = mImageWorker.getImageCache();
			if (cache != null) {
				mImageWorker.getImageCache().clearCaches();
				DiskLruCache.clearCache(getActivity(), IMAGE_CACHE_DIR);
				Toast.makeText(getActivity(), R.string.clear_cache_complete,
						Toast.LENGTH_SHORT).show();
			}
			return true;
		case R.id.send_email:
			EasyTracker.getTracker().trackEvent("Email Share", "Email",
					mImagePath, 0);
			final Intent emailIntent = new Intent(Intent.ACTION_SEND);
			emailIntent.setType("plain/html");
			emailIntent.putExtra(Intent.EXTRA_SUBJECT,
					getString(R.string.email_subject, Calendar.getInstance()));
			emailIntent.putExtra(Intent.EXTRA_STREAM,
					Uri.parse("file://" + mImagePath));
			startActivity(emailIntent);
			break;
		case R.id.settings:
			final Intent i = new Intent(getActivity(), SettingsActivity.class);
			startActivity(i);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/*
	 * {@inheritDoc}
	 */

	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.btnUpload:
			final String id = SessionStore.restoreFacebookId(getActivity());
			if (id != null) {
				postPhoto(id);
			} else {
				getFacebookUserInfo(true);
			}
			break;
		case R.id.layoutFacebookMe:
			// hide this view in the popup since we won't allow a person to tag
			// themselves multiple times
			((View) mPopUp.getContentView().findViewById(R.id.layoutFacebookMe))
					.setVisibility(View.GONE);
			final String idNum = SessionStore.restoreFacebookId(getActivity());
			final View container = (View) mPopUp.getContentView().findViewById(
					R.id.layoutFacebookMe);
			final String name = ((TextView) container
					.findViewById(R.id.txtFacebookName)).getText().toString();
			// we don't really need the picture at this point and restoring it
			// from the session store could be dependant on whether
			// FbUserInfoRequest is completed...
			FacebookUser me = new FacebookUser(idNum, name, null);
			addTag(me);
			break;
		}
	}

	/*
	 * {@inheritDoc}
	 * 
	 * This is called when an item in the friend's list is selected.
	 */
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {

		FacebookUser friend = (FacebookUser) parent.getItemAtPosition(position);
		addTag(friend);
	}

	/**
	 * Method that stores tag locations in memory and displays them in the
	 * layout over the photo.
	 * 
	 * @param user
	 */
	private void addTag(final FacebookUser user) {

		final Tag tag = new Tag();
		tag.user = user;
		// append the current location(in precent * 100 format for facebook)
		tag.xLocation = mImageView.getTagPositionX();
		tag.yLocation = mImageView.getTagPositionY();
		// add to our list of tags
		mTags.add(tag);

		// begin creating a view for indicating the tag on screen
		LayoutInflater inflater = (LayoutInflater) getActivity()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		final Button layout = (Button) inflater.inflate(R.layout.tag_label,
				mTagFrame, false);
		layout.setText(user.name);

		// take the user out of the friends list
		mFriendsAdapter.remove(user);
		mFriendsAdapter.notifyDataSetChanged();

		// set the layout to remove the tag and view on clicks
		layout.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				// add the user back into friends if they are not the logged in
				// user
				if (SessionStore.restoreFacebookId(getActivity()) != user.id) {
					mFriendsAdapter.add(user);
					mFriendsAdapter.notifyDataSetChanged();

					// reset the search by friends name filter and text
					final String searchText = ((EditText) mPopUp
							.getContentView().findViewById(
									R.id.editTextSearchFriends)).getText()
							.toString();
					mFriendsAdapter.getFilter().filter(searchText);
				} else {
					// show the view for allowing a user to tag themselves
					((View) mPopUp.getContentView().findViewById(
							R.id.layoutFacebookMe)).setVisibility(View.VISIBLE);
				}
				mTags.remove(tag);
				mTagFrame.removeView(layout);
			}

		});

		// set margins to place the view over the current tag cursor location
		// TODO: center and back off the right and bottom margins
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		lp.setMargins((int) mImageView.getTagCursorPositionX(),
				(int) mImageView.getTagCursorPositionY(), 0, 0);
		mTagFrame.addView(layout, lp);

		// reset the search by friends name filter and text
		((EditText) mPopUp.getContentView().findViewById(
				R.id.editTextSearchFriends)).setText("");
		Filter filter = mFriendsAdapter.getFilter();
		filter = null;

		mPopUp.dismiss();
	}

	/*
	 * {@inheritDoc}
	 * 
	 * The touch listener callback that creates and update the Friends pop-up
	 * when the tag location is moved.
	 */
	public boolean onTouch(View v, MotionEvent event) {

		if (mPopUp != null && mPopUp.isShowing())
			mPopUp.dismiss();

		// We will only react on up events.
		if (event.getAction() == android.view.MotionEvent.ACTION_UP) {

			if (mPopUp != null) {
				// Pop-up is set up we can just update the location.
				int[] location = new int[2];
				mImageView.getLocationInWindow(location);
				final int x = location[0]
						+ (int) mImageView.getTagCursorPositionX()
						+ mImageView.getTagCursorWidth() + 8;
				final int y = location[1]
						+ (int) mImageView.getTagCursorPositionY();
				mPopUp.showAtLocation(mImageView, Gravity.NO_GRAVITY, x, y);
				// allowTouchOutsideOfPopup();
			} else {
				// Need to create and display the pop-up.
				LayoutInflater inflater = (LayoutInflater) getActivity()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View layout = inflater.inflate(R.layout.tag_popup,
						(ViewGroup) mImageView.getParent(), false);

				final float SCALE = getResources().getDisplayMetrics().density;
				final int widthDip = (int) (316 * SCALE + 0.5f);
				final int heightDip = (int) (366 * SCALE + 0.5f);

				mPopUp = new PopupWindow(layout, widthDip, heightDip, true);

				mPopUp.setBackgroundDrawable(new BitmapDrawable(getActivity()
						.getResources()));
				mPopUp.setFocusable(true);
				mPopUp.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
				mPopUp.setAnimationStyle(R.style.Animations_GrowFromBottom);

				mPopUp.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
						| LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION);
				mPopUp.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
				mPopUp.setOnDismissListener(new OnDismissListener() {

					public void onDismiss() {
						mImageView.mTagActive = false;
						mImageView.invalidate();
					}

				});

				int[] location = new int[2];
				mImageView.getLocationInWindow(location);
				final int x = location[0]
						+ (int) mImageView.getTagCursorPositionX()
						+ mImageView.getTagCursorWidth() + 8;
				final int y = location[1]
						+ (int) mImageView.getTagCursorPositionY();
				mPopUp.showAtLocation(mImageView, Gravity.NO_GRAVITY, x, y);

				ListView list = (ListView) mPopUp.getContentView()
						.findViewById(R.id.listFacebookFriends);
				list.setAdapter(mFriendsAdapter);
				list.setOnItemClickListener(this);
				list.setEmptyView(mPopUp.getContentView().findViewById(
						R.id.listFacebookFriendsEmpty));
				EditText search = (EditText) mPopUp.getContentView()
						.findViewById(R.id.editTextSearchFriends);
				search.addTextChangedListener(new FilterTextWatcher());
				// this will make the popup non-modal, must be run after showing
				// popup
				// allowTouchOutsideOfPopup();
				setPopupUserDetails();
			}
		}
		// return false so we don't consume the touch event and continue passing
		// it
		return false;
	}

	/**
	 * This helper method will set a the mPopup window's FrameLayout container
	 * to keep from consuming touch events. This way we can continue to receive
	 * touch events on other views in the fragment. This could also be achieved
	 * by setting the PopupWindow to non-focusable, but then views in the popup
	 * that require focus(e.g. EditText) would be essentially disabled.
	 * 
	 * <b>Notice: <b> This method must only be called after the pop has been
	 * shown otherwise there will not be a FrameLayout created yet to set the
	 * appropriate layout parameters.
	 * 
	 * @return void
	 */
	private void allowTouchOutsideOfPopup() {
		FrameLayout popupContainer = (FrameLayout) mPopUp.getContentView()
				.getParent();
		WindowManager.LayoutParams p = (WindowManager.LayoutParams) popupContainer
				.getLayoutParams();
		p.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
		WindowManager windowManager = (WindowManager) getActivity()
				.getSystemService(Context.WINDOW_SERVICE);
		windowManager.updateViewLayout(popupContainer, p);
	}

	/**
	 * Updates the user's information in the approriate views.
	 */
	private void setPopupUserDetails() {
		final HashMap<String, String> user = SessionStore
				.restoreFacebookUser(getActivity());

		final String id = user.get(SessionStore.FB_ID);
		if (mPopUp != null && id != null) {
			final View container = (View) mPopUp.getContentView().findViewById(
					R.id.layoutFacebookMe);

			mFriendsAdapter.mImageWorker.loadImage(
					"https://graph.facebook.com/" + id + "/picture",
					(ImageView) container.findViewById(R.id.imgFacebookPic));
			((TextView) container.findViewById(R.id.txtFacebookName))
					.setText(user.get(SessionStore.FB_NAME));
			container.setOnClickListener(this);
			container.setBackgroundResource(R.drawable.list_selector_holo_dark);
		}
	}

	/**
	 * A class that watches for changes to the text on our name filter field.
	 */
	private class FilterTextWatcher implements TextWatcher {

		public void afterTextChanged(Editable s) {
			// no action
		}

		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			// no action
		}

		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			// Update the filter.
			mFriendsAdapter.getFilter().filter(s);
		}

	}

	/**
	 * Helper method to initiate a request for the user's Facebook information.
	 * 
	 * @param sendPhoto
	 */
	private void getFacebookUserInfo(boolean sendPhoto) {
		// TODO: add check for existing user info in stored prefs

		// mProgressDialog = ProgressDialog.show(this, null,
		// "Retrieving your Facebook details...", true, true);
		AsyncFacebookRunner runner = new AsyncFacebookRunner(
				mFacebookConnector.getFacebook());

		Bundle params = new Bundle();
		params.putString("fields", "name, picture, email");
		runner.request("me", params, new FbUserInfoRequestListener(sendPhoto));

	}

	/**
	 * Callback for a retrieving a facebook user's information(i.e. id, photo,
	 * email)
	 */
	private class FbUserInfoRequestListener implements RequestListener {

		private boolean mSendPhotoAfterCompleted = false;

		public FbUserInfoRequestListener(boolean sendPhotoAfterCompleted) {
			mSendPhotoAfterCompleted = sendPhotoAfterCompleted;
		}

		public void onComplete(final String response, final Object state) {

			if (BuildConfig.DEBUG) {
				Log.d(getClass().getName(), "onComplete: " + response);
			}

			if (getActivity() == null)
				return;

			getActivity().runOnUiThread(new Runnable() {
				public void run() {
					if (response != null) {
						try {
							SessionStore.saveFacebookUser(new JSONObject(
									response), getActivity());
							if (mSendPhotoAfterCompleted) {
								postPhoto(SessionStore
										.restoreFacebookId(getActivity()));
							} else {
								getFacebookFriends();
								setPopupUserDetails();
							}
						} catch (JSONException e) {
							Toast.makeText(getActivity(),
									"Unable to read response from Facebook.",
									Toast.LENGTH_LONG).show();
						}
					} else {

						Toast.makeText(
								getActivity(),
								"Unable to get user information from Facebook.",
								Toast.LENGTH_LONG).show();
						;
					}

				}
			});

		}

		public void onIOException(final IOException e, final Object state) {

			if (BuildConfig.DEBUG)
				Log.d(getClass().getName(), "onIOException: " + e);

			if (getActivity() == null)
				return;

			getActivity().runOnUiThread(new Runnable() {
				public void run() {
					if (BuildConfig.DEBUG) {
						Log.e(getClass().getName(), "onIOException: " + e);
					}
					Toast.makeText(getActivity(),
							"Unable to get user information from Facebook.",
							Toast.LENGTH_LONG).show();
					;
				}
			});

		}

		public void onFileNotFoundException(final FileNotFoundException e,
				final Object state) {

			if (BuildConfig.DEBUG)
				Log.d(getClass().getName(), "onFileNotFoundException: " + e);

			if (getActivity() == null)
				return;

			getActivity().runOnUiThread(new Runnable() {
				public void run() {
					if (BuildConfig.DEBUG) {
						Log.e(getClass().getName(), "onFileNotFoundException: "
								+ e);
					}
					Toast.makeText(getActivity(),
							"Unable to get user information from Facebook.",
							Toast.LENGTH_LONG).show();
					;
				}
			});

		}

		public void onMalformedURLException(final MalformedURLException e,
				final Object state) {

			if (BuildConfig.DEBUG)
				Log.d(getClass().getName(), "onMalformedURLException: " + e);

			if (getActivity() == null)
				return;

			getActivity().runOnUiThread(new Runnable() {
				public void run() {
					if (BuildConfig.DEBUG) {
						Log.e(getClass().getName(), "onMalformedURLException: "
								+ e);
					}

					Toast.makeText(getActivity(),
							"Unable to get user information from Facebook.",
							Toast.LENGTH_LONG).show();
					;
				}
			});

		}

		public void onFacebookError(final FacebookError e, final Object state) {

			if (BuildConfig.DEBUG)
				Log.d(getClass().getName(), "onFacebookError: " + e);

			if (getActivity() == null)
				return;

			getActivity().runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(getActivity(),
							"Unable to get user information from Facebook.",
							Toast.LENGTH_LONG).show();
					;
				}
			});

		}

	}

	/**
	 * Helper method to initiate a request from facebook to retrieve a user's
	 * friends.
	 */
	private void getFacebookFriends() {

		// mProgressDialog = ProgressDialog.show(this, null,
		// "Retrieving your Facebook details...", true, true);
		AsyncFacebookRunner runner = new AsyncFacebookRunner(
				mFacebookConnector.getFacebook());

		Bundle params = new Bundle();
		params.putString("fields", "id, name, picture");
		runner.request("me/friends", params, new FacebookFriendsListener());

	}

	/**
	 * Callback for the friends request. Defines our actions on failures and
	 * parse succesful responses.
	 */
	public class FacebookFriendsListener extends BaseRequestListener {

		public void onComplete(final String response, final Object state) {

			if (BuildConfig.DEBUG)
				Log.d(getClass().getName(), "onComplete: " + response);

			if (getActivity() == null)
				return;

			getActivity().runOnUiThread(new Runnable() {

				public void run() {

					if (mProgressDialog != null)
						mProgressDialog.dismiss();

					JSONObject json;

					try {
						json = Util.parseJson(response);

						if (BuildConfig.DEBUG) {
							Log.d(getClass().getName(),
									"Parsed Facebook friends response: "
											+ json.toString(5));
						}

						JSONArray data = json.getJSONArray("data");

						final String obfuscatedId = Utils
								.md5(mFacebookConnector.getFacebookToken());
						EasyTracker.getTracker().trackEvent("Facebook",
								"Friends Count", obfuscatedId, data.length());

						JSONObject friend;
						mFriendsAdapter.clear();

						for (int i = 0; i < data.length(); i++) {
							friend = data.getJSONObject(i);
							mFriendsAdapter.add(new FacebookUser(friend
									.getString("id"), friend.getString("name"),
									friend.getString("picture")));
						}

						mFriendsAdapter.notifyDataSetChanged();

					} catch (FacebookError e) {
						Toast.makeText(
								getActivity(),
								"Unable to retrieve facebook friends at this time. Facebook Error: "
										+ e.getMessage(), Toast.LENGTH_LONG)
								.show();
					} catch (JSONException e) {
						Toast.makeText(
								getActivity(),
								"Unable to retrieve facebook friends at this time. Error reading response from Facebook.",
								Toast.LENGTH_LONG).show();
					}

				}
			});
		}

		public void onFacebookError(final FacebookError error) {

			if (BuildConfig.DEBUG)
				Log.d(getClass().getName(), "onFacebookError: " + error);

			if (getActivity() == null)
				return;

			getActivity().runOnUiThread(new Runnable() {

				public void run() {

					if (mProgressDialog != null)
						mProgressDialog.dismiss();
					Toast.makeText(
							getActivity(),
							"Unable to retrieve facebook friends at this time. Facebook Error: "
									+ error.getMessage(), Toast.LENGTH_LONG)
							.show();
				}
			});

		}
	}

	/**
	 * Helper method to setup a request to post a photo to Facebook.
	 * 
	 * @param id
	 *            - The id number of a photo on Facebook.
	 */
	private void postPhoto(String id) {

		if (id != null) {
			mProgressDialog = ProgressDialog.show(getActivity(), null,
					"Posting photo to Facebook...", true, true);

			Bundle params = new Bundle();

			byte[] data = ImageResizer.getEncodedByteArrayFromFile(mImagePath,
					720, 720, CompressFormat.JPEG, 70);

			params.putByteArray("photo", data);
			// params.putString("caption", "photo upload from poggled");
			AsyncFacebookRunner runner = new AsyncFacebookRunner(
					mFacebookConnector.getFacebook());
			runner.request("me/photos", params, "POST",
					new PhotoUploadListener(), null);

			final FileLoggerMessage msg = new FileLoggerMessage(
					FileLoggerMessage.FACEBOOK_POST_IMAGE,
					Utils.getWifiSignalStrength(getActivity()));

			new Thread() {
				@Override
				public void run() {
					PhotoTaggerApplication.getFileLogger().info(msg.toString());
				}
			}.start();

		} else {
			Toast.makeText(
					getActivity(),
					"Unable to upload your photo at this time. Are you logged in?",
					Toast.LENGTH_LONG).show();
		}

	}

	/**
	 * Callback for the photo upload. Defines our actions on success and
	 * failure.
	 */
	public class PhotoUploadListener extends BaseRequestListener {
		/*
		 * {@inheritDoc}
		 */

		public void onComplete(final String response, final Object state) {

			if (BuildConfig.DEBUG)
				Log.d(getClass().getName(), "onComplete: " + response);

			JSONObject json;

			try {
				json = Util.parseJson(response);
				final String photo_id = json.getString("id");

				final String obfuscatedId = Utils.md5(mFacebookConnector
						.getFacebookToken());
				EasyTracker.getTracker().trackEvent("Facebook", "Photo Upload",
						obfuscatedId, 0);

				final FileLoggerMessage msg = new FileLoggerMessage(
						FileLoggerMessage.FACEBOOK_POST_IMAGE_SUCCESS,
						Utils.getWifiSignalStrength(getActivity()));
				PhotoTaggerApplication.getFileLogger().info(msg.toString());

				if (getActivity() == null)
					return;

				getActivity().runOnUiThread(new Runnable() {

					public void run() {

						if (mProgressDialog != null)
							mProgressDialog.dismiss();

						postTag(photo_id);

					}
				});
			} catch (FacebookError e) {
				onFacebookError(e, state);
			} catch (JSONException e) {

				final FileLoggerMessage msg = new FileLoggerMessage(
						FileLoggerMessage.FACEBOOK_POST_IMAGE_ERROR,
						Utils.getWifiSignalStrength(getActivity()));
				msg.addMessage(e.getMessage());
				PhotoTaggerApplication.getFileLogger().info(msg.toString());

				if (getActivity() == null)
					return;
				getActivity().runOnUiThread(new Runnable() {

					public void run() {

						if (mProgressDialog != null)
							mProgressDialog.dismiss();

						Toast.makeText(
								getActivity(),
								"Unable to upload your photo at this time. Error reading response from Facebook.",
								Toast.LENGTH_LONG).show();

					}
				});
			}

		}

		/*
		 * {@inheritDoc}
		 */

		public void onFacebookError(final FacebookError error, Object state) {

			if (BuildConfig.DEBUG)
				Log.d(getClass().getName(), "onFacebookError: " + error);

			if (getActivity() == null)
				return;

			getActivity().runOnUiThread(new Runnable() {

				public void run() {

					if (mProgressDialog != null)
						mProgressDialog.dismiss();
					Toast.makeText(
							getActivity(),
							"Unable to upload your photo at this time. Facebook Error: "
									+ error.getMessage(), Toast.LENGTH_LONG)
							.show();
				}
			});

			final FileLoggerMessage msg = new FileLoggerMessage(
					FileLoggerMessage.FACEBOOK_POST_IMAGE_ERROR,
					Utils.getWifiSignalStrength(getActivity()));
			msg.addMessage(error.getMessage());
			PhotoTaggerApplication.getFileLogger().info(msg.toString());

		}

		/*
		 * {@inheritDoc}
		 */
		@Override
		public void onIOException(final IOException e, Object state) {

			if (BuildConfig.DEBUG)
				Log.d(getClass().getName(), "onIOException: " + e);

			if (getActivity() == null)
				return;

			getActivity().runOnUiThread(new Runnable() {

				public void run() {

					if (mProgressDialog != null)
						mProgressDialog.dismiss();
					Toast.makeText(
							getActivity(),
							"Unable to upload your photo at this time. Connection error: "
									+ e.getMessage(), Toast.LENGTH_LONG).show();
				}
			});

			final String type = (e instanceof ConnectTimeoutException) ? FileLoggerMessage.FACEBOOK_POST_IMAGE_TIMEOUT
					: FileLoggerMessage.FACEBOOK_POST_IMAGE_ERROR;
			final FileLoggerMessage msg = new FileLoggerMessage(type,
					Utils.getWifiSignalStrength(getActivity()));
			msg.addMessage(e.getMessage());
			PhotoTaggerApplication.getFileLogger().info(msg.toString());
		}

		/*
		 * {@inheritDoc}
		 */
		@Override
		public void onMalformedURLException(final MalformedURLException e,
				Object state) {

			if (BuildConfig.DEBUG)
				Log.d(getClass().getName(), "onMalformedURLException: " + e);

			if (getActivity() == null)
				return;

			getActivity().runOnUiThread(new Runnable() {

				public void run() {

					if (mProgressDialog != null)
						mProgressDialog.dismiss();
					Toast.makeText(
							getActivity(),
							"Unable to upload your photo at this time. Connection error: "
									+ e.getMessage(), Toast.LENGTH_LONG).show();
				}
			});

			final FileLoggerMessage msg = new FileLoggerMessage(
					FileLoggerMessage.FACEBOOK_POST_IMAGE_ERROR,
					Utils.getWifiSignalStrength(getActivity()));
			msg.addMessage(e.getMessage());
			PhotoTaggerApplication.getFileLogger().info(msg.toString());
		}

	}

	/**
	 * Helper method to kick-off a request to tag a photo.
	 * 
	 * @param photoID
	 *            - The Facebook-assigned id number for the photo.
	 */
	public void postTag(String photoID) {

		if (photoID == null)
			return;

		// Build the graph request in the form photoID/tags/
		StringBuilder sb = new StringBuilder();
		sb.append(photoID);
		sb.append("/tags/");

		JSONArray tags = new JSONArray();

		// tags.put(getJSONTag(SessionStore.restoreFacebookId(getActivity()),
		// mImageView.getTagPositionX(), mImageView.getTagPositionY()));
		try {
			// Poggled tag should be in the upper right for frames and lower
			// right for watermarks
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(getActivity());
			final boolean useFrames = prefs.getBoolean(
					getString(R.string.pref_frame_enabled), false);
			if (useFrames) {
				tags.put(buildJSONTag("121083561691", 85, 0));
			} else {
				tags.put(buildJSONTag("121083561691", 95, 95));
			}
			for (Tag tag : mTags) {
				tags.put(buildJSONTag(tag.user.id, tag.xLocation, tag.yLocation));
			}
		} catch (JSONException e) {

			if (mProgressDialog != null)
				mProgressDialog.dismiss();
			Toast.makeText(
					getActivity(),
					"Unable to upload your photo at this time. Error reading response from Facebook.",
					Toast.LENGTH_LONG).show();

			final FileLoggerMessage msg = new FileLoggerMessage(
					FileLoggerMessage.FACEBOOK_TAG_IMAGE_ERROR,
					Utils.getWifiSignalStrength(getActivity()));
			msg.addMessage(e.getMessage());

			new Thread() {
				@Override
				public void run() {
					PhotoTaggerApplication.getFileLogger().info(msg.toString());
				}
			}.start();
			return;
		}

		if (tags.length() < 1) {
			return;
		}

		if (BuildConfig.DEBUG)
			Log.d(getClass().getName(),
					"JSON tags for Facebook: " + tags.toString());

		mProgressDialog = ProgressDialog.show(getActivity(), null,
				"Tagging your image...", true, true);

		Bundle params = new Bundle();
		params.putString("tags", tags.toString());

		AsyncFacebookRunner runner = new AsyncFacebookRunner(
				mFacebookConnector.getFacebook());
		runner.request(sb.toString(), params, "POST",
				new TagPhotoRequestListener(tags.length()), null);

		final FileLoggerMessage msg = new FileLoggerMessage(
				FileLoggerMessage.FACEBOOK_TAG_IMAGE,
				Utils.getWifiSignalStrength(getActivity()));

		new Thread() {
			@Override
			public void run() {
				PhotoTaggerApplication.getFileLogger().info(msg.toString());
			}
		}.start();
	}

	/**
	 * Helper method that converts values into a formatted JSONObject that the
	 * Facebook api can handle.
	 * 
	 * @param id
	 *            - The id of the facebook user to be tagged.
	 * @param xLoc
	 *            - The horizontal location of the tag in percent * 100 format
	 *            for facebook.
	 * @param yLoc
	 *            - The vertical location of the tag in percent * 100 format for
	 *            facebook.
	 * @return - A JSONObject string with the parameters correctly formatted.
	 * @throws JSONException
	 */
	private JSONObject buildJSONTag(String id, int xLoc, int yLoc)
			throws JSONException {

		JSONObject object = new JSONObject();

		// The reference at
		// http://developers.facebook.com/docs/reference/api/photo/ claims the
		// id param should be id, but only tag_uid seems to work
		object.put("tag_uid", id);
		object.put("x", xLoc);
		object.put("y", yLoc);

		return object;
	}

	/**
	 * The callback for handling a tag photo request.
	 */
	public class TagPhotoRequestListener extends BaseRequestListener {

		private int mTagCount = 0;

		/**
		 * Constructor with the tag count specified. This should be defined at
		 * creation so that we can log the number of tags that were successfully
		 * tagged.
		 * 
		 * @param tagCount
		 */
		public TagPhotoRequestListener(int tagCount) {
			super();
			mTagCount = tagCount;
		}

		/*
		 * {@inheritDoc}
		 */

		public void onComplete(final String response, final Object state) {

			if (BuildConfig.DEBUG)
				Log.d(getClass().getName(), "onComplete: " + response);

			if (response.equals("true")) {

				final String obfuscatedId = Utils.md5(mFacebookConnector
						.getFacebookToken());
				EasyTracker.getTracker().trackEvent("Facebook", "Tag Photo",
						obfuscatedId, mTagCount);

				final FileLoggerMessage msg = new FileLoggerMessage(
						FileLoggerMessage.FACEBOOK_TAG_IMAGE_SUCCESS,
						Utils.getWifiSignalStrength(getActivity()));
				PhotoTaggerApplication.getFileLogger().info(msg.toString());
			} else {
				final FileLoggerMessage msg = new FileLoggerMessage(
						FileLoggerMessage.FACEBOOK_TAG_IMAGE_ERROR,
						Utils.getWifiSignalStrength(getActivity()));
				msg.addMessage(response);
				PhotoTaggerApplication.getFileLogger().info(msg.toString());
			}

			if (getActivity() == null)
				return;

			getActivity().runOnUiThread(new Runnable() {

				public void run() {

					if (mProgressDialog != null)
						mProgressDialog.dismiss();
					if (response.equals("true")) {

						AlertDialog alertDialog = new AlertDialog.Builder(
								getActivity()).create();
						alertDialog.setTitle("Success");
						alertDialog.setMessage("Photo uploaded to Facebook.");
						alertDialog.setButton("OK",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
										getActivity().finish();
										dialog.dismiss();
									}
								});
						alertDialog.setIcon(R.drawable.file_icon);
						alertDialog.show();
					} else {
						Toast.makeText(
								getActivity(),
								"Unable to tag your photo at this time, but your photo has been uploaded.",
								Toast.LENGTH_LONG).show();
					}
				}
			});

		}

		/*
		 * {@inheritDoc}
		 */
		@Override
		public void onFacebookError(final FacebookError error, Object state) {

			if (BuildConfig.DEBUG)
				Log.d(getClass().getName(), "onFacebookError: " + error);

			if (getActivity() == null)
				return;

			getActivity().runOnUiThread(new Runnable() {

				public void run() {

					if (mProgressDialog != null)
						mProgressDialog.dismiss();
					Toast.makeText(
							getActivity(),
							"Unable to tag your photo at this time, but your photo has been uploaded.",
							Toast.LENGTH_LONG).show();
				}
			});

			final FileLoggerMessage msg = new FileLoggerMessage(
					FileLoggerMessage.FACEBOOK_TAG_IMAGE_ERROR,
					Utils.getWifiSignalStrength(getActivity()));
			msg.addMessage(error.getMessage());
			PhotoTaggerApplication.getFileLogger().info(msg.toString());
		}

		/*
		 * {@inheritDoc}
		 */

		public void onIOException(final IOException e, Object state) {

			if (BuildConfig.DEBUG)
				Log.d(getClass().getName(), "onIOException: " + e);

			getActivity().runOnUiThread(new Runnable() {

				public void run() {

					if (mProgressDialog != null)
						mProgressDialog.dismiss();
					Toast.makeText(
							getActivity(),
							"Unable to tag your photo at this time, but your photo has been uploaded. Connection error: "
									+ e.getMessage(), Toast.LENGTH_LONG).show();
				}
			});

			final String type = (e instanceof ConnectTimeoutException) ? FileLoggerMessage.FACEBOOK_TAG_IMAGE_TIMEOUT
					: FileLoggerMessage.FACEBOOK_TAG_IMAGE_ERROR;
			final FileLoggerMessage msg = new FileLoggerMessage(type,
					Utils.getWifiSignalStrength(getActivity()));
			msg.addMessage(e.getMessage());
			PhotoTaggerApplication.getFileLogger().info(msg.toString());
		}

		/*
		 * {@inheritDoc}
		 */
		@Override
		public void onMalformedURLException(final MalformedURLException e,
				Object state) {

			if (BuildConfig.DEBUG)
				Log.d(getClass().getName(), "onMalformedURLException: " + e);

			if (getActivity() == null)
				return;

			getActivity().runOnUiThread(new Runnable() {

				public void run() {

					if (mProgressDialog != null)
						mProgressDialog.dismiss();
					Toast.makeText(
							getActivity(),
							"Unable to tag your photo at this time, but your photo has been uploaded. Connection error: "
									+ e.getMessage(), Toast.LENGTH_LONG).show();
				}
			});

			final FileLoggerMessage msg = new FileLoggerMessage(
					FileLoggerMessage.FACEBOOK_TAG_IMAGE_ERROR,
					Utils.getWifiSignalStrength(getActivity()));
			msg.addMessage(e.getMessage());
			PhotoTaggerApplication.getFileLogger().info(msg.toString());
		}
	}

	/**
	 * Simple POJO object for our tags
	 */
	public static class Tag {
		public FacebookUser user;
		public int xLocation;
		public int yLocation;

		/**
		 * Default constructor no values defined.
		 */
		public Tag() {
			// default values
		}

		/**
		 * Constructor with all fields defined.
		 * 
		 * @param user
		 * @param x
		 * @param y
		 */
		public Tag(FacebookUser user, int x, int y) {
			this.user = user;
			this.xLocation = x;
			this.yLocation = y;
		}
	}

	/**
	 * Simple POJO object for our Facebook friends that we retrieve from the
	 * Facebook api.
	 */
	public static class FacebookUser {
		public String id;
		public String name;
		public String picture;

		/**
		 * Basic constructor with all fields declared
		 * 
		 * @param id
		 * @param name
		 * @param picture
		 */
		public FacebookUser(String id, String name, String picture) {
			this.id = id;
			this.name = name;
			this.picture = picture;
		}

		/*
		 * {@inheritDoc}
		 */
		public String toString() {
			return this.name;
		}
	}

	/**
	 * An adapter that
	 */
	private static class FriendsArrayAdapter extends ArrayAdapter<FacebookUser> {

		private static final String IMAGE_CACHE_DIR = "thumbs/facebook";

		private ImageFetcher mImageWorker;

		/**
		 * Constructor. Calls super constructor and additionally sets up our
		 * ImageWorker for caching.
		 * 
		 * @param context
		 * @param textViewResourceId
		 */
		public FriendsArrayAdapter(Context context, int textViewResourceId) {

			super(context, textViewResourceId);
			ImageCacheParams cacheParams = new ImageCacheParams(IMAGE_CACHE_DIR);

			cacheParams.memCacheSize = 1024 * 1024 * 5; // 5MB

			// The ImageWorker takes care of loading images into our ImageView
			// children asynchronously
			mImageWorker = new ImageFetcher(context, 50, 50);
			mImageWorker.setImageCache(new ImageCache(context, cacheParams));
			mImageWorker.setLoadingImage(R.drawable.facebook_default);
			mImageWorker.setImageFadeIn(false);
		}

		/*
		 * {@inheritDoc}
		 * 
		 * This method is implemented to recycle views with convertView and also
		 * speed up view traversal with a ViewHolder.
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			ViewHolder holder;
			if (view == null) {
				holder = new ViewHolder();
				LayoutInflater vi = (LayoutInflater) parent.getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = vi.inflate(R.layout.friends_list_item, parent, false);
				holder.txtName = (TextView) view
						.findViewById(R.id.txtFacebookName);
				holder.txtCount = (TextView) view
						.findViewById(R.id.txtFacebookTagCount);
				holder.image = (ImageView) view
						.findViewById(R.id.imgFacebookPic);
				view.setTag(holder);
			} else {
				holder = (ViewHolder) view.getTag();
			}

			FacebookUser friend = null;
			if (getCount() > 0)
				friend = (FacebookUser) getItem(position);

			if (friend != null) {
				holder.txtName.setText(friend.name);

				mImageWorker.loadImage("https://graph.facebook.com/"
						+ friend.id + "/picture", holder.image);

			}
			return view;
		}

		/**
		 * Holds on to references for our views so that the entire view doesn't
		 * have to be walked each time we reuse it.
		 */
		private class ViewHolder {

			public TextView txtName;
			public TextView txtCount;
			public ImageView image;
		}

	}

}
