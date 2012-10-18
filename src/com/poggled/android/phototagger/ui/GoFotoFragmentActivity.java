/**
 * PhotoTagger
 * GoFotoFragmentActivity.java
 * 
 * @author Jason Harris on July 27, 2012
 * @author Jay Aniceto on September 24, 2012
 * @copyright 2012 Poggled, Inc. All rights reserved
 * 
 * Typical Usage: As a parent class for FragmentActivities in the application. 
 * 
 */
package com.poggled.android.phototagger.ui;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;

import com.google.android.apps.analytics.easytracking.EasyTracker;
import com.poggled.android.phototagger.R;
import com.poggled.android.phototagger.ui.email.EmailActivity;
import com.poggled.android.phototagger.ui.survey.SurveyActivity;
//import com.google.android.apps.analytics.easytracking.*;

/**
 * This class is a parent class for that Fragment Activities in the the
 * application that provides automated Google Analytics tracking through
 * Google's EasyTracker implementation. The class is largely based on
 * TrackedActivity from EasyTracker:
 * 
 * @see http
 *      ://code.google.com/p/analytics-api-samples/source/browse/tags/EasyTracker
 *      -release-1.0/android/EasyTracker/Library/src/com/google/android/apps/
 *      analytics/easytracking/TrackedActivity.java
 * 
 */
public class GoFotoFragmentActivity extends FragmentActivity implements
		ActionBar.TabListener {
	private View mActionBarView;
	private Integer the_active_tab;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Only one call to setContext is needed, but additional calls don't
		// hurt
		// anything, so we'll always make the call to ensure EasyTracker gets
		// setup properly.
		EasyTracker.getTracker().setContext(getApplicationContext());

	}

	@Override
	protected void onStart() {
		super.onStart();

		// This call will ensure that the Activity in question is tracked
		// properly,
		// based on the setting of ga_auto_activity_tracking parameter. It will
		// also ensure that startNewSession is called appropriately.
		EasyTracker.getTracker().trackActivityStart(this);
	}

	/**
	 * This method was onRetainNonConfigurationInstance prior to Android 3.0
	 * (Honeycomb), but EZ Tracker for GoogleAnalytics support contains only
	 * support for the older method and not the newer Fragment API. This should
	 * give us the same assurances that config changes will not result in new
	 * sessions.
	 */
	@Override
	public Object onRetainCustomNonConfigurationInstance() {
		// TODO: Test this to ensure new sessions are not being created on
		// rotation.
		Object o = super.onRetainCustomNonConfigurationInstance();

		// This call is needed to ensure that configuration changes (like
		// orientation) don't result in new sessions. Remove this line if you
		// want
		// configuration changes to for a new session in Google Analytics.
		EasyTracker.getTracker().trackActivityRetainNonConfigurationInstance();
		return o;
	}

	@Override
	protected void onStop() {
		super.onStop();

		// This call is needed to ensure time spent in an Activity and an
		// Application are measured accurately.
		EasyTracker.getTracker().trackActivityStop(this);
	}

	protected void setActiveTab(Integer active_tab) {
		the_active_tab = active_tab;
	}

	// --- START ACTION BAR ---//

	protected void createBar() {
		ActionBar bar = getActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		bar.setDisplayShowTitleEnabled(false);

		Tab goFotoTab = bar.newTab().setText(R.string.tab_photos);
		goFotoTab.setTabListener(this);

		Tab emailsTab = bar.newTab().setText(R.string.tab_emails);
		emailsTab.setTabListener(this);

		Tab surveyTab = bar.newTab().setText(R.string.tab_survey);
		surveyTab.setTabListener(this);

		bar.addTab(goFotoTab, 0, (the_active_tab == 0));
		bar.addTab(emailsTab, 1, (the_active_tab == 1));
		bar.addTab(surveyTab, 2, (the_active_tab == 2));

		mActionBarView = getLayoutInflater().inflate(
				R.layout.action_bar_custom, null);
		bar.setCustomView(mActionBarView);
		bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM
				| ActionBar.DISPLAY_USE_LOGO);
		bar.setDisplayShowHomeEnabled(true);
	}

	public void onTabReselected(Tab tab, android.app.FragmentTransaction ft) {
	}

	public void onTabSelected(Tab tab, android.app.FragmentTransaction ft) {
		Log.d(getClass().getSimpleName(), Integer.toString(tab.getPosition()));
		Log.d(getClass().getSimpleName(), Integer.toString(the_active_tab));
		if (tab.getPosition() != the_active_tab) {
			switch (tab.getPosition()) {
			case 0:
				Intent i = new Intent(this, HomeGridActivity.class);
				startActivity(i);
				break;
			case 1:
				i = new Intent(this, EmailActivity.class);
				startActivity(i);
				break;
			case 2:
				i = new Intent(this, SurveyActivity.class);
				startActivity(i);
				break;
			}
		}
		ft.commit();
	}

	public void onTabUnselected(Tab tab, android.app.FragmentTransaction ft) {
	}

	// --- END ACTION BAR ---//
}
