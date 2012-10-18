package com.poggled.android.phototagger.io;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import com.poggled.android.phototagger.BuildConfig;

public class FacebookParser {

    
    public static void parseFacebookISer(String response)throws JSONException {
        boolean success = false;
        String errorMessage = null;
            final JSONObject json = new JSONObject(response);
            if(BuildConfig.DEBUG) {
                Log.d(StringParser.class.getName(), json.toString(5));
            }
            final String email = json.getString("email");
            final String id = json.getString("id");
            
        
    }
}
