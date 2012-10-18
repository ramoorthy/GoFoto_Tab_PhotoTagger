package com.poggled.android.phototagger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.google.android.apps.analytics.easytracking.EasyTracker;
import com.poggled.android.phototagger.service.ImageProcessingService;
import com.poggled.android.phototagger.util.FacebookConnector;
import com.poggled.android.phototagger.util.SessionStore;

public class PhotoTaggerApplication extends Application {

	// debug facebook key app name: Poggled_DEBUG
	// private static final String FACEBOOK_ID = "226657470708450";

	// debug facebook key app name: bars with friends
	// rivate static final String FACEBOOK_ID = "211959445584534";

	// production poggled facebook key
	public static final String FACEBOOK_ID = "144050912294582";

	// facebook app access token: 226657470708450|b0E3czaGJnyq2Sj__3v2r9juhjo
	// user token:
	// AAADOJM4GZBuIBAILWUN09ZB1BGLuAvsC47N3Fu45jZBWXMeZAQGEucTFJKZAJ7YwyaZCWszlebQRsIFJUSC2q7CXZCZAZAxJxFBNSeK5wKE3mowZDZD
	private static final String[] FACEBOOK_PERMISSIONS = { "publish_stream",
			"email", "user_birthday", "user_photos" };

	private static FacebookConnector mFacebookConnector;

	private static String sID = null;
	private static final String INSTALLATION_FILE = "INSTALLATION";

	private static Logger sLogger;

	@Override
	public void onCreate() {
		super.onCreate();

		mFacebookConnector = new FacebookConnector(FACEBOOK_ID,
				FACEBOOK_PERMISSIONS, getApplicationContext());

		Intent intent = new Intent(this, ImageProcessingService.class);
		intent.setAction(Intent.ACTION_SYNC);
		startService(intent);

		sLogger = LoggerFactory.getLogger(PhotoTaggerApplication.class);

		EasyTracker.getTracker().setContext(getApplicationContext());
		// Track custom variable for this install id (slot:1
		// scope:visitor-level)
		EasyTracker.getTracker().setCustomVar(1, "Installation Id",
				id(getApplicationContext()), 1);
	}

	public synchronized static void logout(Context context) {

		SessionStore.clearFacebookUser(context);
		mFacebookConnector.logout();
		SessionStore.clearFacebookSession(context);
	}

	/**
	 * @return the facebookConnector
	 */
	public static String getFacebookId() {
		return FACEBOOK_ID;
	}

	/**
	 * @return the facebookConnector
	 */
	public static FacebookConnector getFacebookConnector() {
		return mFacebookConnector;
	}

	public synchronized static String id(Context context) {
		if (sID == null) {
			File installation = new File(context.getFilesDir(),
					INSTALLATION_FILE);
			try {
				if (!installation.exists())
					writeInstallationFile(installation);
				sID = readInstallationFile(installation);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return sID;
	}

	private static String readInstallationFile(File installation)
			throws IOException {
		RandomAccessFile f = new RandomAccessFile(installation, "r");
		byte[] bytes = new byte[(int) f.length()];
		f.readFully(bytes);
		f.close();
		return new String(bytes);
	}

	private static void writeInstallationFile(File installation)
			throws IOException {
		FileOutputStream out = new FileOutputStream(installation);
		String id = UUID.randomUUID().toString();
		out.write(id.getBytes());
		out.close();
	}

	public static Logger getFileLogger() {
		return sLogger;
	}
}
