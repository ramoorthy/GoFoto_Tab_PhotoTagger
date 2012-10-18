package com.poggled.android.phototagger.ui.widget;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FbDialog;
import com.poggled.android.phototagger.R;

public class AddAccountFbDialog extends FbDialog {
    
    LinearLayout mDialogParentFrame;
    
    public AddAccountFbDialog(Context context, String url, DialogListener listener) {
        super(context, url, listener, R.style.Theme_FbDialog);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //super.onCreate(savedInstanceState);
        
        
        mSpinner = new ProgressDialog(getContext());
        mSpinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mSpinner.setMessage("Loading...");
        mSpinner.setCancelable(true);
        mSpinner.setCanceledOnTouchOutside(true);
        mSpinner.setOnCancelListener(new OnCancelListener() {

            public void onCancel(DialogInterface dialog) {
                AddAccountFbDialog.this.cancel();
            }
            
        });
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.fb_dialog);
        
        mDialogParentFrame = (LinearLayout) findViewById(R.id.dialog_parent_frame);
        mDialogParentFrame.setVisibility(View.INVISIBLE);
        
        mContent = (FrameLayout) findViewById(R.id.fb_content);
        
        /* Create the 'x' image, but don't add to the mContent layout yet
         * at this point, we only need to know its drawable width and height 
         * to place the webview
         */
        createCrossImage();
        
        /* Now we know 'x' drawable width and height, 
         * layout the webivew and add it the mContent layout
         */
        int halfCrossWidth = mCrossImage.getDrawable().getIntrinsicWidth() / 2;
        setUpWebView(halfCrossWidth);
        mWebView.setWebViewClient(new AddAccountFbWebViewClient());
        
        ((LinearLayout.LayoutParams) ((LinearLayout) findViewById(R.id.extra_container)).getLayoutParams()).setMargins(halfCrossWidth , 0, halfCrossWidth , halfCrossWidth);
        
        /* Finally add the 'x' image to the mContent layout and
         * add mContent to the Dialog view
         */
        
        mContent.addView(mCrossImage, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        //addContentView(mContent, new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, 0, 1));
    }
    
    protected class AddAccountFbWebViewClient extends FbWebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            
            mDialogParentFrame.setVisibility(View.VISIBLE);
        }
    }
    
}
