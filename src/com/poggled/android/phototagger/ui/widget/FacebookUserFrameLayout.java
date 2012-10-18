package com.poggled.android.phototagger.ui.widget;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.FacebookError;
import com.poggled.android.phototagger.BuildConfig;
import com.poggled.android.phototagger.PhotoTaggerApplication;
import com.poggled.android.phototagger.R;
import com.poggled.android.phototagger.ui.TagPhotoActivity;
import com.poggled.android.phototagger.util.FacebookConnector;
import com.poggled.android.phototagger.util.ImageCache;
import com.poggled.android.phototagger.util.ImageCache.ImageCacheParams;
import com.poggled.android.phototagger.util.ImageFetcher;
import com.poggled.android.phototagger.util.ImageResizer;
import com.poggled.android.phototagger.util.SessionEvents;
import com.poggled.android.phototagger.util.SessionEvents.AuthListener;
import com.poggled.android.phototagger.util.SessionEvents.LogoutListener;
import com.poggled.android.phototagger.util.SessionStore;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;

public class FacebookUserFrameLayout extends FrameLayout implements AuthListener, LogoutListener, OnClickListener {
    
    private static final String IMAGE_CACHE_DIR = "thumbs/facebook";
    
    private ImageView mUserPic;
    private TextView mUserName;
    private TextView mLogOut;
    private LinearLayout mLoggedInContainer;
    private FacebookConnector mFacebookConnector;
    private ImageResizer mImageWorker;
    //private FbAPIAuthListener mFbAuthListener;
    
    public FacebookUserFrameLayout(Context context) {
        super(context);
        initView(context);
    }

    public FacebookUserFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public FacebookUserFrameLayout(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }
    
    private void initView(Context context) {
        final LayoutInflater inflator = LayoutInflater.from(context);
        View v = inflator.inflate(R.layout.fb_user, this);
        mUserPic = (ImageView) v.findViewById(R.id.imgFbPic);
        mUserName = (TextView) v.findViewById(R.id.txtFbName);
        mLogOut = (TextView) v.findViewById(R.id.txtFbLogOut);
        mLoggedInContainer = (LinearLayout) v.findViewById(R.id.layoutFacebookLoggedIn);
        
        setOnClickListener(this);
        
        ImageCacheParams cacheParams = new ImageCacheParams(IMAGE_CACHE_DIR); 

        // We shouldn't need too much memory for the facebook pic since it is only one small image
        cacheParams.memCacheSize = 1024 * 1024; //1MB
        
        // The ImageWorker takes care of loading images into our ImageView children asynchronously
        mImageWorker = new ImageFetcher(getContext(), 50, 50);
        mImageWorker.setImageCache(new ImageCache(getContext(), cacheParams));
        mImageWorker.setImageFadeIn(false);
        
        // Setup our Facebook connectivity 
        mFacebookConnector = PhotoTaggerApplication.getFacebookConnector();
        //mFbAuthListener = new FbAPIAuthListener();
        
        // Finally update the interface facebook information
        getFacebookUserInfo();
    }
    
    /* (non-Javadoc)
     * @see android.view.View#onAttachedToWindow()
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        SessionEvents.addAuthListener(this);
        SessionEvents.addLogoutListener(this);
    }

    /* (non-Javadoc)
     * @see android.view.View#onDetachedFromWindow()
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        SessionEvents.removeAuthListener(this);
        SessionEvents.removeLogoutListener(this);
    }

    private void updateFacebookStatus() {
        if( mFacebookConnector.getFacebook().isSessionValid()) {
           
            mLoggedInContainer.setVisibility(View.VISIBLE);
            mLogOut.setText(this.getResources().getString(R.string.logout_menu));
            final HashMap<String, String> user = SessionStore.restoreFacebookUser(this.getContext());
            final String id = user.get(SessionStore.FB_ID);
            if(id != null ) {
                mUserName.setText(user.get(SessionStore.FB_NAME));
                mImageWorker.loadImage("https://graph.facebook.com/" + id + "/picture", mUserPic);
            } else {
                // set some default text?
            }
        } else {
            
            // TODO: move to string resources
            mLogOut.setText("Signed Out");
            mLoggedInContainer.setVisibility(View.GONE);
        }
    }
    
    private void getFacebookUserInfo() { 
        final HashMap<String, String> user = SessionStore.restoreFacebookUser(this.getContext());
        
        // Don't get user information if we already have it; instead update the view
        if(mFacebookConnector.getFacebook().isSessionValid() && user.get(SessionStore.FB_ID) == null ) {
            // TODO: move to string resources
            mLogOut.setText("Loading...");
            Bundle params = new Bundle();
            params.putString("fields", "name, picture");
            AsyncFacebookRunner runner = new AsyncFacebookRunner(mFacebookConnector.getFacebook());
            runner.request("me", params, new FbUserInfoRequestListener());
        } else {
            updateFacebookStatus();
        }
        

    }
    
    /**
     * Set on the ImageView in the ViewPager children fragments, to launch the facebook dialog or tagging view
     * when the ImageView is touched.
     */
    public void onClick(View v) {
        PhotoTaggerApplication.logout(this.getContext());
        if (TagPhotoActivity.class.isInstance(this.getContext())) {
            ((Activity) this.getContext()).finish();
        }

// TODO: implement login from activity bar?       
//      if(mFacebookConnector.getFacebook().isSessionValid()) {
//      PhotoTaggerApplication.logout(this.getContext());
//        if (TagPhotoActivity.class.isInstance(this.getContext())) {
//            ((Activity) this.getContext()).finish();
//        }
//        } else {
//          Dialog authDialog = mFacebookConnector.getCustomDialogAuth(this, new DialogListener() {
//          public void onComplete(Bundle values) {
//              //getFacebookUserInfo();
//              if(mCreateAccount) {
//                  registerPoggledAccount(mFacebookConnector.getFacebookToken());
//              } 
//              //proceed to photo tagging view
//              launchTagPhotoActivity();
//          }
//
//          @Override
//          public void onFacebookError(FacebookError e) {
//              
//              Toast.makeText(PhotoDetailActivity.this, "Unable to connect to Facebook.  Error: " + e.toString(),
//                      Toast.LENGTH_LONG).show();
//          }
//
//          @Override
//          public void onError(DialogError e) {
//              Toast.makeText(PhotoDetailActivity.this, "Unable to connect to Facebook.  Error: " + e.toString(),
//                      Toast.LENGTH_LONG).show();
//              
//          }
//
//          @Override
//          public void onCancel() {
//              //we really don't need to bug the user if they cancel the dialog
//          }
//          
//      });
//      
//      authDialog.show();
//      CheckedTextView chkBox = (CheckedTextView) authDialog.findViewById(R.id.checktxt_create_account);
//      chkBox.setOnClickListener(new View.OnClickListener() {
//          public void onClick(View v) {
//              final CheckedTextView check = (CheckedTextView) v;
//              check.toggle();
//              mCreateAccount = check.isChecked();
//              
//          }
//      });
//        }
        
    }
    

