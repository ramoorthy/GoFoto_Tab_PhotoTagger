/**
 * PhotoTagger
 * ImageProcessingService.java
 * 
 * @author Jason Harris on Jul 9, 2012
 * @copyright 2012 Poggled, Inc. All rights reserved
 * 
 * Typical Usage: Started on application startup and will run continuously in the background
 * to monitor images added to the Eye-Fi directory.  Spawns an AsyncTask for each image in the 
 * Eye-Fi directory(checking for already processed images) to be processed and saved in the
 * a separate directory.
 * 
 */
package com.poggled.android.phototagger.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.media.ExifInterface;
import android.mtp.MtpConstants;
import android.mtp.MtpDevice;
import android.mtp.MtpObjectInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.poggled.android.phototagger.BuildConfig;
import com.poggled.android.phototagger.PhotoTaggerApplication;
import com.poggled.android.phototagger.R;
import com.poggled.android.phototagger.provider.Images;
import com.poggled.android.phototagger.ui.ImageServiceControllerActivity;
import com.poggled.android.phototagger.util.BitmapUtils;
import com.poggled.android.phototagger.util.BitmapUtils.ScalingLogic;
import com.poggled.android.phototagger.util.FileLoggerMessage;
import com.poggled.android.phototagger.util.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service implementation of ImageProcessing to handle background processing of images.
 *
 * Notification Manager is provided to allow a user to manually start/stop the service
 */

public class ImageProcessingService extends Service {
    
    // Bitmaps for use in watermarking the images
    public static Bitmap mSponsorBitmap;
    private Bitmap mLogoBitmap;

    // Directory location for monitoring and output
    private static File mSourceDir;
    private static File mOutputDir;
    
    public static final String OUTPUT_DIR_NAME = "processed";
    private static final String OUTPUT_TEMP_DIR_NAME = "temp";
    
    // File observer for watching the eye-fi directory
    private static FileObserver mObserver;
    
    // References to service objects
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private final Handler mUiThreadHandler = new Handler();
    private NotificationManager mNotificationManager;

    // Unique Identification Number for the Notification.
    private int START_NOTIFICATION = R.string.image_service_started;
    
    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class ImageProcessingBinder extends Binder {
        ImageProcessingService getService() {
            return ImageProcessingService.this;
        }
    }
    
