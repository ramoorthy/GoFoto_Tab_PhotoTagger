/**
 * PhotoTagger
 * WatermarkFragment.java
 * 
 * @author Jason Harris on Jun 19, 2012
 * @copyright 2012 Poggled, Inc. All rights reserved
 * 
 * Typical Usage: Used to select images to display as the sponsor's watermark
 * 
 */

package com.poggled.android.phototagger.ui.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.poggled.android.phototagger.BuildConfig;
import com.poggled.android.phototagger.R;
import com.poggled.android.phototagger.provider.Images;
import com.poggled.android.phototagger.service.ImageProcessingService;
import com.poggled.android.phototagger.util.ImageCache;
import com.poggled.android.phototagger.util.ImageCache.ImageCacheParams;
import com.poggled.android.phototagger.util.ImageLoader;
import com.poggled.android.phototagger.util.Utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *  A Fragment for displaying and selecting watermarks for use in the Image Processing Service. 
 */
public class WatermarkFragment extends ListFragment implements LoaderManager.LoaderCallbacks<List<File>> {
    
    private static final String IMAGE_DIR = "watermarks";
    
    private WatermarkArrayAdapter mAdapter;
    
    private TextView mEmptyText;
    
    /* 
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.settings_list_fragment, container, false);
        mEmptyText = (TextView) view.findViewById(android.R.id.empty);
        return view;
    }
    
    
    /* 
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        ArrayList<File> imgList = getLocalWatermarks();
        
        mAdapter = new WatermarkArrayAdapter(this.getActivity(), android.R.id.text1, imgList); 
        setListAdapter(mAdapter);
        
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        
        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        getActivity().getSupportLoaderManager().initLoader(0, null, this).forceLoad();
        
        final String watermarkFileName = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(getString(R.string.pref_watermark_key), null);
        
        if(watermarkFileName != null) {
            final File watermarkFile = new File(watermarkFileName);
            if(imgList.contains(watermarkFile)) {
                getListView().setItemChecked(imgList.indexOf(watermarkFile), true);
            } else {
                // the saved file is not found in our directory of available images (moved, deleted?)
                // so we need to clear out the persistant storage or our watermarked filename
                saveWatermark(null);
            }
        }
        
        if(imgList.size() < 1)
            setEmptyText("Loading available watermarks from server...");
        //setListShown(false);
    }
    
    
    /* 
     * {@inheritDoc}
     */
    @Override
    public void setEmptyText(CharSequence text) {
        
        // Method is overridden to provide the most basic default behavior that we lose by using a custom layout.
        if(mEmptyText !=  null) {
            mEmptyText.setText(text);
        }
    }

    /* 
     * {@inheritDoc}
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        
        // Save the filename into persistent storage
        final String filename = ((File) l.getItemAtPosition(position)).getAbsolutePath();
        saveWatermark(filename);
        
        // Decode and update the service on a background thread since this can sometimes take a few moments
        new Thread() {
            @Override public void run() {
                // update the reference to the new sponsor's watermark
                Bitmap bitmap = BitmapFactory.decodeFile(filename);
                ImageProcessingService.mSponsorBitmap = bitmap;
            }
        }.start();
        
        // Close the activity
        getActivity().finish();
    }

    /**
     * Returns the watermarks that will be displayed initially from the image directory for watermarks.
     * 
     * @return An ArrayList of File objects 
     */
    private ArrayList<File> getLocalWatermarks() {
        
        Images imgs = new Images (new File(Utils.getExternalFileDir(this.getActivity()), IMAGE_DIR));
        ArrayList<File> imgList = new ArrayList<File>(Arrays.asList(imgs.getFileList()));
        Collections.sort(imgList);
        return imgList;
        
    }
    
