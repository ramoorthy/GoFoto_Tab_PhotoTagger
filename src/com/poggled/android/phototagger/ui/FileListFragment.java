/**
 * PhotoTagger
 * FileListFragment.java
 * 
 * @author Jason Harris on Jun 19, 2012
 * @copyright 2012 Poggled, Inc. All rights reserved
 * 
 * Typical Usage: Used to select files to display in the photo's fragment
 * 
 */
package com.poggled.android.phototagger.ui;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.poggled.android.phototagger.R;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

public class FileListFragment extends ListFragment {

	// Stores names of traversed directories
	private ArrayList<String> mDirectories = new ArrayList<String>();

	// Check if the first level of the directory structure is the one showing
	private Boolean atTopLevel = false;

	private Item[] mFiles;
	private File path = new File(Environment.getExternalStorageDirectory() + "/Eye-Fi");

	private ListAdapter mAdapter;

	/* (non-Javadoc)
	 * @see android.app.ListFragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.file_list, null);
		return view;
	}

	/* (non-Javadoc)
	 * @see android.app.Fragment#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(path.exists()) {
		    mDirectories.add("Eye-Fi");
		} else {
	        Toast.makeText(
	                      getActivity(), 
	                      "Eye-Fi directory not found.  You may need to find the directory manually.", 
	                      Toast.LENGTH_LONG).show();
	        atTopLevel = true;
	        path = new File(Environment.getExternalStorageDirectory() + "");
		}
		
		getFileList();
		setListAdapter(mAdapter);
		
	}

	/* (non-Javadoc)
	 * @see android.app.Fragment#onPause()
	 */
	@Override
	public void onPause() {

		super.onPause();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		
		if (mFiles == null) {
			Log.e(getClass().getName(), "No files loaded");
			return;
		}
		
		final String chosenFile = mFiles[position].file;
		File sel = new File(path + File.separator + chosenFile);
		if (sel.isDirectory()) {
			atTopLevel = false;

			// Adds chosen directory to list
			mDirectories.add(chosenFile);
			mFiles = null;
			path = new File(sel + "");

			getFileList();

			setListAdapter(mAdapter);

		}

		// Checks if 'up' was clicked
		else if (chosenFile.equalsIgnoreCase("up") && !sel.exists()) {

			// present directory removed from list
		    String s = new String();
		    if(mDirectories.size() > 0) {
		        s = mDirectories.remove(mDirectories.size() - 1);
		    }
		    
			// path modified to exclude present directory
			path = new File(path.toString().substring(0,
					path.toString().lastIndexOf(s)));
			mFiles = null;
			
			
			// if there are no more directories in the list, then
			// its the first level
			if (mDirectories.isEmpty()) {
				atTopLevel = true;
			}
			getFileList();

			setListAdapter(mAdapter);

		}
		// File picked
		else {
			// Perform action with file picked
		    
//TODO: find a home for this code		    
		    //this code may be useful for selecting an image overlay to apply to a picture
//		    Intent i = new Intent(Intent.ACTION_PICK,
//		               android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//		    startActivityForResult(i, 3);
		    
		    Uri selectedUri = Uri.fromFile(sel);
		     String fileExtension
		      = MimeTypeMap.getFileExtensionFromUrl(selectedUri.toString());
		     String mimeType
		      = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
		     
//		     Toast.makeText(getActivity(),
//		       "FileExtension: " + fileExtension + "\n" +
//		       "MimeType: " + mimeType,
//		       Toast.LENGTH_LONG).show();
		     
		     //Start Activity to view the selected file
		     Intent intent;
		     intent = new Intent(Intent.ACTION_VIEW);
		     
		     if(mimeType != null && !mimeType.trim().isEmpty()) {
		         if(mimeType.startsWith("image/")) {
		             //update photo fragment with selected image
		             getListView().setItemChecked(position, true);

		             // Check what fragment is currently shown, replace if needed.
		             PhotoFragment photoFragment = (PhotoFragment)
		                     getActivity().getSupportFragmentManager().findFragmentById(R.id.photos_frag);
		             if (photoFragment == null || !sel.getAbsolutePath().equals(photoFragment.getShownFilePath())) {
		                 Toast.makeText(
		                          getActivity(), 
		                          "path = " + sel.getAbsolutePath(), 
		                          Toast.LENGTH_LONG).show();
		                 Log.d(getClass().getName(), "test");
    		             photoFragment = PhotoFragment.newInstance(sel.getAbsolutePath());
    
    		             // Execute a transaction, replacing any existing fragment
    		             // with this one inside the frame.
    		             FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
    		             ft.replace(R.id.photos_frag, photoFragment);
    		             ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
    		             ft.commit();
		             }
		             return;
		         } else {
		             //if its not a photo but we have the mimeType we can likely resolve the activity to open more easily
		             intent.setDataAndType(selectedUri, mimeType);
		         }
		     } else {
		         //attempt opening the file without a mimeType
		         intent.setData(selectedUri);
		     }
		     try {
	            startActivity(intent);
	        } catch (android.content.ActivityNotFoundException e) {
	            //catch and handle files that can't be resolved to an activity.
	            Toast.makeText(getActivity(), "No application found to open this file.  You can try opening the file by selecting an application...", Toast.LENGTH_SHORT).show();
	            Intent newIntent = Intent.createChooser(intent, "Select an application to open this file:");
	            startActivity(newIntent);
	            
	        }
		}

	}

