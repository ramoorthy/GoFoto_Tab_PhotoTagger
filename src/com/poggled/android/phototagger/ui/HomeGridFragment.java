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

package com.poggled.android.phototagger.ui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;
import com.poggled.android.phototagger.BuildConfig;
import com.poggled.android.phototagger.R;
import com.poggled.android.phototagger.provider.Images;
import com.poggled.android.phototagger.service.ImageProcessingService;
import com.poggled.android.phototagger.ui.email.EmailActivity;
import com.poggled.android.phototagger.ui.prefs.SettingsActivity;
import com.poggled.android.phototagger.ui.survey.SurveyActivity;
import com.poggled.android.phototagger.util.BitmapUtils;
import com.poggled.android.phototagger.util.DiskLruCache;
import com.poggled.android.phototagger.util.ImageCache;
import com.poggled.android.phototagger.util.ImageCache.ImageCacheParams;
import com.poggled.android.phototagger.util.ImageLoader;
import com.poggled.android.phototagger.util.ImageResizer;
import com.poggled.android.phototagger.util.Utils;

/**
 * The main fragment that powers the ImageGridActivity screen. Fairly straight forward GridView
 * implementation with the key addition being the ImageWorker class w/ImageCache to load children
 * asynchronously, keeping the UI nice and smooth and caching thumbnails for quick retrieval. The
 * cache is retained over configuration changes like orientation change so the images are populated
 * quickly as the user rotates the device.
 */
public class HomeGridFragment extends Fragment implements AdapterView.OnItemClickListener {
	public static final String POSITION = "position";
	public static final String ELEMENTS = "elements";
	
    private static final String TAG = "ImageGridFragment";
    private static final String IMAGE_CACHE_DIR = "thumbs";

    private int mImageThumbSize;
    private int mImageThumbSpacing;
    private ImageAdapter mAdapter;
    private ImageResizer mImageWorker;
    
    private FileObserver mObserver;
    
   
    
    /**
     * Empty constructor as per the Fragment documentation
     */
    public HomeGridFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        
        
       
        mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
        mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

        mAdapter = new ImageAdapter(getActivity());

        ImageCacheParams cacheParams = new ImageCacheParams(IMAGE_CACHE_DIR);

        // Allocate a fourth of the per-app memory limit to the bitmap memory cache. This value
        // should be chosen carefully based on a number of factors. 
        cacheParams.memCacheSize = 1024 * 1024 * Utils.getMemoryClass(getActivity()) / 4;
        // The ImageWorker takes care of loading images into our ImageView children asynchronously
        mImageWorker = new ImageLoader(getActivity(), mImageThumbSize);
        mImageWorker.setAdapter(new Images(getActivity()));
        mImageWorker.setLoadingImage(R.drawable.loading_square);
        mImageWorker.setImageCache(ImageCache.findOrCreateCache(getActivity(), cacheParams));
        
        final File dir = new File(Utils.getExternalCacheDir(HomeGridFragment.this.getActivity()), ImageProcessingService.OUTPUT_DIR_NAME);
        mObserver = new ProcessedFileObserver(dir.getAbsolutePath());
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	
        final View v = inflater.inflate(R.layout.photo_grid_fragment, container, false);
        
        final GridView mGridView = (GridView) v.findViewById(R.id.gridView);
        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemClickListener(this);
        
        //start watching for updates after the adapter 
        mObserver.startWatching();

