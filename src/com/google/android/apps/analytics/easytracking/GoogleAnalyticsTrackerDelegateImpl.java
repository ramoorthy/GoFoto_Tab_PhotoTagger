// Copyright 2011 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.android.apps.analytics.easytracking;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.google.android.apps.analytics.Item;
import com.google.android.apps.analytics.Transaction;

import android.content.Context;

public class GoogleAnalyticsTrackerDelegateImpl implements
		GoogleAnalyticsTrackerDelegate {

	private GoogleAnalyticsTracker tracker = GoogleAnalyticsTracker
			.getInstance();

	public void startNewSession(String accountId, int dispatchPeriod,
			Context ctx) {
		tracker.startNewSession(accountId, dispatchPeriod, ctx);
	}

	public void trackEvent(String category, String action, String label,
			int value) {
		tracker.trackEvent(category, action, label, value);
	}

	public void trackPageView(String pageUrl) {
		tracker.trackPageView(pageUrl);
	}

	public boolean dispatch() {
		return tracker.dispatch();
	}

	public void stopSession() {
		tracker.stopSession();
	}

	public boolean setCustomVar(int index, String name, String value, int scope) {
		return tracker.setCustomVar(index, name, value, scope);
	}

	public boolean setCustomVar(int index, String name, String value) {
		return tracker.setCustomVar(index, name, value);
	}

	public void addTransaction(Transaction transaction) {
		tracker.addTransaction(transaction);
	}

	public void addItem(Item item) {
		tracker.addItem(item);
	}

	public void trackTransactions() {
		tracker.trackTransactions();
	}

	public void clearTransactions() {
		tracker.clearTransactions();
	}

	public void setAnonymizeIp(boolean anonymizeIp) {
		tracker.setAnonymizeIp(anonymizeIp);
	}

	public void setSampleRate(int sampleRate) {
		tracker.setSampleRate(sampleRate);
	}

	public boolean setReferrer(String referrer) {
		return tracker.setReferrer(referrer);
	}

	public void setDebug(boolean debug) {
		tracker.setDebug(debug);
	}

	public void setDryRun(boolean dryRun) {
		tracker.setDryRun(dryRun);
	}
}
