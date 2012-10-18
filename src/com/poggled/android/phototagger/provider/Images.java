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

/*
 * Modified by @author Jason Harris
 */
     
package com.poggled.android.phototagger.provider;

import android.content.Context;
import android.util.Log;

import com.poggled.android.phototagger.BuildConfig;
import com.poggled.android.phototagger.service.ImageProcessingService;
import com.poggled.android.phototagger.util.BitmapUtils;
import com.poggled.android.phototagger.util.ImageWorker.ImageWorkerAdapter;
import com.poggled.android.phototagger.util.Utils;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Images from the Eye-Fi directory
 */
public class Images extends ImageWorkerAdapter{
    
    private File mPath;
    
    private File[] mFiles;
    
    // constructor to retrieve files based on output directory from image processing service
    public Images(Context c) {
        this.mPath = new File(Utils.getExternalCacheDir(c), ImageProcessingService.OUTPUT_DIR_NAME);
        getFileList();
    }
    
    public Images(String path) {
        this.mPath = new File(path);
        getFileList();
    }
    
    public Images(File path) {
        this.mPath = path;
        getFileList();
    }
    
    public File[] getFileList() {

        // Checks whether path exists
        if (mPath.exists()) {
            
            FileFilter filter = new FileFilter() {
  
                public boolean accept(File filename) {
                    // Filters based on whether the file is hidden or not
                    return BitmapUtils.isImage(filename);

                }
            };
            
            mFiles = mPath.listFiles(filter);
            if (BuildConfig.DEBUG) {
                for (int i = 0; i < mFiles.length; i++) {
                    Log.v(getClass().getName(), "files list [" + i + "] " + mFiles[i]);
                }
            }
        } else {
            mPath.mkdirs();
            mFiles = new File[0];
            if (BuildConfig.DEBUG) {
                Log.e(getClass().getName(), "Path not found. Empty directory created.");
            }
        }
        
        Arrays.sort( mFiles, new FileComparator());
        return mFiles;
    }
    
    public String[] getFileNames() {
        String[] filenames = new String[mFiles.length];
        for(int i = 0; i<mFiles.length; i++) {
            filenames[i] = mFiles[i].getName();
        }
        return filenames;
    }
    
    @Override
    public Object getItem(int num) {
        return mFiles[num].getAbsolutePath();
    }

    @Override
    public int getSize() {
        return mFiles.length;
    }
    
    /**
     * The comparator used to determine ordering so that we can obtain a sorted collection 
     * from a given collection of File objects.  Determines the odering with the compare(File first, File second) method.
     */
    private static class FileComparator implements Comparator<File> {
        
        /* 
         * {@inheritDoc}
         * 
         * This method will return negative values for greater lastModified dates and positive values for
         * smaller lastModified dates, thus, it a list sorted by this method will be most recent first 
         * (chronologically reverse order).
         */
        public int compare(File first, File second) {
            
            if(first.lastModified() > second.lastModified()) {
                return -1;
            } else if (first.lastModified() < second.lastModified()) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
  
