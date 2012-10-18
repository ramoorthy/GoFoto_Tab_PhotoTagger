package com.poggled.android.phototagger.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.poggled.android.phototagger.R;
import com.poggled.android.phototagger.service.ImageProcessingService;

public class ImageServiceControllerActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.image_service_controller);

        // Watch for button clicks.
        Button button = (Button)findViewById(R.id.btnStartImageService);
        button.setOnClickListener(mStartListener);
        button = (Button)findViewById(R.id.btnStopImageService);
        button.setOnClickListener(mStopListener);
    }

    private OnClickListener mStartListener = new OnClickListener() {
        public void onClick(View v) {
            // Make sure the service is started.  It will continue running
            // until someone calls stopService().  The Intent we use to find
            // the service explicitly specifies our service component, because
            // we want it running in our own process and don't want other
            // applications to replace it.
            
            Intent intent = new Intent(ImageServiceControllerActivity.this, ImageProcessingService.class);
            intent.setAction(Intent.ACTION_SYNC);
            startService(intent);
        }
    };

    private OnClickListener mStopListener = new OnClickListener() {
        public void onClick(View v) {   
            // Cancel a previous call to startService().  Note that the
            // service will not actually stop at this point if there are
            // still bound clients.
            stopService(new Intent(ImageServiceControllerActivity.this,
                    ImageProcessingService.class));
        }
    };
}



