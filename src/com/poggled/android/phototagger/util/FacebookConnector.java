package com.poggled.android.phototagger.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebViewClient;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;
import com.facebook.android.FbDialog;
import com.facebook.android.Util;
import com.google.android.apps.analytics.easytracking.EasyTracker;
import com.poggled.android.phototagger.BuildConfig;
import com.poggled.android.phototagger.PhotoTaggerApplication;
import com.poggled.android.phototagger.ui.widget.AddAccountFbDialog;
import com.poggled.android.phototagger.util.SessionEvents.AuthListener;
import com.poggled.android.phototagger.util.SessionEvents.LogoutListener;

import java.util.ArrayList;

public class FacebookConnector {

    private static final String LOGIN = "oauth";
    private static final String TOKEN = "access_token";
    private static final String EXPIRES = "expires_in";
    public static final String REDIRECT_URI = "fbconnect://success";
    protected static String DIALOG_BASE_URL =
            "https://m.facebook.com/dialog/";
    
	private Facebook mFacebook = null;
	private Context mContext;
	private String[] mPermissions;
	private Handler mHandler;
	
	private SessionListener mSessionListener = new SessionListener();
	
	public FacebookConnector(String appId, String[] permissions, Context context) {
		this.mFacebook = new Facebook(appId);
		
		SessionStore.restoreFacebookSession(mFacebook, context);
        SessionEvents.addAuthListener(mSessionListener);
        SessionEvents.addLogoutListener(mSessionListener);
        
		this.mContext = context;
		this.mPermissions = permissions;
		this.mHandler = new Handler();
	}
	
	public void login(Activity activity) {
        if (!mFacebook.isSessionValid()) {
        	mFacebook.authorize(activity, this.mPermissions, new LoginDialogListener());
        	//startCustomDialogAuth(activity, this.mPermissions);
        }
    }
	
	public void logout() {
        SessionEvents.onLogoutBegin();
        AsyncFacebookRunner asyncRunner = new AsyncFacebookRunner(mFacebook);
        asyncRunner.logout(this.mContext, new LogoutRequestListener());
	}
	
	public Dialog getCustomDialogAuth(Activity activity) {
	    return getCustomDialogAuth(activity, new LoginDialogListener());
	}
    
    public Dialog getCustomDialogAuth(Activity activity, final DialogListener listener) {
        // Logout the user if the token has expired
        if (!mFacebook.isSessionValid()) {
            PhotoTaggerApplication.logout(activity);
        }
        
        // locally log this action off the UI thread
        final FileLoggerMessage msg = new FileLoggerMessage(FileLoggerMessage.FACEBOOK_DIALOG, Utils.getWifiSignalStrength(activity));
        
        new Thread() {
            @Override 
            public void run() {
                PhotoTaggerApplication.getFileLogger().info(msg.toString());
            }
        }.start();
        
        Bundle params = new Bundle();
        if (mPermissions.length > 0) {
            params.putString("scope", TextUtils.join(",", mPermissions));
        }
        CookieSyncManager.createInstance(activity);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
        //dialog(activity, LOGIN, params, new LoginDialogListener());
        String endpoint = DIALOG_BASE_URL + LOGIN;
        params.putString("display", "touch");
        params.putString("redirect_uri", REDIRECT_URI);
        params.putString("type", "user_agent");
        params.putString("client_id", mFacebook.getAppId());
        if (mFacebook.isSessionValid()) {
            params.putString(TOKEN, mFacebook.getAccessToken());
        }
        String url = endpoint + "?" + Util.encodeUrl(params);
        FbDialog dialog = new AddAccountFbDialog(activity, url, new DialogListener() {

            public void onComplete(Bundle values) {
                // ensure any cookies set by the dialog are saved
                CookieSyncManager.getInstance().sync();
                mFacebook.setAccessToken(values.getString(TOKEN));
                mFacebook.setAccessExpiresIn(values.getString(EXPIRES));
                if (mFacebook.isSessionValid()) {
                    SessionEvents.onLoginSuccess();
                    listener.onComplete(values);
                } else {
                    //shouldn't normally happen but in case it's not valid call errors
                    SessionEvents.onLoginError("Failed to receive access token.");
                    listener.onFacebookError(new FacebookError(
                                    "Failed to receive access token."));
                }
            }

            public void onError(DialogError error) {
                SessionEvents.onLoginError(error.getMessage());
                listener.onError(error);
            }

            public void onFacebookError(FacebookError error) {
                SessionEvents.onLoginError(error.getMessage());
                listener.onFacebookError(error);
            }

            public void onCancel() {
                listener.onCancel();
            }
        });
        dialog.setOwnerActivity(activity);
        return dialog;
    }
    
