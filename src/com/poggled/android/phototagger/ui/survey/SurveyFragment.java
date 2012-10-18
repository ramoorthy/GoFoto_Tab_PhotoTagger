package com.poggled.android.phototagger.ui.survey;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.poggled.android.phototagger.R;

public class SurveyFragment extends Fragment {

  TextView submit;
  RadioGroup answer1;
  RadioGroup answer2;
  RadioGroup answer3;
  RadioGroup answer4;
  
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
  
  public Activity activity;
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.survey, container, false);
    return v;
  }
  
//  @Override
//  protected void onCreate(Bundle savedInstanceState) {
//    super.onCreate(savedInstanceState);
//    setContentView(R.layout.survey);
//  
//    activity = this;
//    
//    answer1 = (RadioGroup) findViewById(R.id.answer1);
//    answer2 = (RadioGroup) findViewById(R.id.answer2);
//    answer3 = (RadioGroup) findViewById(R.id.answer3);
//    answer4 = (RadioGroup) findViewById(R.id.answer4);
//    
//    try {
//        root = Environment.getExternalStorageDirectory();
//        if (root.canWrite()) {
//            file = new File(root, "surveys.csv");
//            filewriter = new FileWriter(file, true);
//        } else {
//          Toast.makeText(activity, "can't write", Toast.LENGTH_LONG).show();
//        }
//    } catch (IOException e) {
//        Log.e("fileIO", "Could not write file " + e.getMessage());
//    }
//    
//    submit = (TextView) findViewById(R.id.submit);
//    submit.setOnClickListener(new OnClickListener() {
//      
//      @Override
//      public void onClick(View v) {
//        if (answer1.getCheckedRadioButtonId() == -1 || answer2.getCheckedRadioButtonId() == -1 || answer3.getCheckedRadioButtonId() == -1 || answer4.getCheckedRadioButtonId() == -1) {
//          final AlertDialog alertdialog = new AlertDialog.Builder(activity).create();
//          alertdialog.setTitle(getText(R.string.survey_hey));
//          alertdialog.setMessage(getText(R.string.survey_answer_all));
//          alertdialog.setButton("OK", new DialogInterface.OnClickListener() {
//            
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//              alertdialog.dismiss();
//            }
//            
//          });
//          alertdialog.show();
//        } else {
//          try {
//              out = new BufferedWriter(filewriter);
//              
//              switch (answer1.getCheckedRadioButtonId()) {
//                case(R.id.rad_male):
//                  out.write(getText(R.string.survey_male) + ",");
//                  break;
//                case(R.id.rad_female):
//                  out.write(getText(R.string.survey_female) + ",");
//                  break;                
//              }
//
//              switch (answer2.getCheckedRadioButtonId()) {
//                case(R.id.rad_21):
//                  out.write(getText(R.string.survey_age1) + ",");
//                  break;
//                case(R.id.rad_26):
//                  out.write(getText(R.string.survey_age2) + ",");
//                  break;
//                case(R.id.rad_31):
//                  out.write(getText(R.string.survey_age3) + ",");
//                  break;
//                case(R.id.rad_40):
//                  out.write(getText(R.string.survey_age4) + ",");
//                  break;
//              }
//              
//              switch (answer3.getCheckedRadioButtonId()) {
//                case(R.id.rad_enjoy_1):
//                  out.write(getText(R.string.survey_scale1) + ",");
//                  break;
//                case(R.id.rad_enjoy_2):
//                  out.write(getText(R.string.survey_scale2) + ",");
//                  break;
//                case(R.id.rad_enjoy_3):
//                  out.write(getText(R.string.survey_scale3) + ",");
//                  break;
//                case(R.id.rad_enjoy_4):
//                  out.write(getText(R.string.survey_scale4) + ",");
//                  break;
//                case(R.id.rad_enjoy_5):
//                  out.write(getText(R.string.survey_scale5) + ",");
//                  break;
//              }
//              
//              switch (answer4.getCheckedRadioButtonId()) {
//                case(R.id.rad_buy_1):
//                  out.write(getText(R.string.survey_scale1) + ",");
//                  break;
//                case(R.id.rad_buy_2):
//                  out.write(getText(R.string.survey_scale2) + ",");
//                  break;
//                case(R.id.rad_buy_3):
//                  out.write(getText(R.string.survey_scale3) + ",");
//                  break;
//                case(R.id.rad_buy_4):
//                  out.write(getText(R.string.survey_scale4) + ",");
//                  break;
//                case(R.id.rad_buy_5):
//                  out.write(getText(R.string.survey_scale5) + ",");
//                  break;
//              }
//              
//              out.write("\n");
//                out.flush();
//                
//                answer1.clearCheck();
//                answer2.clearCheck();
//                answer3.clearCheck();
//                answer4.clearCheck();
//                
//                final AlertDialog ad = new AlertDialog.Builder(activity).create();
//                ad.setTitle(getText(R.string.survey_thanks));
//                ad.setMessage(getText(R.string.survey_message));
//                ad.setButton("OK", new DialogInterface.OnClickListener() {
//              
//              @Override
//              public void onClick(DialogInterface dialog, int which) {
//                ad.dismiss();
//              }
//              
//            });
//                ad.show();
//          } catch (IOException e) {
//              Log.e("fileIO", "Could not write file " + e.getMessage());
//          }
//        }
//      }
//    });
//    
//    gesture = new GestureDetector(getApplicationContext(), new SimpleOnGestureListener(){
//        public boolean onDown(MotionEvent event) {
//          return true;
//        }
//      });
////      gestureListener = new View.OnTouchListener() {
////        @Override
////        public boolean onTouch(View v, MotionEvent event) {
////          return gesture.onTouchEvent(event);
////        }
////      };
//  }
//  
//  /**
//   * Adds the menu to the screen
//   * 
//   * @param Menu menu
//   * @return boolean
//   */
//  @Override
//  public boolean onCreateOptionsMenu(Menu menu) {
//    MenuInflater inflater = getMenuInflater();
//    inflater.inflate(R.menu.survey_menu, menu);
//    return true;
//  }
//  
//  @Override
//  public boolean onOptionsItemSelected(MenuItem item) {
//    switch (item.getItemId()) {
//    case R.id.settings:
//        Intent i = new Intent(this, SettingsActivity.class);
//        startActivity(i);
//        return true;
//    case R.id.tag:
//        i = new Intent(this, HomeGridActivity.class);
//        startActivity(i);
//        return true;
//    case R.id.email:
//      i = new Intent(this, EmailActivity.class);
//      startActivity(i);
//      return true;
//    }
//    return super.onOptionsItemSelected(item);
//  }
//  
//  @Override
//    public boolean onTouchEvent(MotionEvent event) {   
//    try {
//      int action = event.getAction() & MotionEvent.ACTION_MASK;
//      if (action == MotionEvent.ACTION_POINTER_UP) {
//        Calendar now = Calendar.getInstance();
//        
//        if (event.getPointerCount() == 2) {
//          if (!click1) {
//            click1 = true;
//            click2 = false;
//            first = now.getTimeInMillis(); 
//          } else if (click1){
//            click2 = true;
//            second = now.getTimeInMillis();
//          } else {
//            //hmmmmm what to do...?
//          }
//          
//          if (click1 && click2 && Math.abs(second-first) < 500) {
//            try {
//              out.close();
//            } catch (IOException e) {
//              e.printStackTrace();
//            } catch (NullPointerException e2) {
//              e2.printStackTrace();
//            }
//            finish();
//          } else if (Math.abs(second-first) >= 500) {
//            click1 = false;
//            click2 = false;
//          }
//        }
//      }
//    } catch (Exception e){
//      
//    }
//    return true;
//    }
}
