package com.poggled.android.phototagger.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.ActionBar;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import com.poggled.android.phototagger.R;

public class HomeGridActivity extends GoFotoFragmentActivity implements
		ActionBar.TabListener {

	private static final String TAG = "ImageGridFragment";

	// url to make request
	private static String url = "https://api.poggled.com/galleries/search?format=json&ids=5048c4a101e7fe4349000005";

	// JSON Node names
	private static final String TAG_DATA = "data";
	private static final String TAG_GALLERIES = "galleries";
	private static final String TAG_IMAGES = "images";
	private static final String TAG_IMAGE = "image";
	private static final String TAG_FOMATS = "formats";
	private static final String TAG_SMALL_240 = "small_240";
	private static final String TAG_URL = "url";
	public static ArrayList<Bitmap> bitmapArray = new ArrayList<Bitmap>();
	


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super.setActiveTab(0);
		super.createBar();
		
		loadJSONImage();
	

		// Check that the activity is using the layout version with
		// the fragment_container FrameLayout
		// if (findViewById(R.id.files_frag) != null) {
		//
		// // However, if we're being restored from a previous state,
		// // then we don't need to do anything and should return or else
		// // we could end up with overlapping fragments.
		// if (savedInstanceState != null) {
		// return;
		// }
		//
		// // Create an instance of FileListFragment
		// FileListFragment fileListFragment = new FileListFragment();
		//
		// // In case this activity was started with special instructions from
		// an Intent,
		// // pass the Intent's extras to the fragment as arguments
		// fileListFragment.setArguments(getIntent().getExtras());
		//
		// // Add the fragment to the 'fragment_container' FrameLayout
		// getSupportFragmentManager().beginTransaction()
		// .add(R.id.files_frag, fileListFragment).commit();
		// }

		final FragmentTransaction ft = getSupportFragmentManager()
				.beginTransaction();
		if (getSupportFragmentManager().findFragmentByTag(TAG) == null) {
			ft.add(R.id.photos_frag, new HomeGridFragment(), TAG);
		}

		ft.commit();

		setContentView(R.layout.main);
	}

	public void loadJSONImage() {
		// parsing JSON
		// Creating JSON Parser instance
		JSONParser jParser = new JSONParser();

		// getting JSON string from URL
		JSONObject json = jParser.getJSONFromUrl(url);

		try {

			JSONObject data = json.getJSONObject(TAG_DATA);
			// Getting Array of galleries
			JSONArray galleries = data.getJSONArray(TAG_GALLERIES);

			// looping through All Galleries
			for (int i = 0; i < galleries.length(); i++) {
				JSONObject galleriesObject = galleries.getJSONObject(i);

				JSONArray images = galleriesObject.getJSONArray(TAG_IMAGES);
				for (int k = 0; k < images.length(); k++) {

					JSONObject imagesObject = images.getJSONObject(k);
					JSONObject image = imagesObject.getJSONObject(TAG_IMAGE);
					JSONObject format = image.getJSONObject(TAG_FOMATS);
					JSONObject small_240 = format.getJSONObject(TAG_SMALL_240);
					String url2 = small_240.getString(TAG_URL);
					Bitmap bimage = getBitmapFromURL(url2);
					bitmapArray.add(bimage);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public static Bitmap getBitmapFromURL(String src) {
		try {
			Log.e("src", src);
			URL url = new URL(src);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setDoInput(true);
			connection.connect();
			InputStream input = connection.getInputStream();
			Bitmap myBitmap = BitmapFactory.decodeStream(input);
			Log.e("Bitmap", "returned");
			return myBitmap;
		} catch (IOException e) {
			e.printStackTrace();
			Log.e("Exception", e.getMessage());
			return null;
		}
	}
}