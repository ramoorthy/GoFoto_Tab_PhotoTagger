package com.poggled.android.phototagger.ui;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.poggled.android.phototagger.R;

//import id.flwi.example.grid2gallery.R;

/**
 * hardcoded image adapter to showcase the use of grid and gallery project
 */
public class ImageAdapter extends BaseAdapter {

	private static LayoutInflater inflater = null;
	private Activity activity;

	private String mode = "";

	int[] images = { R.drawable.h1, R.drawable.h2, R.drawable.h3,
			R.drawable.h4, R.drawable.h5, R.drawable.h6, R.drawable.h7,
			R.drawable.h8, R.drawable.h9, R.drawable.h10, R.drawable.h11,
			R.drawable.h12, R.drawable.h13, R.drawable.h14, R.drawable.h15,
			R.drawable.h16, R.drawable.h17, R.drawable.h18, R.drawable.h19,
			R.drawable.h20, R.drawable.h21, R.drawable.h22, R.drawable.h23,
			R.drawable.h24, R.drawable.h25, R.drawable.h26, R.drawable.h27,
			R.drawable.h28, R.drawable.h29, R.drawable.h30, R.drawable.h31,
			R.drawable.h32, R.drawable.h33, R.drawable.h34, R.drawable.h35,
			R.drawable.h36, R.drawable.h37, R.drawable.h38, R.drawable.h39,
			R.drawable.h40, R.drawable.h41, R.drawable.h42, R.drawable.h43, };

	public ImageAdapter(Activity act, String mode) {
		inflater = (LayoutInflater) act
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		activity = act;
		this.mode = mode;
	}

	// @Override
	public int getCount() {
		return images.length;
	}

	// @Override
	public Object getItem(int position) {
		return new Integer(images[position]);
	}

	// @Override
	public long getItemId(int position) {
		return position;
	}

	// @Override
	public View getView(int position, View view, ViewGroup parent) {
		if (mode.equalsIgnoreCase("grid")) {
			if (view == null) {
				view = inflater.inflate(R.layout.each_image, null);
			}
			ImageView iv = (ImageView) view.findViewById(R.id.imageView);
			iv.setImageResource(images[position]);
		} else if (mode.equalsIgnoreCase("gallery")) {
			if (view == null) {
				view = inflater.inflate(R.layout.each_image_gallery, null);
			}
			ImageView iv = (ImageView) view.findViewById(R.id.imageView);
			iv.setImageResource(images[position]);
		}
		return view;
	}

}
