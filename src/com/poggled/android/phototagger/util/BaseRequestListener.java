package com.poggled.android.phototagger.util;

import android.util.Log;

import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.FacebookError;
import com.poggled.android.phototagger.BuildConfig;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Skeleton base class for RequestListeners, providing default error 
 * handling. Applications should handle these error conditions.
 *
 */
public abstract class BaseRequestListener implements RequestListener {

    
    //TODO: add defaults to toast messages through the application context
    public void onFacebookError(FacebookError e, final Object state) {
        if(BuildConfig.DEBUG)
            Log.e("Facebook", e.getMessage());
   
    }

    public void onFileNotFoundException(FileNotFoundException e,
                                        final Object state) {
        if(BuildConfig.DEBUG)
            Log.e("Facebook", e.getMessage());
        
    }

    public void onIOException(IOException e, final Object state) {
        if(BuildConfig.DEBUG)
            Log.e("Facebook", e.getMessage());
        
    }

    public void onMalformedURLException(MalformedURLException e,
                                        final Object state) {
        
        if(BuildConfig.DEBUG)
            Log.e("Facebook", e.getMessage());
    }
    
}
