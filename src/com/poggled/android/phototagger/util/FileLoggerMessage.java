/**
 * PhotoTagger
 * FileLoggerMessage.java
 * 
 * @author Jason Harris on Aug 10, 2012
 * @copyright 2012 Poggled, Inc. All rights reserved
 * 
 * Typical Usage: 
 * 
 */
package com.poggled.android.phototagger.util;

import java.util.ArrayList;

/**
 * A utility class for generating message statements for the local file Logger.
 */
public class FileLoggerMessage {
    
    public static final String DELIMITER = ",";
    
    public static final String IMAGE_RECEIVED = "Image Received";
    
    public static final String FACEBOOK_DIALOG = "Facebook Dialog Opened";
    public static final String FACEBOOK_DIALOG_TIMEOUT = "Facebook Dialog Timeout";
    public static final String FACEBOOK_DIALOG_ERROR = "Facebook Dialog Error";
    public static final String FACEBOOK_CONNECT_SUCCESS = "Facebook Connect Success";
    public static final String FACEBOOK_CONNECT_ERROR = "Facebook Connect Error";
    
    public static final String FACEBOOK_POST_IMAGE = "Facebook Post Image";
    public static final String FACEBOOK_POST_IMAGE_SUCCESS = "Facebook Post Image Success";
    public static final String FACEBOOK_POST_IMAGE_TIMEOUT = "Facebook Post Image Timeout";
    public static final String FACEBOOK_POST_IMAGE_ERROR = "Facebook Post Image Error";
    
    public static final String FACEBOOK_TAG_IMAGE = "Facebook Tag Image";
    public static final String FACEBOOK_TAG_IMAGE_SUCCESS = "Facebook Tag Image Success";
    public static final String FACEBOOK_TAG_IMAGE_TIMEOUT = "Facebook Tag Image Timeout";
    public static final String FACEBOOK_TAG_IMAGE_ERROR = "Facebook Tag Image Error";
    
    private ArrayList<String> mAdditionalMsgs = new ArrayList<String>();
    private String mSignal;
    private String mMessage;
    
    public FileLoggerMessage(String msg, String signal) {
        mMessage = msg;
        mSignal = signal;
    }
    
    public void addMessage(String msg) {
        mAdditionalMsgs.add(msg);
    }
    
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        
        sb.append(mSignal);
        sb.append(DELIMITER);
        sb.append(mMessage);
        
        for (String s: mAdditionalMsgs) {
            sb.append(DELIMITER);
            sb.append(s);
        }
        return sb.toString();
    }
}