    private void saveWatermark(String filename) {
        // Save the filename into persistent storage
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.pref_watermark_key), filename);
        editor.commit();
    }
    /* 
     * {@inheritDoc}
     */

    public Loader<List<File>> onCreateLoader(int id, Bundle args) {
        return new WatermarkLoader(getActivity());
    }

    /* 
     * {@inheritDoc}
     */

    public void onLoaderReset(Loader<List<File>> loader) {
        
        // We want to reload our initial local files but only if the activity is not finishing or gone
        // (Activity seems to be resetting the loader  when pressing back).
        
        if(getActivity() != null && !getActivity().isFinishing()) {
            setEmptyText("Loading available watermarks from server...");
            getActivity().setProgressBarIndeterminateVisibility(true);
            
            // Clear the data in the adapter and load any local watermarks
            mAdapter.setData(getLocalWatermarks());
        } else {
            mAdapter.setData(null);
        }
    }
    
    /**
     *  Helper function that removes all watermark files and queries the server for new matches.
     *  
     *  Useful for clearing out old images that store locally.
     * 
     */
    public void deleteAndReset() {
        setEmptyText("Loading available watermarks from server...");
        getListView().setItemChecked(getListView().getCheckedItemPosition(), false);
        saveWatermark(null);
        for(int i = 0; i < mAdapter.getCount(); i++) {
            mAdapter.getItem(i).delete();
        }
        mAdapter.clear();
        getActivity().getSupportLoaderManager().restartLoader(0, null, this).forceLoad();
    }
    
    /* 
     * {@inheritDoc}
     */

    public void onLoadFinished(Loader<List<File>> loader, List<File> data) {
        
        setEmptyText("No watermarks found.");
        getActivity().setProgressBarIndeterminateVisibility(false); 
        
        // Notify the user of problems encountered.
        final Exception error = ((WatermarkLoader) loader).getError();
        
        if(error != null ) {
            Toast.makeText(getActivity(), "There was a problem retrieving watermarks from the server.", Toast.LENGTH_LONG).show();
            
        } 
        
        // Set the new data in the adapter. We will just add without clearing the adapter since our Loader 
        // only returns new files that were found.
        mAdapter.addAll(data);
    }

    /**
     * An Adapter that is backed by an array of files.  The class takes care of image loading with the use of an image worker
     * off of the UI thread.  This class returns custom views and most of the implementation for adding files is the default
     * implementation from the parent class.
     * 
     */
    private static class WatermarkArrayAdapter extends ArrayAdapter<File> {
        
        private static final String IMAGE_CACHE_DIR = "watermarks";
        
        private ImageLoader mImageWorker;
        
        /**
         * The main constructor for the watermark array adapter implementation.
         * 
         * @param context The current context 
         * @param textViewResourceId The textview resource id to be set in the parent class
         * @param objects The objects to represent in the ListView.
         */
        public WatermarkArrayAdapter(Context context, int textViewResourceId, List<File> objects) {
            
            super(context, textViewResourceId, objects);
            ImageCacheParams cacheParams = new ImageCacheParams(IMAGE_CACHE_DIR); 

            cacheParams.memCacheSize = 1024 * 1024 * 2; //2MB
            cacheParams.diskCacheEnabled = false;
            
            // The ImageWorker takes care of loading images into our ImageView children asynchronously
            mImageWorker = new ImageLoader(context, 100, 100);
            mImageWorker.setImageCache(new ImageCache(context, cacheParams));
            mImageWorker.setLoadingImage(R.drawable.logo);
            mImageWorker.setImageFadeIn(false);
        }
        
        /* 
         * {@inheritDoc}
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            ViewHolder holder;
            if (view == null) {
                holder = new ViewHolder();
                LayoutInflater vi = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = vi.inflate(R.layout.settings_list_item, parent, false);
                holder.chcktxtWatermarkName = (TextView) view.findViewById(android.R.id.text1);
                holder.image = (ImageView) view.findViewById(R.id.imgWatermark);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }
            
            
            File file = null;
            
            if(getCount() > 0)
                file = (File) getItem(position);
            
            if (file != null) {
                
                holder.chcktxtWatermarkName.setText(file.getName());
               
                mImageWorker.loadImage(file.getAbsolutePath(), holder.image);
                
            }
            return view;
        }
        
        /**
         * A holder class that holds onto view references to make the list view more efficient as views are recycled.
         */
        
        private class ViewHolder {
            public TextView chcktxtWatermarkName;
            public ImageView image;
        }
        
        /**
         * A helper function to both clear and add new file objects to the adapter.
         * @param data
         */
        public void setData(List<File> data) {
            clear();
            if (data != null) {
                addAll(data);
            }
        }
    }

    /**
     * A custom implementation of an AsyncTaskLoader that connects to the appropriate endpoint and checks
     * for new watermarks, downloading them locally.
     * 
     */
    public static class WatermarkLoader extends AsyncTaskLoader<List<File>> {
        
        private static final String WATERMARKS_ENDPOINT = "http://api.barswithfriends.com/images/search?format=json&category=watermarks&group=watermarks";
        
        private File mOutputDirectory;
        
        private Exception mError;
        
        /**
         * Constructor
         * @param context The current context.
         */
        public WatermarkLoader(Context context) {
            super(context);
            mOutputDirectory = new File(Utils.getExternalFileDir(context), IMAGE_DIR);
        }

        /* 
         * {@inheritDoc}
         */
        @Override
        public List<File> loadInBackground() {
            
            mError = null;
            
            // For older devices.
            Utils.disableConnectionReuseIfNecessary();
            
            InputStream inputStream = null;
            ArrayList<File> watermarks = new ArrayList<File>();
            HttpURLConnection urlConnection = null;
            BufferedOutputStream outStream = null;
            
            try {
                
                // Connect to the endpoint.
                
                URL url = new URL(WATERMARKS_ENDPOINT);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setDoInput(true);
                urlConnection.connect();
                int responseCode = urlConnection.getResponseCode();
                
                inputStream = urlConnection.getInputStream();
                
                // Read the response.
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                
                reader.close();
                
                if(BuildConfig.DEBUG) {
                    Log.d(getClass().getName(), "The response is: " + response.toString() + " Code: " + responseCode);
                    
                }
                
                // Parse the response and retrieve our list of image urls.
                ArrayList<String> urls = parseJSON(response.toString());
                
                int b;
                
                for(String imgUrl:urls) {
                    
                    url = new URL(imgUrl);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    
                    String filename =  url.getFile().substring(url.getFile().lastIndexOf("/") + 1);
                    File localCopy = new File(mOutputDirectory, filename);
                    
                    // Since FileOutputStream will automatically create the file if necessary we need to know
                    // beforehand if the file is new so we can add it into our list of existing files.
                    boolean isNew = true;
                    if(localCopy.exists()) {
                        isNew = false;
                    };
                    
                    // We want to overwrite the local files every time in case the image on the server changes
                    inputStream = new BufferedInputStream(urlConnection.getInputStream(), Utils.IO_BUFFER_SIZE);
                    outStream = new BufferedOutputStream(new FileOutputStream(localCopy), Utils.IO_BUFFER_SIZE);

                    b = 0;
                    while ((b = inputStream.read()) != -1) {
                        outStream.write(b);
                    }
                    
                    outStream.close();
                    
                    // Only return files that are new and not already shown from local storage
                    if(isNew) {
                        watermarks.add(localCopy);
                    }
                }
                
                
            } catch (IOException e) {
                mError = e;
                Log.e(getClass().getName(), "Error retrieving watermarks " + e);
             
            } catch (JSONException e) {
                mError = e;
                Log.e(getClass().getName(), "Error retrieving watermarks " + e);
                
             // Makes sure that the InputStream is closed after the app is
             // finished using it.
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        mError = e;
                        Log.e(getClass().getName(), "Error retrieving watermarks " + e);
                    }
                } 
                if (outStream != null) {
                    try {
                        outStream.close();
                    } catch (final IOException e) {
                        mError = e;
                        Log.e(getClass().getName(), "Error retrieving watermarks " + e);
                    }
                }
            }
            
            return watermarks;
        }

        /**
         * A helper method that returns a list of strings that represent the urls of watermarks returned by the server.
         * 
         * @param response The string from the server to be parsed
         * @return The list of strings
         * @throws JSONException If the result cannot be parsed
         */
        private ArrayList<String> parseJSON(String response) throws JSONException {
            
            JSONObject object = new JSONObject(response);
            
            JSONArray imgs = object.getJSONObject("data").getJSONArray("images");
            
            ArrayList<String> urls = new ArrayList<String>();
            
            String url;
            for (int index = 0; index < imgs.length(); index++) {
                url = imgs.getJSONObject(index).getJSONObject("formats").getJSONObject("main").getString("url");
                urls.add(url);
            }
            
            return urls;
        }
        
        /**
         * Returns an error that was caught by the loader in loadInBackground().  Should be checked before displaying results
         * so that any reporting to the user can update the UI on the UI thread. 
         * 
         * @return The Exception that was caught 
         */
        public Exception getError() {
            return mError;
        }
    }
}
