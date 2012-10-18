package com.poggled.android.phototagger.ui.email;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.poggled.android.phototagger.R;
import com.poggled.android.phototagger.ui.HomeGridActivity;
import com.poggled.android.phototagger.ui.prefs.SettingsActivity;
import com.poggled.android.phototagger.ui.survey.SurveyActivity;

public class EmailFragment extends Fragment {

  TextView submit;
  EditText email;
  Context activity;
  
  File file;
  FileWriter filewriter;
  BufferedWriter out;
  File root;
  private GestureDetector gesture;
  //private View.OnTouchListener gestureListener;
  boolean click1 = false;
  boolean click2 = false;
  long first = 0;
  long second = 0;
  
  private EmailValidator emailValidator;
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.email, container, false);
    return v;
//    
//    emailValidator = new EmailValidator();
//    email = (EditText) v.findViewById(R.id.email);
//    activity = (Activity) this;
//    
//    try {
//      root = Environment.getExternalStorageDirectory();
//      if (root.canWrite()) {
//          file = new File(root, "emails.csv");
//          filewriter = new FileWriter(file, true);
//      } else {
//        Toast.makeText(activity, "can't write", Toast.LENGTH_LONG).show();
//      }
//    } catch (IOException e) {
//        Log.e("fileIO", "Could not write file " + e.getMessage());
//    }
//    
//    submit = (TextView) v.findViewById(R.id.submit);
//    submit.setOnClickListener(new OnClickListener() {
//      @Override
//      public void onClick(View v) {
//        String address = email.getText().toString();
//        if (emailValidator.validate(address)) {
//          try {
//            out = new BufferedWriter(filewriter);
//            out.write(address + ",\n");
//            out.flush();
//            email.setText("");
//
//            final AlertDialog ad = new AlertDialog.Builder(activity).create();
//            ad.setTitle(getText(R.string.email_thank_you));
//            ad.setMessage(getText(R.string.email_subscribe_message));
//            ad.setButton("OK", new DialogInterface.OnClickListener() {
//              @Override
//              public void onClick(DialogInterface dialog, int which) {
//                ad.dismiss();
//              }
//            });
//            ad.show();
//          } catch (IOException e) {
//              Log.e("fileIO", "Could not write file " + e.getMessage());
//          }
//        } else {
//          final AlertDialog alertdialog = new AlertDialog.Builder(activity).create();
//          alertdialog.setTitle(getText(R.string.email_invalid_email));
//          alertdialog.setMessage(getText(R.string.email_enter_email));
//          alertdialog.setButton("OK", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//              alertdialog.dismiss();
//            }
//          });
//          alertdialog.show();
//        }
//      }
//    });
//    
//    return v;
  }
  
}
