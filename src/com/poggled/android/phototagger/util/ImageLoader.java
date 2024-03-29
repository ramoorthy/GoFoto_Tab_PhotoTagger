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
 * 
 * @author - modified from ImageFetcher.java by Jason Harris on Jun 21, 2012
 * 
 */

package com.poggled.android.phototagger.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.poggled.android.phototagger.BuildConfig;

/**
 * A simple subclass of {@link ImageResizer} that fetches images from files.
 * 
 */
public class ImageLoader extends ImageResizer {
    private static final String TAG = "ImageLoader";

    /**
     * Initialize providing a target image width and height for the processing images.
     *
     * @param context
     * @param imageWidth
     * @param imageHeight
     */
    public ImageLoader(Context context, int imageWidth, int imageHeight) {
        super(context, imageWidth, imageHeight);
    }

    /**
     * Initialize providing a single target image size (used for both width and height);
     *
     * @param context
     * @param imageSize
     */
    public ImageLoader(Context context, int imageSize) {
        super(context, imageSize);
    }

    /**
     * The main process method, which will be called by the ImageWorker in the AsyncTask background
     * thread.
     *
     * @param data The data to load the bitmap, in this case, a filepath
     * @return The loaded and resized bitmap
     */
    private Bitmap processBitmap(String data) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "processBitmap - " + data);
        }

        // Return a sampled down version
        return decodeSampledBitmapFromFile(data, mImageWidth, mImageHeight);
    }

    @Override
    protected Bitmap processBitmap(Object data) {
        return processBitmap(String.valueOf(data));
    }

  
}
