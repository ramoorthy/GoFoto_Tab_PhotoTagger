/**
 * PhotoTagger
 * EmailActivity.java
 * 
 * @author Eric Swierczek <swierczek@hardindd.com>
 * @author Jay Aniceto <jason.aniceto@poggled.com>
 * @copyright 2012 Poggled, Inc. All rights reserved
 * 
 * For the e-mail collection activity
 * Displays a subscribe text box for users to enter a valid email address
 * and saves the results to a text file.
 * 
 * Uses layout files in either 'layout' or 'layout-land' depending on orientation.
 */

package com.poggled.android.phototagger.ui.email;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.poggled.android.phototagger.R;
import com.poggled.android.phototagger.ui.GoFotoFragmentActivity;
import com.poggled.android.phototagger.ui.prefs.SettingsActivity;


public class EmailActivity extends GoFotoFragmentActivity {

  //for navigation and clicks
  TextView submit;
  EditText email;
  Activity activity;
  
  //for writing to the file
  File file;
  FileWriter filewriter;
  BufferedWriter out;
  File root;
  
  //email must be in a valid format
  private EmailValidator emailValidator;
  
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.email);
    super.setActiveTab(1);
    super.createBar();
    
    //to verify a valid email address is input
    emailValidator = new EmailValidator();
    email = (EditText) findViewById(R.id.email);
    activity = this;
    
    //creates new file or alerts the user if the card can't be written to
    try {
      root = Environment.getExternalStorageDirectory();
      if (root.canWrite()) {
        file = new File(root, "emails.csv");
        filewriter = new FileWriter(file, true);
      } else {
        Toast.makeText(activity, "can't write", Toast.LENGTH_LONG).show();
      }
    } catch (IOException e) {
      Log.e("fileIO", "Could not write file " + e.getMessage());
    }
    
    submit = (TextView) findViewById(R.id.submit);
    submit.setOnClickListener(new OnClickListener() {
      /**
       * If the email address isn't valid, alert the user.  Otherwise write the
       * data to the file.
       */
 
      public void onClick(View v) {
        String address = email.getText().toString();
        //makes sure the email is valid
        if (emailValidator.validate(address)) {
          try {
            file = new File(root, "emails.csv");
            filewriter = new FileWriter(file, true);
            out = new BufferedWriter(filewriter);
            out.write(address + ",\n");
            out.flush();
            email.setText("");
            //alert that file was written successfully
            final AlertDialog ad = new AlertDialog.Builder(activity).create();
            ad.setTitle(getText(R.string.email_thank_you));
            ad.setMessage(getText(R.string.email_subscribe_message));
            ad.setButton("OK", new DialogInterface.OnClickListener() {
    
              public void onClick(DialogInterface dialog, int which) {
                ad.dismiss();
              }
            });
            ad.show();
          } catch (IOException e) {
            Log.e("fileIO", "Could not write file " + e.getMessage());
          }
        } else {
          //alert user to input valid email address
          final AlertDialog alertdialog = new AlertDialog.Builder(activity).create();
          alertdialog.setTitle(getText(R.string.email_invalid_email));
          alertdialog.setMessage(getText(R.string.email_enter_email));
          alertdialog.setButton("OK", new DialogInterface.OnClickListener() {
  
            public void onClick(DialogInterface dialog, int which) {
              alertdialog.dismiss();
            }
          });
          alertdialog.show();
        }
      }
    });
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.email_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.settings:
      Intent i = new Intent(this, SettingsActivity.class);
      startActivity(i);
      return true;
    case R.id.export:
      // Email the data
      final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
      emailIntent.setType("plain/text");
      emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.export_subject, Calendar.getInstance()));
      ArrayList<Uri> uris = new ArrayList<Uri>(); //to hold attachments
      File root = Environment.getExternalStorageDirectory(); //root filepath
      //get emails.csv file
      File fileIn = new File(root, "emails.csv");
      Uri u;
      if(fileIn.exists()) {
          u = Uri.fromFile(fileIn);
          uris.add(u);
      }
      //get surveys.csv file
      fileIn = new File(root, "surveys.csv");
      if(fileIn.exists()) {
          u = Uri.fromFile(fileIn);
          uris.add(u);
      }
      emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
      startActivity(Intent.createChooser(emailIntent, "Send mail..."));
      return true;
    case R.id.clear:
      // Clear the data
      AlertDialog.Builder ad = new AlertDialog.Builder(activity);
      ad.setTitle(getText(R.string.clear_clear));
      ad.setMessage(getText(R.string.clear_are_you_sure));
      ad.setPositiveButton("OK", new DialogInterface.OnClickListener() {
  
        public void onClick(DialogInterface dialog, int which) {
          //clear .csv files
          File root = Environment.getExternalStorageDirectory();
          File clear = new File(root, "emails.csv");
          clear.delete();
          clear = new File(root, "surveys.csv");
          clear.delete();
        }
      });
      ad.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int which) {
          dialog.cancel();
        }
      });
      AlertDialog alertDialog = ad.create();
      alertDialog.show();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }


}