        // This listener is used to get the final width of the GridView and then calculate the
        // number of columns and the width of each column. The width of each column is variable
        // as the GridView has stretchMode=columnWidth. The column width is used to set the height
        // of each view so we get nice square thumbnails.
        mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    public void onGlobalLayout() {
                        if (mAdapter.getNumColumns() == 0) {
                            final int numColumns = (int) Math.floor(
                                    mGridView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
                            if (numColumns > 0) {
                                final int columnWidth =
                                        (mGridView.getWidth() / numColumns) - mImageThumbSpacing;
                                mAdapter.setNumColumns(numColumns);
                                mAdapter.setItemHeight(columnWidth);
                                if (BuildConfig.DEBUG) {
                                    Log.d(TAG, "onCreateView - numColumns set to " + numColumns);
                                }
                            }
                        }
                    }
                });

        return v;
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onDestroyView()
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mObserver.stopWatching();
    }

    @Override
    public void onResume() {
        super.onResume();
        mImageWorker.setExitTasksEarly(false);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        mImageWorker.setExitTasksEarly(true);
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onDestroy()
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // remove the reference to the file observer so that we don't leak it
        mObserver = null;
    }


    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        final Intent i = new Intent(getActivity(), PhotoDetail.class);
        i.putExtra(POSITION, position);
        startActivity(i);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.grid_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_all:
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                final int count = mImageWorker.getAdapter().getSize();
                builder.setMessage(getResources().getQuantityString(R.plurals.dialog_delete_all_text, count, count))
                        .setTitle(R.string.dialog_delete_all_title)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                ProgressDialog progress = ProgressDialog.show(getActivity(), 
                                        getString(R.string.dialog_delete_all_progress_title), 
                                        getString(R.string.dialog_delete_all_progress_text), true, true);
                                deleteAll();
                                progress.dismiss();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
                return true;
            case R.id.refresh:
                refresh();
                return true;
            case R.id.clear_cache:
                clearCache(true);
                return true;
            case R.id.settings:
                Intent i = new Intent(getActivity(), SettingsActivity.class);
                startActivity(i);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    
    private void refresh() {
        mImageWorker.setAdapter(new Images(getActivity()));
        mAdapter.notifyDataSetChanged();
    }
    
    private void clearCache(boolean notifyUser) {
        final ImageCache cache = mImageWorker.getImageCache();
        if (cache != null) {
            mImageWorker.getImageCache().clearCaches();
            DiskLruCache.clearCache(getActivity(), IMAGE_CACHE_DIR);
            if(notifyUser) {
                Toast.makeText(getActivity(), R.string.clear_cache_complete,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void deleteAll() {
        
//        ProgressDialog dialog = ProgressDialog.show(getActivity(), "Status", 
//                "Deleting all photos...", true, true);
        final File[] files = ((Images) mImageWorker.getAdapter()).getFileList();
        File eyeFiDir = new File(Environment.getExternalStorageDirectory() + File.separator + "Eye-Fi");
        for(File file:files) {
            File eyeFiFile = new File(eyeFiDir, file.getName());
            if(eyeFiFile.exists()) {
                eyeFiFile.delete();
            }
            if(file.exists()) file.delete();
        }
        clearCache(false);
        refresh();
        //if(dialog.isShowing()) dialog.cancel();
    }
    /**
     * The main adapter that backs the GridView.
     */
    private class ImageAdapter extends BaseAdapter {

        private final Context mContext;
        private int mItemHeight = 0;
        private int mNumColumns = 0;
        private GridView.LayoutParams mImageViewLayoutParams;

        public ImageAdapter(Context context) {
            super();
            mContext = context;
            mImageViewLayoutParams = new GridView.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        }


        public int getCount() {
            //return mImageWorker.getAdapter().getSize();
        	return HomeGridActivity.bitmapArray.size();
        }


        public Object getItem(int position) {
            return mImageWorker.getAdapter().getItem(position);
        }


        public long getItemId(int position) {
            return position;
        }


        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
        //---returns an ImageView view---
        public View getView(int position, View convertView, ViewGroup parent) 
        {
            ImageView imageView;
            if (convertView == null) {
                imageView = new ImageView(getActivity());
                imageView.setLayoutParams(new GridView.LayoutParams(185, 185));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(5, 5, 5, 5);
            } else {
                imageView = (ImageView) convertView;
            }
            imageView.setImageBitmap(HomeGridActivity.bitmapArray.get(position));
            return imageView;
        }
      
        /*
        public View getView(int position, View convertView, ViewGroup container) {

            // Handle the main ImageView thumbnails
            ImageView imageView;
            if (convertView == null) { // if it's not recycled, instantiate and initialize
                imageView = new ImageView(mContext);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setLayoutParams(mImageViewLayoutParams);
            } else { // Otherwise re-use the converted view
                imageView = (ImageView) convertView;
            }
            
            // Check the height matches our calculated column width
            if (imageView.getLayoutParams().height != mItemHeight) {
                imageView.setLayoutParams(mImageViewLayoutParams);
            }
            
            imageView.setImageBitmap(bitmapArray.get(position));
            
            // Finally load the image asynchronously into the ImageView, this also takes care of
            // setting a placeholder image while the background thread runs
            mImageWorker.loadImage(position, imageView);
            return imageView;
        }
        */
        /**
         * Sets the item height. Useful for when we know the column width so the height can be set
         * to match.
         *
         * @param height
         */
        public void setItemHeight(int height) {
            if (height == mItemHeight) {
                return;
            }
            mItemHeight = height;
            mImageViewLayoutParams =
                    new GridView.LayoutParams(LayoutParams.MATCH_PARENT, mItemHeight);
            mImageWorker.setImageSize(height);
            notifyDataSetChanged();
        }

        public void setNumColumns(int numColumns) {
            mNumColumns = numColumns;
        }

        public int getNumColumns() {
            return mNumColumns;
        }
    }
     
    public class ProcessedFileObserver extends FileObserver {

        public String mAbsolutePath;
        private Handler mHandler;

        public ProcessedFileObserver(String path) {
            //super(path, FileObserver.ALL_EVENTS);
            super(path, CLOSE_WRITE | MOVED_TO | MOVED_FROM | DELETE);
            mAbsolutePath = path;
            mHandler = new Handler();
        }

        @Override
        public void onEvent(final int event, String path) {
            if (path == null) {
                return;
            }

            final File file = new File(mAbsolutePath + File.separator + path);
            // check if the file exist and is an image
            if (file.exists() && BitmapUtils.isImage(file)) {
                if (BuildConfig.DEBUG) {
                    Log.i(getClass().getName(), "PhotoGrid: Received notification file written, deleted, or moved: " + file.getName());
                }
                // update the ui; ui should always be updated from the main thread
                mHandler.post(new Runnable() {
                    public void run() {
                        if(event ==  FileObserver.MOVED_FROM | event == FileObserver.DELETE) {
                            mImageWorker.getImageCache().removeBitmapFromCache(file.getAbsolutePath());
                        }
                        mImageWorker.setAdapter(new Images(getActivity()));
                        mAdapter.notifyDataSetChanged();
                    }
    
                });
            }
           

        }
    }
}
