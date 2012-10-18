package com.poggled.android.phototagger.io;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.poggled.android.phototagger.BuildConfig;

public class StringParser
{

	public static boolean parseSuccess(String response)throws Exception {
		boolean success = false;
		String message = null;
		try {
			final JSONObject json = new JSONObject(response);
			if(BuildConfig.DEBUG) {
				Log.d(StringParser.class.getName(), json.toString(5));
			}
			
			final JSONObject status = json.getJSONObject("status");
			final int code =  status.getInt("code");
			
			if(code == 200) {
			    final String id = json.getJSONObject("data").getJSONObject("user").getString("id");
			    if(!id.equals(null) && id.length() > 0) {
			        success = true;
			    }
			} else {
			    message =  status.optString("message");
			    if(code == 615) { //user already exists
			        return true; //ignore this message
			    }
			}
		} catch (JSONException e) {
			if(BuildConfig.DEBUG)
				Log.d(StringParser.class.getName(), e.toString());

			throw new RuntimeException("Error reading response from server."); 
		}	
		if (message != null)
			throw new RuntimeException(message);
		return success;
	}
	

}
