/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.poggled.android.phototagger.util;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

import java.io.File;
import java.security.NoSuchAlgorithmException;

/**
 * Class containing some static utility methods.
 */
public class Utils {
    public static final int IO_BUFFER_SIZE = 8 * 1024;
    
    private Utils() {};

    /**
     * Workaround for bug pre-Froyo, see here for more info:
     * http://android-developers.blogspot.com/2011/09/androids-http-clients.html
     */
    public static void disableConnectionReuseIfNecessary() {
        // HTTP connection reuse which was buggy pre-froyo
        if (hasHttpConnectionBug()) {
            System.setProperty("http.keepAlive", "false");
        }
    }

    /**
     * Get the size in bytes of a bitmap.
     * @param bitmap
     * @return size in bytes
     */
    @SuppressLint("NewApi")
    public static int getBitmapSize(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            return bitmap.getByteCount();
        }
        // Pre HC-MR1
        return bitmap.getRowBytes() * bitmap.getHeight();
    }

    /**
     * Check if external storage is built-in or removable.
     *
     * @return True if external storage is removable (like an SD card), false
     *         otherwise.
     */
    @SuppressLint("NewApi")
    public static boolean isExternalStorageRemovable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return Environment.isExternalStorageRemovable();
        }
        return true;
    }

    /**
     * Get the external app cache directory.
     *
     * @param context The context to use
     * @return The external cache dir
     */
    @SuppressLint("NewApi")
    public static File getExternalCacheDir(Context context) {
        if (hasExternalCacheDir()) {
            return context.getExternalCacheDir();
        }

        // Before Froyo we need to construct the external cache dir ourselves
        final String cacheDir = "/Android/data/" + context.getPackageName() + "/cache/";
        return new File(Environment.getExternalStorageDirectory().getPath() + cacheDir);
    }
    
    /**
     * Get the external app files directory.
     *
     * @param context The context to use
     * @return The external cache dir
     */
    @SuppressLint("NewApi")
    public static File getExternalFileDir(Context context) {
        if (hasExternalFileDir()) {
            return context.getExternalFilesDir(null);
        }

        // Before Froyo we need to construct the external cache dir ourselves
        final String packageDir = "/Android/data/" + context.getPackageName();
        return new File(Environment.getExternalStorageDirectory().getPath() + packageDir);
    }

    /**
     * Check how much usable space is available at a given path.
     *
     * @param path The path to check
     * @return The space available in bytes
     */
    @SuppressLint("NewApi")
    public static long getUsableSpace(File path) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return path.getUsableSpace();
        }
        final StatFs stats = new StatFs(path.getPath());
        return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
    }

    /**
     * Get the memory class of this device (approx. per-app memory limit)
     *
     * @param context
     * @return
     */
    public static int getMemoryClass(Context context) {
        return ((ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE)).getMemoryClass();
    }

    /**
     * Check if OS version has a http URLConnection bug. See here for more information:
     * http://android-developers.blogspot.com/2011/09/androids-http-clients.html
     *
     * @return
     */
    public static boolean hasHttpConnectionBug() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO;
    }

    /**
     * Check if OS version has built-in external cache dir method.
     *
     * @return
     */
    public static boolean hasExternalCacheDir() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    /**
     * Check if OS version has built-in external files dir method.
     *
     * @return
     */
    public static boolean hasExternalFileDir() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    /**
     * Check if ActionBar is available.
     *
     * @return
     */
    public static boolean hasActionBar() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }
    
    private static final String SIGNAL_UNIT = " RSSI";
    private static final String SIGNAL_NOT_ACTIVE = "Not Active";
    private static final String SIGNAL_NOT_ENABLED = "Not Enabled";
    /**
     * Get the current Wifi signal strength if Wifi is enabled, returns null otherwise.
     *
     * @return A string representing the RSSI signal strength measured in dbM.
     */
    public static String getWifiSignalStrength(Context context) {
        final WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        final StringBuilder sb = new StringBuilder();
        
        if(wifi.isWifiEnabled()){
            WifiInfo info = wifi.getConnectionInfo();  
            if(info != null) {
                sb.append(info.getRssi());
                sb.append(SIGNAL_UNIT);
            } else {
                sb.append(SIGNAL_NOT_ACTIVE);
            }
        } else {
            sb.append(SIGNAL_NOT_ENABLED);
        }
        
        return sb.toString();

    }
    
    
    public static String md5(String hashString) {

        StringBuffer hb = new StringBuffer();
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(hashString.getBytes());
            byte messageDigest[] = digest.digest();
    
            for (int i = 0; i < messageDigest.length; i++) {
                String hex = Integer.toHexString(0xff & messageDigest[i]);
                if (hex.length() == 1)
                    hb.append('0');
                hb.append(hex);
             }
            return hashString = hb.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