    public void onAuthSucceed() {
        final HashMap<String, String> user = SessionStore.restoreFacebookUser(this.getContext());
        if(mFacebookConnector.getFacebook().isSessionValid() && user.get(SessionStore.FB_ID) != null) {
            updateFacebookStatus();
        } else {
            getFacebookUserInfo();
        }
    }

   
    public void onAuthFail(String error) {
        updateFacebookStatus();
    }
    
    public void onLogoutBegin() {
        // TODO: move to string resources
        //if(!mLogOut.getText().equals("Signed Out")) 
            mLogOut.setText("Logging out...");
        
    }
    
    public void onLogoutFinish() {
        updateFacebookStatus();
    }

    private class FbUserInfoRequestListener implements AsyncFacebookRunner.RequestListener {


        public void onComplete(final String response, final Object state) {
            
            if(BuildConfig.DEBUG)
                Log.d(getClass().getName(), "onComplete: " + response);
            
            ((Activity) FacebookUserFrameLayout.this.getContext()).runOnUiThread(new Runnable() {
                public void run() {
                    if(response != null) {
                        try {
                            SessionStore.saveFacebookUser(new JSONObject(response), FacebookUserFrameLayout.this.getContext());
                            updateFacebookStatus();
                            
                        } catch (JSONException e) {
                            Toast.makeText(FacebookUserFrameLayout.this.getContext(), "Unable to read response from Facebook.  Please try again.",
                                    Toast.LENGTH_LONG).show();
                        }
                    } else {    
                        
                        Toast.makeText(FacebookUserFrameLayout.this.getContext(), "Unable to get user information from Facebook.  Please try again.",
                                Toast.LENGTH_LONG).show();;
                    }
                    
                }
            });
            
            
        }
        
        


        public void onIOException(final IOException e, final Object state) {
            if(BuildConfig.DEBUG) {
                Log.e(getClass().getName(), "onIOException: " + e);
            }
            
//            runOnUiThread(new Runnable() {
//                public void run() {
//                    
//                    Toast.makeText(FacebookUserFrameLayout.this.getContext(), "Unable to get user information from Facebook.  Please try again.",
//                            Toast.LENGTH_LONG).show();;
//                }
//            });
            
            
        }


        public void onFileNotFoundException(final FileNotFoundException e,
                final Object state) {
            if(BuildConfig.DEBUG) {
                Log.e(getClass().getName(), "onFileNotFoundException: " + e);
            }
//            runOnUiThread(new Runnable() {
//                public void run() {
//                    
//                    Toast.makeText(FacebookUserFrameLayout.this.getContext(), "Unable to get user information from Facebook.  Please try again.",
//                            Toast.LENGTH_LONG).show();
//                }
//            });
            
            
        }


        public void onMalformedURLException(final MalformedURLException e,
                final Object state) {
            
            if(BuildConfig.DEBUG)
                Log.e(getClass().getName(), "onMalformedURLException: " + e);
            
//            runOnUiThread(new Runnable() {
//                public void run() {
//                    
//                    Toast.makeText(FacebookUserFrameLayout.this.getContext(), "Unable to get user information from Facebook.  Please try again.",
//                            Toast.LENGTH_LONG).show();;
//                }
//            });
            
            
        }


        public void onFacebookError(final FacebookError e, final Object state) {
            if(BuildConfig.DEBUG)
                Log.e(getClass().getName(), "onFacebookError: " + e);
            
//            runOnUiThread(new Runnable() {
//                public void run() {
//                    
//                    Toast.makeText(FacebookUserFrameLayout.this.getContext(), "Unable to get user information from Facebook.  Please try again.",
//                            Toast.LENGTH_LONG).show();;
//                }
//            });
            
        }
        
    }

}