    @Override
    public void onCreate() {
        
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        startForeground(START_NOTIFICATION,  buildNotification());
        
        // Load the filename for the sponsor's watermark.
        final String watermarkFileName = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.pref_watermark_key), null);
        
        if(watermarkFileName != null) {
            mSponsorBitmap = BitmapFactory.decodeFile(watermarkFileName);
        } else {
            // Default to the poggled logo if a watermark has not been selected
            mSponsorBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
        }
        
        mLogoBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
        
        // TODO: check media state
        mSourceDir = new File(Environment.getExternalStorageDirectory() + File.separator + "Eye-Fi");
        mOutputDir = new File(Utils.getExternalCacheDir(this), OUTPUT_DIR_NAME);
        
        if (!mOutputDir.exists()) mOutputDir.mkdir();
        if (!mSourceDir.exists()) mSourceDir.mkdir();
        
        mObserver = new EyeFiFileObserver(mSourceDir.getAbsolutePath());
        mObserver.startWatching();
        
        // Start the thread to a priority slightly less than the UI thread
        HandlerThread thread = new HandlerThread(ImageProcessingService.class.getSimpleName(),
                Process.THREAD_PRIORITY_FOREGROUND);
        thread.start();
        
        // Get the HandlerThread's Looper and use it for our Handler 
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (BuildConfig.DEBUG) {
            Log.i(getClass().getName(), "Received start id " + startId + ": " + intent);
        }
        
        if(intent == null || intent.getAction() == Intent.ACTION_SYNC) { 
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = startId;
            msg.what = HANDLE_ALL_FILES;
            mServiceHandler.sendMessage(msg);
        } else {
            UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if(device != null) {
                Message msg = mServiceHandler.obtainMessage();
                msg.arg1 = startId;
                msg.obj = device;
                msg.what = HANDLE_USB;
                mServiceHandler.sendMessage(msg);
            } else {
                Uri uri = intent.getData();
                if(uri != null && uri.getScheme().equals("file")) {
                    Message msg = mServiceHandler.obtainMessage();
                    msg.arg1 = startId;
                    msg.obj = uri;
                    msg.what = HANDLE_SINGLE_FILE;
                    mServiceHandler.sendMessage(msg);
                } else {
                    Toast.makeText(this, R.string.image_service_error, Toast.LENGTH_SHORT).show();
                }
            }
        }
       
            
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        mNotificationManager.cancel(START_NOTIFICATION);

        // Tell the user we stopped.
        Toast.makeText(this, R.string.image_service_stopped, Toast.LENGTH_SHORT).show();
        
        mObserver.stopWatching();
        mObserver = null;
        
        mServiceLooper.quit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // This is the object that receives interactions from clients.  
    private final IBinder mBinder = new ImageProcessingBinder();

    /**
     * Show a notification while this service is running.
     */
    private Notification buildNotification() {
        
        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, ImageServiceControllerActivity.class), 0);
        
        return new NotificationCompat.Builder(this)
            .setContentTitle(getText(R.string.image_service_label))
            .setContentText(getText(R.string.image_service_started))
            .setSmallIcon(R.drawable.icon)
            .setContentIntent(contentIntent)
            .setWhen(System.currentTimeMillis())
            .build();
    }
    
    private static final int HANDLE_ALL_FILES = 0;
    private static final int HANDLE_SINGLE_FILE = 1;
    private static final int HANDLE_USB = 2;
    
    /**
     * Handler that allows us to send and process messages on it's own thread. This way messages can
     * be used to enqueue an action to be done in the background.
     * 
     * All messages have a msg.what field for the type of action to be performed adn msg.arg1 field
     * for the job start id.
     * 
     * The field msg.obj is dependant on the action and holds a parcellable object that will be necessary 
     * to perform the image processing:
     * 
     * HANDLE_ALL_FILES: msg.obj - Not used
     * HANDLE_SINGLE_FILE msg.obj - (Uri) a file uri to process.
     * HANDLE_USB msg.obj - (UsbDevice) the attached usb device.
     */
    @SuppressLint("HandlerLeak")
    private final class ServiceHandler extends Handler {
        
        private static final int IMAGE_TARGET_WIDTH = 720;
        private static final int IMAGE_TARGET_HEIGHT = 480;
        private static final int IMAGE_COMPRESS_QUALITY = 100;
        
        private int USB_NOTIFICATION_ID = R.string.usb_start_search;
        private int USB_NOTIFICATION_DELAY = 10000;
        
        /**
         * Constructor for this handler.
         * @param looper
         */
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        
        /* 
         * {@inheritDoc}
         */
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLE_ALL_FILES:
                    // Start with the initial images to be processed
                    String[] imgs  = new Images(mSourceDir).getFileNames();
                    // and the images in the output directory
                    List<String> processed = Arrays.asList(new Images(ImageProcessingService.this).getFileNames());
                    for(int i=0; i < imgs.length; i++) {
                        // Only launch new process task if the images are not already processed
                        if(!processed.contains(imgs[i])) {
                            //new ProcessImageTask().execute(imgs[i]);
                            processBitmap(new File(mSourceDir, imgs[i]));
                        }
                    }
                    
                    // Remove any remaining requests to update all files from the message queue
                    removeMessages(HANDLE_ALL_FILES);
                    break;
                case HANDLE_SINGLE_FILE:
                    Uri uri = (Uri) msg.obj;
                    processBitmap(new File(uri.getPath()));
                    break;
                case HANDLE_USB:
                    UsbDevice device = (UsbDevice) msg.obj;
                    
                    if (device == null) return;
                    
                    // Setup the device system service and open a connection
                    UsbManager usbManager = (UsbManager) ImageProcessingService.this.getSystemService(Context.USB_SERVICE);
                    UsbDeviceConnection usbDeviceConnection = usbManager.openDevice(device);
                    
                    // Create and open the MtpDevice; now the Mtp device owns the Usb connection
                    MtpDevice mtpDevice = new MtpDevice(device);
                    if (!mtpDevice.open(usbDeviceConnection)) return;
                    
                    // Send a notification that we are starting the search.
                    NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(ImageProcessingService.this);
                    Notification notification = notificationBuilder.setContentTitle(getText(R.string.image_service_label))
                        .setContentText(getText(R.string.usb_start_search))
                        .setSmallIcon(R.drawable.icon)
                        .setProgress(0, 0, true)
                        .setOngoing(true)
                        .build();
                    
                    mNotificationManager.notify(USB_NOTIFICATION_ID, notification);
                    
                    ArrayList<MtpObjectInfo> jpegs = getMtpFiles(mtpDevice);
                    
                    File dir = new File(Utils.getExternalCacheDir(ImageProcessingService.this), ImageProcessingService.OUTPUT_TEMP_DIR_NAME);
                    if(!dir.exists()) dir.mkdirs();
                    
                    // Replace the notification with one showing our progress
                    String text = getResources().getQuantityString(R.plurals.usb_transfer, jpegs.size(), 0, jpegs.size());
                    notification = notificationBuilder.setContentTitle(getText(R.string.image_service_label))
                            .setContentText(text)
                            .setSmallIcon(R.drawable.icon)
                            .setProgress(jpegs.size(), 0, false)
                            .setOngoing(true)
                            .build();
                    
                    mNotificationManager.notify(USB_NOTIFICATION_ID, notification);
                    
                    // Process each of the jpegs we have found.
                    int count = 0;
                    for(MtpObjectInfo jpeg:jpegs) {
                        File temp = new File(dir, jpeg.getName());
                        mtpDevice.importFile(jpeg.getObjectHandle(), temp.getAbsolutePath());
                        processBitmap(temp);  
                        temp.delete();
                        count++;
                        
                        // Update the progress notification.
                        text = getResources().getQuantityString(R.plurals.usb_transfer, jpegs.size(), count, jpegs.size());
                        notification = notificationBuilder.setContentTitle(getText(R.string.image_service_label))
                                .setContentText(text)
                                .setSmallIcon(R.drawable.icon)
                                .setProgress(jpegs.size(), count, false)
                                .setOngoing(true)
                                .build();
                        mNotificationManager.notify(USB_NOTIFICATION_ID, notification);
                    }
                    
                    // Delay clearing the message for some time
                    mUiThreadHandler.postDelayed(new Runnable() {

             
                        public void run() {
                            mNotificationManager.cancel(USB_NOTIFICATION_ID);
                        }
                        
                    }, USB_NOTIFICATION_DELAY);
                    
                    mtpDevice.close();
                    usbDeviceConnection.close();
                    
                    // Remove any remaining requests to sync usb from the message queue
                    removeMessages(HANDLE_USB);
                    break;
               default:
                   if (BuildConfig.DEBUG) {
                       Log.i(getClass().getName(), "ImageProcessingService: Unknown message type " + msg.toString());
                   }
                   break;
            }
        }
        
        /**
         * Base method for setting up an Mtp device search.  This allows us to handle the case of
         * multiple storageIds on a single device
         * 
         * @param device An open MtpDevice
         * @return An ArrayList of MtpObjectInfo references to found jpegs
         */
        //TODO: Refactor this into searchStorage if the non-recursive solution appears to be working on multiple devices
        private ArrayList<MtpObjectInfo> getMtpFiles(MtpDevice device){
            ArrayList<MtpObjectInfo> result = new ArrayList<MtpObjectInfo>();
            // Get storage ids 
            int[] storageIds = device.getStorageIds();
            
            if (storageIds == null) {
                return result;
            }
            
            // Usually there will be only one storageId but just in case
            for (int storageId : storageIds) {
                searchStorage(result, device, storageId, 0);
            } 
            return result;
        }
        
        /**
         * Searches through a given storageId on an open MtpDevice, adding jpegs to list.
         * 
         * @param list The list of MtpObjectInfo references to accumulate jpegs
         * @param mtpDevice A reference to the device that will be searched
         * @param storageId The storage id on the device to search.
         * @param parent Argument that can specify the object handle number of a folder to be searched
         */
        private void searchStorage(ArrayList<MtpObjectInfo> list, MtpDevice mtpDevice, int storageId, int parent) {
            
            int[] objectHandles = mtpDevice.getObjectHandles(storageId, MtpConstants.FORMAT_EXIF_JPEG, parent);
            if (objectHandles == null) {
                return;
            }
            
            for (int objectHandle : objectHandles) {
                // Skip the object if we can't get the object info; shouldn't normally occur.
                MtpObjectInfo mtpObjectInfo = mtpDevice.getObjectInfo(objectHandle);
                if (mtpObjectInfo == null) {
                    continue;
                }

                // Initial implementation was excluding handles where mtpObjectInfo.getParent() != parent and
                // recursively searching for folders encountered where the association matched MtpConstants.ASSOCIATION_TYPE_GENERIC_FOLDER. 
                // This simpler implementation seems to be faster since getObjectHandles appears to return all 
                // handles including subfolders, however, this could possibly be device dependent(?).  May need 
                // to be changed back if we encounter devices that are not returning all images.
                
                if (mtpObjectInfo.getProtectionStatus() != MtpConstants.PROTECTION_STATUS_NON_TRANSFERABLE_DATA) {
                    
                    // Check if image has already been received
                    File prev = new File(Utils.getExternalCacheDir(ImageProcessingService.this), 
                            ImageProcessingService.OUTPUT_DIR_NAME + File.separator + mtpObjectInfo.getName());
                    if(!prev.exists()) {
                        list.add(mtpObjectInfo);
                    }
                }
            }
        }
        
        /**
         * Processes an image file with the appropriate size, scaling, rotation, and watermarks.
         * 
         * @param imageFile The file to be processed.
         */
        private void processBitmap(File imageFile) {
            
            boolean success = false;
            
            try {
                
                // Decode the image and sample efficiently to reduce memory usage
                Bitmap bitmap = BitmapUtils.decodeFile(imageFile.getAbsolutePath(), IMAGE_TARGET_WIDTH, IMAGE_TARGET_HEIGHT, ScalingLogic.FIT);
                // Scale the image
                bitmap = BitmapUtils.createScaledBitmap(bitmap, IMAGE_TARGET_WIDTH, IMAGE_TARGET_HEIGHT, ScalingLogic.FIT);
                
                // Start reading the exif info to see if we need to rotate the image
                ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                int rotate = 0;
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        rotate = 270;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        rotate = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        rotate = 90;
                        break;
                }
                
                Matrix matrix = new Matrix();
                matrix.postRotate(rotate);
                
                // Apply the rotation, if necessary
                if(rotate != 0) {
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                }
                
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                final boolean shouldUseFrame = preferences.getBoolean(getString(R.string.pref_frame_enabled), false);
                
                if(shouldUseFrame) {
                    
                    if(rotate == 0 || rotate == 180) {
                        final String frameFilepath = preferences.getString(getString(R.string.pref_frame_landscape_image_key), null);
                        Bitmap frame = BitmapFactory.decodeFile(frameFilepath);
                        bitmap = BitmapUtils.frameImage(bitmap, frame);
                    } else {
                        final String frameFilepath = preferences.getString(getString(R.string.pref_frame_portrait_image_key), null);
                        Bitmap frame = BitmapFactory.decodeFile(frameFilepath);
                        bitmap = BitmapUtils.frameImage(bitmap, frame);
                    }
                } else {
                    // Watermark the image
                    bitmap = BitmapUtils.watermarkImage(bitmap, mSponsorBitmap, mLogoBitmap);
                }
                
                // Save the image to our output directory
                final String outputFilename = mOutputDir.getAbsolutePath() + File.separator + imageFile.getName();
                success = BitmapUtils.writeBitmapToFile(bitmap, outputFilename, CompressFormat.JPEG, IMAGE_COMPRESS_QUALITY);
                
            } catch (OutOfMemoryError e) {
                onError(new Exception("Out of Memory"));
            } catch (FileNotFoundException e) {
                onError(e);
            } catch (IOException e) {
                onError(e);
            } finally {
                if(success) {
                    final FileLoggerMessage msg = new FileLoggerMessage(FileLoggerMessage.IMAGE_RECEIVED, Utils.getWifiSignalStrength(ImageProcessingService.this));
                    PhotoTaggerApplication.getFileLogger().info(msg.toString());
                }
                
                if (BuildConfig.DEBUG) {
                    Log.i(getClass().getName(), "ImageProcessingService: Attempt to process image " + imageFile.getName() + " success = " + success);
                }
            }
        }
        
        /**
         * Report an error
         *
         * @param the exception to report
         * 
         */
        private void onError(Exception e) {
            StringBuilder sb = new StringBuilder();
            sb.append(ImageProcessingService.this.getResources().getString(R.string.image_service_error));
            sb.append(" ");
            sb.append(e.toString());
            Toast.makeText(ImageProcessingService.this, sb.toString(), Toast.LENGTH_SHORT).show();
        }
    }
        
    /**
     *  A system file observer that watches a directory for changes. A reference to this object 
     *  must be kept in the service or the FileObserver will stop sending events.
     *  
     *  NOTE: the onEvent method is ran from a background thread so UI updates must be made to occur 
     *  on a foreground thread.
     */
    
    private class EyeFiFileObserver extends FileObserver {
        
        /**
         * Constructor for this object
         * 
         * @param path The absolute file path to the directory we will be watching
         */
        public EyeFiFileObserver(String path) {
            super(path, FileObserver.MOVED_TO | FileObserver.CLOSE_WRITE | FileObserver.MOVED_FROM | FileObserver.DELETE);
        }
        
        /* 
         * {@inheritDoc}
         */
        @Override
        public void onEvent(int event, final String path) {
            if (path == null) {
                return;
            }
            
            if(event == FileObserver.MOVED_TO || event == FileObserver.CLOSE_WRITE ) {
                if (BuildConfig.DEBUG) {
                    Log.i(getClass().getName(), "ImageProcessingService: Received new file observer notification for file " + path + " event = " + event);
                }
                
                final File file = new File(mSourceDir, path);
                if (BitmapUtils.isImage(file)) {
                    // Check if the image has already been processed
                    final File prev = new File(mOutputDir, path);
                    if(!prev.exists()) {
                        // AsyncTask must be started from the ui thread
                        mUiThreadHandler.post(new Runnable() {

                            public void run() {
                                // Start an intent to process the file.
                                Intent intent = new Intent(ImageProcessingService.this, ImageProcessingService.class);
                                intent.setData(Uri.fromFile(file));
                                startService(intent);
                            }
                            
                        });
                        
                    }
                }
            }
            
            // Delete the file from our output directory if we receive a notification that the Eye-Fi file has been removed.
            if(event ==  FileObserver.MOVED_FROM | event == FileObserver.DELETE) {
                if (BuildConfig.DEBUG) {
                    Log.i(getClass().getName(), "ImageProcessingService: Received notification file deleted or moved, removing: " + path + " event = " + event);
                }
                final File f = new File(mOutputDir, path);
                if(f.exists()) {
                    f.delete();
                }
            }
            
        }
    }
}