    public class LoginDialogListener implements DialogListener {
        public void onComplete(Bundle values) {
            CookieSyncManager.getInstance().sync();
            mFacebook.setAccessToken(values.getString(TOKEN));
            mFacebook.setAccessExpiresIn(values.getString(EXPIRES));
            if (mFacebook.isSessionValid()) {
                SessionEvents.onLoginSuccess();
            }else {
                SessionEvents.onLoginError("Failed to receive a valid access token.");
            }
            
        }

        public void onFacebookError(final FacebookError error) {
            final FileLoggerMessage msg = new FileLoggerMessage(FileLoggerMessage.FACEBOOK_CONNECT_ERROR, Utils.getWifiSignalStrength(mContext));
            msg.addMessage(error.getMessage());
            new Thread() {
                @Override public void run() {
                    PhotoTaggerApplication.getFileLogger().info(msg.toString());
                }
            }.start();
            SessionEvents.onLoginError(error.getMessage());
        }
        
        public void onError(final DialogError error) {
            
            String type = (error.getErrorCode() == WebViewClient.ERROR_TIMEOUT) ? FileLoggerMessage.FACEBOOK_DIALOG_TIMEOUT: FileLoggerMessage.FACEBOOK_DIALOG_ERROR;
            final FileLoggerMessage msg = new FileLoggerMessage(type, Utils.getWifiSignalStrength(mContext));
            msg.addMessage(error.getMessage());
            
            new Thread() {
                @Override public void run() {
                    PhotoTaggerApplication.getFileLogger().info(msg.toString());
                }
            }.start();
            SessionEvents.onLoginError(error.getMessage());
        }

        public void onCancel() {
            //SessionEvents.onLoginError("Action Canceled");
        }
    }
    
    public class LogoutRequestListener extends BaseRequestListener {
        public void onComplete(String response, final Object state) {
            // callback should be run in the original thread, 
            // not the background thread
            mHandler.post(new Runnable() {
                public void run() {
                    SessionStore.clearFacebookUser(mContext);
                    SessionEvents.onLogoutFinish();
                }
            });
        }
    }
    
    private class SessionListener implements AuthListener, LogoutListener {
        
        public void onAuthSucceed() {
            if (BuildConfig.DEBUG) {
                Log.d(getClass().toString(), "Auth Success: " + mFacebook.getAccessToken());
            }
            EasyTracker.getTracker().trackEvent("Facebook", "Login", Utils.md5(mFacebook.getAccessToken()), 0);
            SessionStore.saveFacebookSession(mFacebook, mContext);
            final FileLoggerMessage msg = new FileLoggerMessage(FileLoggerMessage.FACEBOOK_CONNECT_SUCCESS, Utils.getWifiSignalStrength(mContext));
            new Thread() {
                @Override public void run() {
                    PhotoTaggerApplication.getFileLogger().info(msg.toString());
                }
            }.start();
        }

        public void onAuthFail(String error) {
            if (BuildConfig.DEBUG) {
                Log.e(getClass().toString(), "onAuthFail error:" + error);
            }
        }
        
        public void onLogoutBegin() {           
        }
        
        public void onLogoutFinish() {
            SessionStore.clearFacebookSession(mContext);
        }
    }

	public Facebook getFacebook() {
		return this.mFacebook;
	}
	
	public String getFacebookToken() {
		return this.mFacebook.getAccessToken();
	}
}
