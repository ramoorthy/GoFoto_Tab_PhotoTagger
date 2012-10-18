/**
 * PhotoTagger
 * FrameFragment.java
 * 
 * @author Jason Harris on Sept 11, 2012
 * @copyright 2012 Poggled, Inc. All rights reserved
 * 
 * Typical Usage: Used to select images to display as the sponsor's watermark
 * 
 */

package com.poggled.android.phototagger.ui.prefs;

import android.content.Context;
import android.content.SharedPreferences;
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
import com.poggled.android.phototagger.ui.prefs.FrameFragment.Frame;
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
import java.util.Iterator;
import java.util.List;

/**
 *  A Fragment for displaying and selecting frames for use in the Image Processing Service. 
 */
public class FrameFragment extends ListFragment implements LoaderManager.LoaderCallbacks<List<Frame>> {
    
    private static final String IMAGE_DIR = "frames";
    
    private FrameArrayAdapter mAdapter;
    
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
        
        //ArrayList<File> imgList = getLocalWatermarks();
        
        mAdapter = new FrameArrayAdapter(this.getActivity(), android.R.id.text1, new ArrayList<Frame>()); 
        setListAdapter(mAdapter);
        
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        
        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        getActivity().getSupportLoaderManager().initLoader(0, null, this).forceLoad();
        setEmptyText(getString(R.string.frame_loading_text));
        
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
        
        final Frame frame = (Frame) l.getItemAtPosition(position);
        final String landscapeFilename = frame.landscapeImage.getAbsolutePath();
        final String portraitFilename = frame.portraitImage.getAbsolutePath();
        
