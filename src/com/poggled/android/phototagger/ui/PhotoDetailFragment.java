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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.poggled.android.phototagger.R;
import com.poggled.android.phototagger.util.ImageWorker;

/**
 * This fragment will populate the children of the ViewPager from {@link PhotoDetailActivity}.
 */
public class PhotoDetailFragment extends Fragment {

    private static final String IMAGE_DATA_EXTRA = "resId";
    private int mImageNum;
    private ImageView mImageView;
    private ImageWorker mImageWorker;

    /**
     * Factory method to generate a new instance of the fragment given an image number.
     *
     * @param imageNum The image number within the parent adapter to load
     * @return A new instance of ImageDetailFragment with imageNum extras
     */
    public static PhotoDetailFragment newInstance(int imageNum) {
        final PhotoDetailFragment f = new PhotoDetailFragment();

        final Bundle args = new Bundle();
        args.putInt(IMAGE_DATA_EXTRA, imageNum);
        f.setArguments(args);

        return f;
    }

    /**
     * Empty constructor as per the Fragment documentation
     */
    public PhotoDetailFragment() {}

    /**
     * Populate image number from extra, use the convenience factory method
     * {@link PhotoDetailFragment#newInstance(int)} to create this fragment.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mImageNum = getArguments() != null ? getArguments().getInt(IMAGE_DATA_EXTRA) : -1;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate and locate the main ImageView
        final View v = inflater.inflate(R.layout.photo_detail_fragment, container, false);
        mImageView = (ImageView) v.findViewById(R.id.imageView);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Use the parent activity to load the image asynchronously into the ImageView (so a single
        // cache can be used over all pages in the ViewPager
        if (PhotoDetailActivity.class.isInstance(getActivity())) {
            mImageWorker = ((PhotoDetailActivity) getActivity()).getImageWorker();
            mImageWorker.loadImage(mImageNum, mImageView);
        }

        // Pass clicks on the ImageView to the parent activity to handle
        if (OnClickListener.class.isInstance(getActivity())) {
            mImageView.setOnClickListener((OnClickListener) getActivity());
        }
    }

    /**
     * Cancels the asynchronous work taking place on the ImageView, called by the adapter backing
     * the ViewPager when the child is destroyed.
     */
    public void cancelWork() {
        ImageWorker.cancelWork(mImageView);
        mImageView.setImageDrawable(null);
        mImageView = null;
    }
    
}
