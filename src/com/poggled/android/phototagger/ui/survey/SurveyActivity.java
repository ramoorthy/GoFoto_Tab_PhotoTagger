/**
 * PhotoTagger
 * SurveyActivity.java
 * 
 * @author Eric Swierczek <swierczek@hardindd.com>
 * @author Jay Aniceto <jason.aniceto@poggled.com>
 * @copyright 2012 Poggled, Inc. All rights reserved
 * 
 * For the survey activity.
 * Displays a survey to the user and saves the results to a text file.
 * 
 * Uses layout files in either 'layout' or 'layout-land' depending on orientation.
 * 
 */

package com.poggled.android.phototagger.ui.survey;

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
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.poggled.android.phototagger.R;
import com.poggled.android.phototagger.ui.GoFotoFragmentActivity;
import com.poggled.android.phototagger.ui.prefs.SettingsActivity;

public class SurveyActivity extends GoFotoFragmentActivity {

  //for navigation and clicks
  TextView submit;
  RadioGroup answer1;
  RadioGroup answer2;
  RadioGroup answer3;
  RadioGroup answer4;
  
  //for writing to the file
  File file;
  FileWriter filewriter;
  BufferedWriter out;
  File root;
  
  public Activity activity;
  
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    super.setActiveTab(2);
    super.createBar();
    setContentView(R.layout.survey);
    
    activity = this;
    
    answer1 = (RadioGroup) findViewById(R.id.answer1);
    answer2 = (RadioGroup) findViewById(R.id.answer2);
    answer3 = (RadioGroup) findViewById(R.id.answer3);
    answer4 = (RadioGroup) findViewById(R.id.answer4);
    
    //creates new file or alerts the user if the card can't be written to
    try {
        root = Environment.getExternalStorageDirectory();
        if (root.canWrite()) {
            file = new File(root, "surveys.csv");
            filewriter = new FileWriter(file, true);
        } else {
          Log.d("fileIO", "waaaat");
          Toast.makeText(activity, "can't write", Toast.LENGTH_LONG).show();
        }
    } catch (IOException e) {
        Log.e("fileIO", "Could not write file " + e.getMessage());
    }
    
    submit = (TextView) findViewById(R.id.submit);
    submit.setOnClickListener(new OnClickListener() {
      /**
       * If all questions aren't answered, alert the user.  Otherwise write the
       * data to the file.
       */

      public void onClick(View v) {
        //are all questions answered?
        if (answer1.getCheckedRadioButtonId() == -1 || answer2.getCheckedRadioButtonId() == -1 || answer3.getCheckedRadioButtonId() == -1 || answer4.getCheckedRadioButtonId() == -1) {
          final AlertDialog alertdialog = new AlertDialog.Builder(activity).create();
          alertdialog.setTitle(getText(R.string.survey_hey));
          alertdialog.setMessage(getText(R.string.survey_answer_all));
          alertdialog.setButton("OK", new DialogInterface.OnClickListener() {
            
   
            public void onClick(DialogInterface dialog, int which) {
              alertdialog.dismiss();
            }
            
          });
          alertdialog.show();
        } else {
          //write to the file
          try {
              
              file = new File(root, "surveys.csv");
              filewriter = new FileWriter(file, true);
              out = new BufferedWriter(filewriter);
              
              //write data for question 1 depending on selected radio button
              switch (answer1.getCheckedRadioButtonId()) {
                case(R.id.rad_male):
                  out.write(getText(R.string.survey_male) + ",");
                  break;
                case(R.id.rad_female):
                  out.write(getText(R.string.survey_female) + ",");
                  break;
              }

              //write data for question 2 depending on selected radio button
              switch (answer2.getCheckedRadioButtonId()) {
                case(R.id.rad_21):
                  out.write(getText(R.string.survey_age1) + ",");
                  break;
                case(R.id.rad_26):
                  out.write(getText(R.string.survey_age2) + ",");
                  break;
                case(R.id.rad_31):
                  out.write(getText(R.string.survey_age3) + ",");
                  break;
                case(R.id.rad_40):
                  out.write(getText(R.string.survey_age4) + ",");
                  break;
              }
              
              //write data for question 3 depending on selected radio button
              switch (answer3.getCheckedRadioButtonId()) {
                case(R.id.rad_enjoy_1):
                  out.write(getText(R.string.survey_scale1) + ",");
                  break;
                case(R.id.rad_enjoy_2):
                  out.write(getText(R.string.survey_scale2) + ",");
                  break;
                case(R.id.rad_enjoy_3):
                  out.write(getText(R.string.survey_scale3) + ",");
                  break;
                case(R.id.rad_enjoy_4):
                  out.write(getText(R.string.survey_scale4) + ",");
                  break;
                case(R.id.rad_enjoy_5):
                  out.write(getText(R.string.survey_scale5) + ",");
                  break;
              }
              
              //write data for question 4 depending on selected radio button
              switch (answer4.getCheckedRadioButtonId()) {
                case(R.id.rad_buy_1):
                  out.write(getText(R.string.survey_scale1) + ",");
                  break;
                case(R.id.rad_buy_2):
                  out.write(getText(R.string.survey_scale2) + ",");
                  break;
                case(R.id.rad_buy_3):
                  out.write(getText(R.string.survey_scale3) + ",");
                  break;
                case(R.id.rad_buy_4):
                  out.write(getText(R.string.survey_scale4) + ",");
                  break;
                case(R.id.rad_buy_5):
                  out.write(getText(R.string.survey_scale5) + ",");
                  break;
              }
              
              //prepare writer for more input
              out.write("\n");
              out.flush();
              
              //reset survey
              answer1.clearCheck();
              answer2.clearCheck();
              answer3.clearCheck();
              answer4.clearCheck();
              
              //alert that file was written successfully
              final AlertDialog ad = new AlertDialog.Builder(activity).create();
              ad.setTitle(getText(R.string.survey_thanks));
              ad.setMessage(getText(R.string.survey_message));
              ad.setButton("OK", new DialogInterface.OnClickListener() {
        
                public void onClick(DialogInterface dialog, int which) {
                  ad.dismiss();
                }
              });
              ad.show();
          } catch (IOException e) {
              Log.e("fileIO", "Could not write file " + e.getMessage());
          }
        }
      }
    });
    

  }
  
  /**
   * Adds the menu to the screen
   * 
   * @param Menu menu
   * @return boolean
   */
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.survey_menu, menu);
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
