package com.poggled.android.phototagger.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import com.facebook.android.Facebook;
import com.poggled.android.phototagger.BuildConfig;

import java.util.HashMap;

public class SessionStore {
    
    private static final String TAG = "Session Store";
    private static final String FACEBOOK_TOKEN = "facebook_access_token";
    private static final String FACEBOOK_EXPIRES = "facebook_expires_in";
    private static final String FACEBOOK_FILE = "facebook-session";
    
    private static final String FACEBOOK_USER_FILE = "facebook-user";
    public static final String FB_ID = "fb_id";
    public static final String FB_NAME = "fb_name";
    public static final String FB_PIC = "fb_pic";
    public static final String FB_EMAIL = "fb_email";
    
    
    private static final String APP_FILE = "phototagger";
    private static final String WATERMARK = "watermark";
    
    
    public static boolean saveFacebookSession(Facebook session, Context context) {
        Editor editor =
            context.getSharedPreferences(FACEBOOK_FILE, Context.MODE_PRIVATE).edit();
        editor.putString(FACEBOOK_TOKEN, session.getAccessToken());
        editor.putLong(FACEBOOK_EXPIRES, session.getAccessExpires());
        
        if(BuildConfig.DEBUG)
            Log.d(TAG, "Saving Session: FACEBOOK_TOKEN = " 
        		+ session.getAccessToken() + " FACEBOOK_EXPIRES = " + session.getAccessExpires());
        
        return editor.commit();
    }

    public static boolean restoreFacebookSession(Facebook session, Context context) {
        SharedPreferences savedSession =
            context.getSharedPreferences(FACEBOOK_FILE, Context.MODE_PRIVATE);
        session.setAccessToken(savedSession.getString(FACEBOOK_TOKEN, null));
        session.setAccessExpires(savedSession.getLong(FACEBOOK_EXPIRES, -1));
        //session.setAccessExpires(savedSession.getLong(EXPIRES, 0));
        
        if(BuildConfig.DEBUG)
            Log.d(TAG, "Restoring Session: FACEBOOK_TOKEN = " + 
        		savedSession.getString(FACEBOOK_TOKEN, null) + " FACEBOOK_EXPIRES = " 
        			+ savedSession.getLong(FACEBOOK_EXPIRES, -1));
        //if(!session.isSessionValid()) clearFacebookSession(context);
        return session.isSessionValid();
    }

    public static void clearFacebookSession(Context context) {
        Editor editor = 
            context.getSharedPreferences(FACEBOOK_FILE, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.commit();
        if(BuildConfig.DEBUG)
            Log.d(TAG, "Clearing Facebook Token");
    }
    
    
    public static void saveFacebookUser(JSONObject json, Context context) throws JSONException {
        Editor editor = context.getSharedPreferences(FACEBOOK_USER_FILE, Context.MODE_PRIVATE).edit();
        
        final String id = json.getString("id");
        final String name = json.optString("name");
        final String pic = json.optString("picture");
        final String email = json.optString("email");
        
        editor.putString(FB_ID, id);
        editor.putString(FB_NAME, name);
        editor.putString(FB_PIC, pic);
        editor.putString(FB_EMAIL, email);
        
        editor.commit();
        
    }
    
    public static HashMap<String, String> restoreFacebookUser(Context context) {
        SharedPreferences settings =
            context.getSharedPreferences(FACEBOOK_USER_FILE, Context.MODE_PRIVATE);
        
        HashMap<String, String> user = new HashMap<String, String>();
        user.put(FB_ID, settings.getString(FB_ID, null));
        user.put(FB_NAME, settings.getString(FB_NAME, null));
        user.put(FB_PIC, settings.getString(FB_PIC, null));
        user.put(FB_EMAIL, settings.getString(FB_EMAIL, null));
        return user;
    }
    
    public static void clearFacebookUser(Context context) {
        Editor editor = 
            context.getSharedPreferences(FACEBOOK_USER_FILE, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.commit();
        if(BuildConfig.DEBUG)
            Log.d(TAG, "Clearing Facebook User");
    }
    
    public static String restoreFacebookId(Context context) {
        
        SharedPreferences settings = context.getSharedPreferences(FACEBOOK_USER_FILE, 0);
        String returnValue = settings.getString(FB_ID, null);
        return returnValue;
    }
    
    public static void saveWatermark(String watermark, Context context) {
        Editor editor = context.getSharedPreferences(APP_FILE, Context.MODE_PRIVATE).edit();
        editor.putString(WATERMARK, watermark);
        editor.commit();
    }
    
    public static String restoreWatermark(Context context) {
        
        SharedPreferences settings = context.getSharedPreferences(APP_FILE, 0);
        String returnValue = settings.getString(WATERMARK, null);
        return returnValue;
    }
    
}
