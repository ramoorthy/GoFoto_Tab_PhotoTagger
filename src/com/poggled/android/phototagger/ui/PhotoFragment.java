/**
 * PhotoTagger
 * PhotoFragment.java
 * 
 * @author Jason Harris on Jun 19, 2012
 * @copyright 2012 Poggled, Inc. All rights reserved
 * 
 * Typical Usage: Called from file list fragment to display photos 
 * and allow the user to begin tagging.
 * 
 */
package com.poggled.android.phototagger.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.poggled.android.phototagger.R;

public class PhotoFragment extends Fragment {
	// private static final String IMAGE_DATA_EXTRA = "pic";
	private ImageView mImageView;

	// Empty constructor, required as per Fragment docs
	public PhotoFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.photo, container, false);

		mImageView = ((ImageView) v.findViewById(R.id.photo_imageview));

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// String filepath = savedInstanceState.getString("pic");
		if (getArguments() != null) {
			Uri selectedImage = Uri.fromFile(new File(getArguments().getString(
					"pic")));
			// Uri selectedImage = Uri.parse(getArguments().getString("pic"));
			Toast.makeText(getActivity(),
					"selectedImage Uri: " + selectedImage.toString(),
					Toast.LENGTH_LONG).show();

			InputStream imageStream;
			try {
				// FileInputStream in =
				// getActivity().openFileInput(getArguments().getString("pic"));
				// mImageView.setImageBitmap(BitmapFactory.decodeStream(in));
				// mImageView.setImageDrawable(Drawable.createFromStream(
				// getActivity().getContentResolver().openInputStream(selectedImage),
				// null));
				// Uri selectedImage =
				imageStream = getActivity().getContentResolver()
						.openInputStream(selectedImage);
				Bitmap yourSelectedImage = BitmapFactory
						.decodeStream(imageStream);
				mImageView.setImageBitmap(yourSelectedImage);
				imageStream.close();
				imageStream = null;
				// ((ImageView)
				// v.findViewById(R.id.photo_imageview)).invalidate();
				// ((ImageView)
				// v.findViewById(R.id.photo_imageview)).setImageURI(selectedImage);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public String getShownFilePath() {
		return getArguments().getString("pic");
	}

	/**
	 * Create a new instance of PhotoFragment, initialized to show the picture
	 * at 'index'.
	 */
	public static PhotoFragment newInstance(String filepath) {
		PhotoFragment frag = new PhotoFragment();

		// Supply file input as an argument.
		Bundle args = new Bundle();
		args.putString("pic", filepath);
		frag.setArguments(args);

		return frag;
	}
}