	private void getFileList() {
//		try {
//			path.mkdirs();
//		} catch (SecurityException e) {
//			Log.e(getClass().getName(), "unable to write on the sd card");
//		}

		// Checks whether path exists
		if (path.exists()) {
			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String filename) {
					File sel = new File(dir, filename);
					// Filters based on whether the file is hidden or not
					return (sel.isFile() || sel.isDirectory())
							&& !sel.isHidden();

				}
			};

			String[] fList = path.list(filter);
			mFiles = new Item[fList.length];
			for (int i = 0; i < fList.length; i++) {
				mFiles[i] = new Item(fList[i], R.drawable.file_icon);

				// Convert into file path
				File sel = new File(path, fList[i]);

				// Set drawables
				if (sel.isDirectory()) {
					mFiles[i].icon = R.drawable.ic_menu_archive;
					Log.d("DIRECTORY", mFiles[i].file);
				} else {
					Log.d("FILE", mFiles[i].file);
				}
			}

			if (!atTopLevel) {
				Item temp[] = new Item[mFiles.length + 1];
				for (int i = 0; i < mFiles.length; i++) {
					temp[i + 1] = mFiles[i];
				}
				temp[0] = new Item("Up", R.drawable.directory_up);
				mFiles = temp;
				
				
			}
			
			mAdapter = new ArrayAdapter<Item>(this.getActivity(),
			        android.R.layout.simple_list_item_activated_1, mFiles) {
				@Override
				public View getView(int position, View convertView, ViewGroup parent) {
					// creates view
					View view = super.getView(position, convertView, parent);
					TextView textView = (TextView) view
							.findViewById(android.R.id.text1);

					// put the image on the text view
					textView.setCompoundDrawablesWithIntrinsicBounds(
							mFiles[position].icon, 0, 0, 0);

					// add margin between image and text (support various screen
					// densities)
					int dp5 = (int) (5 * getResources().getDisplayMetrics().density + 0.5f);
					textView.setCompoundDrawablePadding(dp5);

					return view;
				}
			};
		} else {
		    mAdapter = null;
			Log.e(getClass().getName(), "Path not found.");
		}

		

	}

	private class Item {
		public String file;
		public int icon;

		public Item(String file, Integer icon) {
			this.file = file;
			this.icon = icon;
		}

		@Override
		public String toString() {
			return file;
		}
	}

}
