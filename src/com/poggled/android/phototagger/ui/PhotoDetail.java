package com.poggled.android.phototagger.ui;

import com.poggled.android.phototagger.R;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;

public class PhotoDetail extends Activity implements OnClickListener {

	private ImageButton btnPrevious;
	private ImageButton btnNext;
	private ImageButton btnBacktoGallery;
	private ImageView imgDetail;
	private int position;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.photo_detail_activity2);
		btnNext = (ImageButton) findViewById(R.id.btnNext);
		btnPrevious = (ImageButton) findViewById(R.id.btnPrevious);
		btnBacktoGallery = (ImageButton) findViewById(R.id.btnBackGallery);
		imgDetail = (ImageView) findViewById(R.id.imgDetail);

		btnNext.setOnClickListener(this);
		btnPrevious.setOnClickListener(this);
		btnBacktoGallery.setOnClickListener(this);

		Bundle extras = getIntent().getExtras();
		position = extras.getInt(HomeGridFragment.POSITION);
		// element = extras.getInt(HomeGridFragment.ELEMENTS);

		// for(int i = 0; i < element; i++)
		// {
		// imgArray.add(extras.getString("stringValue" + i));
		// }

		// imgArray.add(extras.getString("stringValue"));

		imgDetail.setImageBitmap(HomeGridActivity.bitmapArray.get(position));
	}

	public void onClick(View v) {
		int id = v.getId();
		switch (id) {
		case R.id.btnNext:
			if (position == HomeGridActivity.bitmapArray.size() - 1) {
				position = 0;
			} else {
				position = position + 1;
			}
			imgDetail
					.setImageBitmap(HomeGridActivity.bitmapArray.get(position));

			break;

		case R.id.btnPrevious:
			if (position == 0) {
				position = HomeGridActivity.bitmapArray.size() - 1;
			} else {
				position = position - 1;
			}
			imgDetail
					.setImageBitmap(HomeGridActivity.bitmapArray.get(position));
			break;
		case R.id.btnBackGallery:
			finish();
			break;

		default:
			break;
		}

	}
}