        // Save the frame info into persistent storage
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.pref_frame_key), frame.name);
        editor.putString(getString(R.string.pref_frame_landscape_image_key), landscapeFilename);
        editor.putString(getString(R.string.pref_frame_portrait_image_key), portraitFilename);
        editor.commit();
        
        // Close the activity
        getActivity().finish();
    }
    
    /* 
     * {@inheritDoc}
     */

    public Loader<List<Frame>> onCreateLoader(int id, Bundle args) {
        return new FrameLoader(getActivity());
    }

    /* 
     * {@inheritDoc}
     */

    public void onLoaderReset(Loader<List<Frame>> loader) {
        
        // Reset loading state only if activity available
        if(getActivity() != null && !getActivity().isFinishing()) {
        
            setEmptyText(getString(R.string.frame_loading_text));
            getActivity().setProgressBarIndeterminateVisibility(true);
        }
        
        // Clear the adapter to avoid duplicates
        mAdapter.setData(null);
        
    }
    

    
    /* 
     * {@inheritDoc}
     */

    public void onLoadFinished(Loader<List<Frame>> loader, List<Frame> data) {
        
        setEmptyText(getString(R.string.frame_empty_text));
        getActivity().setProgressBarIndeterminateVisibility(false); 
        
        // Notify the user of problems encountered.
        final Exception error = ((FrameLoader) loader).getError();
        
        if(error != null ) {
            Toast.makeText(getActivity(), getString(R.string.frame_error_text), Toast.LENGTH_LONG).show();
        } 
        
        mAdapter.clear();
        
        // Search the frame list for our selected frame, set it as checked if found or add it
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final String name = prefs.getString(getString(R.string.pref_frame_key), null);
        
        boolean found = false;
        
        // if name is null we don't have a selected frame and nothing to check or add
        if(name !=  null) {
            for(int i = 0; i < data.size(); i++) {
                if(data.get(i).name.equals(name)) {
                    getListView().setItemChecked(i, true);
                    found = true;
                } 
            }
            if(!found) {
                final String landscapeFilename = prefs.getString(getString(R.string.pref_frame_landscape_image_key), null);
                final String portraitFilename = prefs.getString(getString(R.string.pref_frame_portrait_image_key), null);
                if(landscapeFilename != null && portraitFilename != null) {
                    Frame selected = new Frame(name);
                    selected.landscapeImage = new File(landscapeFilename);
                    selected.portraitImage = new File(portraitFilename);
                    mAdapter.add(selected);
                }
            }
        }
        
        // Set the new data in the adapter. 
        mAdapter.addAll(data);
        
    }

    /**
     * An Adapter that is backed by an array of frames.  The class takes care of image loading with the use of an image worker
     * off of the UI thread.  This class returns custom views and most of the implementation for adding files is the default
     * implementation from the parent class.
     * 
     */
    private static class FrameArrayAdapter extends ArrayAdapter<Frame> {
        
        private static final String IMAGE_CACHE_DIR = "watermarks";
        
        private ImageLoader mImageWorker;
        
        /**
         * The main constructor for the frame array adapter implementation.
         * 
         * @param context The current context 
         * @param textViewResourceId The textview resource id to be set in the parent class
         * @param objects The objects to represent in the ListView.
         */
        public FrameArrayAdapter(Context context, int textViewResourceId, List<Frame> objects) {
            
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
                holder.txtFrameName = (TextView) view.findViewById(android.R.id.text1);
                holder.image = (ImageView) view.findViewById(R.id.imgWatermark);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }
            
            
            Frame frame = null;
            
            if(getCount() > 0)
                frame = (Frame) getItem(position);
            
            if (frame != null) {
                
                holder.txtFrameName.setText(frame.name);
               
                mImageWorker.loadImage(frame.landscapeImage, holder.image);
                
            }
            return view;
        }
        
        /**
         * A holder class that holds onto view references to make the list view more efficient as views are recycled.
         */
        private class ViewHolder {
            public TextView txtFrameName;
            public ImageView image;
        }
        
        /**
         * A helper function to both clear and add new frame objects to the adapter.
         * @param data
         */
        public void setData(List<Frame> data) {
            clear();
            if (data != null) {
                addAll(data);
            }
        }
    }

    /**
     * A custom implementation of an AsyncTaskLoader that connects to the appropriate endpoint and checks
     * for frames, downloading them locally.
     * 
     */
    public static class FrameLoader extends AsyncTaskLoader<List<Frame>> {
        
        private static final String FRAMES_ENDPOINT = "http://api.barswithfriends.com/images/search?format=json&category=frames&group=frames";
        
        private File mOutputDirectory;
        
        private Exception mError;
        
        private String mSelectedLandscapeFilename;
        private String mSelectedPortraitFilename;
        
        /**
         * Constructor
         * @param context The current context.
         */
        public FrameLoader(Context context) {
            super(context);
            mOutputDirectory = new File(Utils.getExternalFileDir(context), IMAGE_DIR);
            
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            mSelectedLandscapeFilename = prefs.getString(context.getString(R.string.pref_frame_landscape_image_key), null);
            mSelectedPortraitFilename = prefs.getString(context.getString(R.string.pref_frame_portrait_image_key), null);
        }

        /* 
         * {@inheritDoc}
         */
        @Override
        public List<Frame> loadInBackground() {
            
            mError = null;
            
            if(!mOutputDirectory.exists()) mOutputDirectory.mkdirs();
            
            // delete all local frames files except for our currently selected images
            if (mOutputDirectory.isDirectory()) {
                String[] children = mOutputDirectory.list();
                
                for (int i = 0; i < children.length; i++) {
                    File child = new File(mOutputDirectory, children[i]);
                    final String childPath = child.getAbsolutePath();
                    if(!childPath.equals(mSelectedLandscapeFilename) && !childPath.equals(mSelectedPortraitFilename)) {
                        child.delete();
                    }
                }
            }
            
            // For older devices.
            Utils.disableConnectionReuseIfNecessary();
            
            InputStream inputStream = null;
            ArrayList<Frame> returnedFrames = new ArrayList<Frame>();
            HttpURLConnection urlConnection = null;
            
            try {
                
                // Connect to the endpoint.
                URL url = new URL(FRAMES_ENDPOINT);
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
                
                // Parse the response and retrieve our frames with portrait and landscape urls.
                ArrayList<Frame> frames = parseJSON(response.toString());
                
                for(Frame frame:frames) {
                    
                    frame.landscapeImage = downloadFrame(frame.landscapeUrl);
                    frame.portraitImage = downloadFrame(frame.portraitUrl);
                    
                    if(frame.landscapeImage != null && frame.portraitImage != null 
                            && frame.landscapeImage.exists() && frame.portraitImage.exists())
                        returnedFrames.add(frame);
                }
                
                
            } catch (IOException e) {
                mError = e;
                if(BuildConfig.DEBUG) {
                    Log.e(getClass().getName(), "Error retrieving watermarks " + e);
                }
             
            } catch (JSONException e) {
                mError = e;
                if(BuildConfig.DEBUG) {
                    Log.e(getClass().getName(), "Error retrieving frames " + e);
                }
                
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
                        if(BuildConfig.DEBUG) {
                            Log.e(getClass().getName(), "Error retrieving frames " + e);
                        }
                    }
                } 
            }
            
            return returnedFrames;
        }

        
        /**
         * Helper method to download a frame given a url.
         * 
         * @param imgUrl the url of the image to download
         * @return a File object representing the file on the local file system
         * @throws IOException in case of connection errors
         */
        private File downloadFrame(String imgUrl) throws IOException {
            
            HttpURLConnection urlConnection = null;
            InputStream inputStream = null;
            BufferedOutputStream outStream = null;
            File localCopy = null;
            
            try {
                URL url = new URL(imgUrl);
                urlConnection = (HttpURLConnection) url.openConnection();
                
                String filename =  url.getFile().substring(url.getFile().lastIndexOf("/") + 1);
                if(filename == null) return null;
                localCopy = new File(mOutputDirectory, filename);
                
                // We want to overwrite the local files every time in case the image on the server changes
                inputStream = new BufferedInputStream(urlConnection.getInputStream(), Utils.IO_BUFFER_SIZE);
                outStream = new BufferedOutputStream(new FileOutputStream(localCopy), Utils.IO_BUFFER_SIZE);
    
                int b = 0;
                while ((b = inputStream.read()) != -1) {
                    outStream.write(b);
                }
                
                outStream.close();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        Log.e(getClass().getName(), "Error retrieving frames " + e);
                    }
                } 
                if (outStream != null) {
                    try {
                        outStream.close();
                    } catch (IOException e) {
                        Log.e(getClass().getName(), "Error retrieving frames " + e);
                    }
                } 
            }
            
            return localCopy;
        }
        /**
         * A helper method that returns a list of strings that represent the urls of frames returned by the server.
         * 
         * @param response The string from the server to be parsed
         * @return The list of strings
         * @throws JSONException If the result cannot be parsed
         */
        private ArrayList<Frame> parseJSON(String response) throws JSONException {
            
            JSONObject object = new JSONObject(response);
            
            JSONObject imgs = object.getJSONObject("data").getJSONObject("images");
            
            ArrayList<Frame> frames = new ArrayList<Frame>();
            
            
            Iterator<?> iterator = imgs.keys();
            while(iterator.hasNext()) {
                String name = (String) iterator.next();
                JSONArray types = imgs.getJSONArray(name);
                Frame frame = new Frame(name);
                
                for (int index = 0; index < types.length(); index++) {
                    JSONObject type = types.getJSONObject(index);
                    String imgClass = type.getString("class");
                    if(imgClass.equalsIgnoreCase("landscape")) {
                        frame.landscapeUrl = type.getJSONObject("formats").getJSONObject("main").getString("url");
                    }
                    if (imgClass.equals("portrait")) {
                        frame.portraitUrl = type.getJSONObject("formats").getJSONObject("main").getString("url");
                    }
                }
                
                if(frame.portraitUrl != null && frame.landscapeUrl != null 
                        && frame.portraitUrl.length() > 0 && frame.landscapeUrl.length() > 0) {
                    frames.add(frame);
                }
            }
            
            return frames;
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
    
    /**
     * A simple data model for frames from the server.
     */
    public static class Frame {
        public String name;
        public String landscapeUrl;
        public String portraitUrl;
        public File landscapeImage;
        public File portraitImage;
        
        public Frame(String name) {
            this.name = name;
        }
        
        public String toString() {
            return this.name;
        }
    }
    
}
