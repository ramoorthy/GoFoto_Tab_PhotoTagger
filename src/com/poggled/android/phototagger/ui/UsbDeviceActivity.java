/**
 * PhotoTagger
 * UsbDeviceActivity.java
 * 
 * @author Jason Harris on Aug 23, 2012
 * @copyright 2012 Poggled, Inc. All rights reserved
 * 
 * Typical Usage: Called by a broadcast intent to launch the ImageProcessingService
 * 
 */

package com.poggled.android.phototagger.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.poggled.android.phototagger.service.ImageProcessingService;

/* This Activity does nothing but receive USB_DEVICE_ATTACHED events from the
 * USB service and initiates the Image Processing Service
 */
public final class UsbDeviceActivity extends Activity {

    static final String TAG = "UsbDeviceActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        intent.setClass(this, ImageProcessingService.class);
            startService(intent);
        finish();
    }
}