package com.poggled.android.phototagger.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

public class BitmapUtils {
    
    public static final String IMAGE_MIME = "image";
    
    
    
    public static Bitmap frameImage(Bitmap bitmap, Bitmap frame) {
        
        Bitmap framedBitmap = null; 
        int width, height = 0; 
        
        if(frame == null) return bitmap;
        
        // initialize with our largest dimensions
        width = Math.max(bitmap.getWidth(), frame.getWidth()); 
        height = Math.max(bitmap.getHeight(), frame.getHeight()); 
        
        framedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); 
        int centerX = (width - bitmap.getWidth()) / 2;
        int centerY = (height - bitmap.getHeight()) / 2;
        Canvas canvas = new Canvas(framedBitmap); 
        canvas.drawBitmap(bitmap, centerX, centerY, null); 
        canvas.drawBitmap(frame, 0f, 0f, null); 
        
        return framedBitmap;
    }
    
    public static Bitmap watermarkImage(Bitmap bitmap, Bitmap logo1, Bitmap logo2) {
        
        Bitmap watermarkedBitmap = null; 
        int width, height = 0; 
        
        // We want an image that is going to be as tall as our tallest image.  For width
        // we want it as wide as the larger of the original image width or the cumulative width '
        // of both logo bitmaps (since they will be placed next to each other).
        width = Math.max(bitmap.getWidth(), logo1.getWidth() + logo2.getWidth()); 
        height = Math.max(Math.max(bitmap.getHeight(), logo1.getHeight()), logo2.getHeight()); 
        
        watermarkedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); 
        
        Canvas canvas = new Canvas(watermarkedBitmap); 
     
        canvas.drawBitmap(bitmap, 0f, 0f, null); 
        canvas.drawBitmap(logo1, 0f, height - logo1.getHeight(), null); 
        canvas.drawBitmap(logo2, width - logo2.getWidth(), height - logo2.getHeight(), null); 
        
        return watermarkedBitmap;
    }
    
    /**
     * Check if a file is an image
     *
     * @param file the file to be checked
     * @return a boolean representing success of failure
     */
    public static boolean isImage(File file) {
        
        Uri selectedUri = Uri.fromFile(file);

        String fileExtension
         = MimeTypeMap.getFileExtensionFromUrl(selectedUri.toString());
        String mimeType
         = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
        
        return file.isFile() && !file.isDirectory() && !file.isHidden() 
                && mimeType != null && !mimeType.trim().isEmpty() && mimeType.startsWith(IMAGE_MIME);
    }
    
    /**
     * Write Bitmap to a filename. This method will create the file if it does not exist.
     *
     * @param bitmap the bitmap to write
     * @param filename the absolute path for the file
     * @param format the format of the compressed image
     * @param quality the quality of compression
     * @return a boolean representing success of failure
     */
    public static boolean writeBitmapToFile(Bitmap bitmap, String filename, CompressFormat format, int quality)
            throws IOException, FileNotFoundException { 

        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(filename), Utils.IO_BUFFER_SIZE);
            return bitmap.compress(format, quality, out);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
    /**
     * ScalingLogic defines how scaling should be carried out if source and
     * destination image has different aspect ratio.
     *
     * CROP: Scales the image the minimum amount while making sure that at least
     * one of the two dimensions fit inside the requested destination area.
     * Parts of the source image will be cropped to realize this.
     *
     * FIT: Scales the image the minimum amount while making sure both
     * dimensions fit inside the requested destination area. The resulting
     * destination dimensions might be adjusted to a smaller size than
     * requested.
     */
    public static enum ScalingLogic {
        CROP, FIT
    }
    
    /**
     * Utility function for decoding an image resource. The decoded bitmap will
     * be optimized for further scaling to the requested destination dimensions
     * and scaling logic.
     *
     * @param res The resources object containing the image data
     * @param resId The resource id of the image data
     * @param dstWidth Width of destination area
     * @param dstHeight Height of destination area
     * @param scalingLogic Logic to use to avoid image stretching
     * @return Decoded bitmap
     */
    public static Bitmap decodeResource(Resources res, int resId, int dstWidth, int dstHeight,
            ScalingLogic scalingLogic) {
        Options options = new Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);
        options.inJustDecodeBounds = false;
        options.inSampleSize = calculateSampleSize(options.outWidth, options.outHeight, dstWidth,
                dstHeight, scalingLogic);
        Bitmap unscaledBitmap = BitmapFactory.decodeResource(res, resId, options);

        return unscaledBitmap;
    }
    
    /**
     * Utility function for decoding an image file. The decoded bitmap will
     * be optimized for further scaling to the requested destination dimensions
     * and scaling logic.
     *
     * @param pathName The path of the file containing the image data
     * @param dstWidth Width of destination area
     * @param dstHeight Height of destination area
     * @param scalingLogic Logic to use to avoid image stretching
     * @return Decoded bitmap
     */
    
    public static Bitmap decodeFile(String pathName, int dstWidth, int dstHeight, ScalingLogic scalingLogic) {

        Options options = new Options();

        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(pathName, options);

        options.inJustDecodeBounds = false;

        options.inSampleSize = calculateSampleSize(options.outWidth, options.outHeight, dstWidth, dstHeight, scalingLogic);

        Bitmap unscaledBitmap = BitmapFactory.decodeFile(pathName, options);

        return unscaledBitmap;

      }
    
    

    /**
     * Calculate optimal down-sampling factor given the dimensions of a source
     * image, the dimensions of a destination area and a scaling logic.
     *
     * @param srcWidth Width of source image
     * @param srcHeight Height of source image
     * @param dstWidth Width of destination area
     * @param dstHeight Height of destination area
     * @param scalingLogic Logic to use to avoid image stretching
     * @return Optimal down scaling sample size for decoding
     */
    public static int calculateSampleSize(int srcWidth, int srcHeight, int dstWidth, int dstHeight,
            ScalingLogic scalingLogic) {
        if (scalingLogic == ScalingLogic.FIT) {
            final float srcAspect = (float)srcWidth / (float)srcHeight;
            final float dstAspect = (float)dstWidth / (float)dstHeight;

            if (srcAspect > dstAspect) {
                return srcWidth / dstWidth;
            } else {
                return srcHeight / dstHeight;
            }
        } else {
            final float srcAspect = (float)srcWidth / (float)srcHeight;
            final float dstAspect = (float)dstWidth / (float)dstHeight;

            if (srcAspect > dstAspect) {
                return srcHeight / dstHeight;
            } else {
                return srcWidth / dstWidth;
            }
        }
    }

    /**
     * Utility function for creating a scaled version of an existing bitmap
     *
     * @param unscaledBitmap Bitmap to scale
     * @param dstWidth Wanted width of destination bitmap
     * @param dstHeight Wanted height of destination bitmap
     * @param scalingLogic Logic to use to avoid image stretching
     * @return New scaled bitmap object
     */
    public static Bitmap createScaledBitmap(Bitmap unscaledBitmap, int dstWidth, int dstHeight,
            ScalingLogic scalingLogic) {
        Rect srcRect = calculateSrcRect(unscaledBitmap.getWidth(), unscaledBitmap.getHeight(),
                dstWidth, dstHeight, scalingLogic);
        Rect dstRect = calculateDstRect(unscaledBitmap.getWidth(), unscaledBitmap.getHeight(),
                dstWidth, dstHeight, scalingLogic);
        Bitmap scaledBitmap = Bitmap.createBitmap(dstRect.width(), dstRect.height(),
                Config.ARGB_8888);
        Canvas canvas = new Canvas(scaledBitmap);
        canvas.drawBitmap(unscaledBitmap, srcRect, dstRect, new Paint(Paint.FILTER_BITMAP_FLAG));

        return scaledBitmap;
    }
    
    
    /**
     * Calculates source rectangle for scaling bitmap
     *
     * @param srcWidth Width of source image
     * @param srcHeight Height of source image
     * @param dstWidth Width of destination area
     * @param dstHeight Height of destination area
     * @param scalingLogic Logic to use to avoid image stretching
     * @return Optimal source rectangle
     */
    public static Rect calculateSrcRect(int srcWidth, int srcHeight, int dstWidth, int dstHeight,
            ScalingLogic scalingLogic) {
        if (scalingLogic == ScalingLogic.CROP) {
            final float srcAspect = (float)srcWidth / (float)srcHeight;
            final float dstAspect = (float)dstWidth / (float)dstHeight;

            if (srcAspect > dstAspect) {
                final int srcRectWidth = (int)(srcHeight * dstAspect);
                final int srcRectLeft = (srcWidth - srcRectWidth) / 2;
                return new Rect(srcRectLeft, 0, srcRectLeft + srcRectWidth, srcHeight);
            } else {
                final int srcRectHeight = (int)(srcWidth / dstAspect);
                final int scrRectTop = (int)(srcHeight - srcRectHeight) / 2;
                return new Rect(0, scrRectTop, srcWidth, scrRectTop + srcRectHeight);
            }
        } else {
            return new Rect(0, 0, srcWidth, srcHeight);
        }
    }

    /**
     * Calculates destination rectangle for scaling bitmap
     *
     * @param srcWidth Width of source image
     * @param srcHeight Height of source image
     * @param dstWidth Width of destination area
     * @param dstHeight Height of destination area
     * @param scalingLogic Logic to use to avoid image stretching
     * @return Optimal destination rectangle
     */
    public static Rect calculateDstRect(int srcWidth, int srcHeight, int dstWidth, int dstHeight,
            ScalingLogic scalingLogic) {
        if (scalingLogic == ScalingLogic.FIT) {
            final float srcAspect = (float)srcWidth / (float)srcHeight;
            final float dstAspect = (float)dstWidth / (float)dstHeight;

            if (srcAspect > dstAspect) {
                return new Rect(0, 0, dstWidth, (int)(dstWidth / srcAspect));
            } else {
                return new Rect(0, 0, (int)(dstHeight * srcAspect), dstHeight);
            }
        } else {
            return new Rect(0, 0, dstWidth, dstHeight);
        }
    }
    private static final float PHOTO_BORDER_WIDTH = 3.0f;
    private static final int PHOTO_BORDER_COLOR = 0xffffffff;

    private static final float ROTATION_ANGLE_MIN = 2.5f;
    private static final float ROTATION_ANGLE_EXTRA = 5.5f;

    private static final Random sRandom = new Random();
    private static final Paint sPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private static final Paint sStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    static {
        sStrokePaint.setStrokeWidth(PHOTO_BORDER_WIDTH);
        sStrokePaint.setStyle(Paint.Style.STROKE);
        sStrokePaint.setColor(PHOTO_BORDER_COLOR);
    }

    /**
     * Rotate specified Bitmap by a random angle. The angle is either negative or positive,
     * and ranges, in degrees, from 2.5 to 8. After rotation a frame is overlaid on top
     * of the rotated image.
     *
     * This method is not thread safe.
     *
     * @param bitmap The Bitmap to rotate and apply a frame onto.
     *
     * @return A new Bitmap whose dimension are different from the original bitmap.
     */
    static Bitmap rotateAndFrame(Bitmap bitmap) {
        final boolean positive = sRandom.nextFloat() >= 0.5f;
        final float angle = (ROTATION_ANGLE_MIN + sRandom.nextFloat() * ROTATION_ANGLE_EXTRA) *
                (positive ? 1.0f : -1.0f);
        final double radAngle = Math.toRadians(angle);

        final int bitmapWidth = bitmap.getWidth();
        final int bitmapHeight = bitmap.getHeight();

        final double cosAngle = Math.abs(Math.cos(radAngle));
        final double sinAngle = Math.abs(Math.sin(radAngle));

        final int strokedWidth = (int) (bitmapWidth + 2 * PHOTO_BORDER_WIDTH);
        final int strokedHeight = (int) (bitmapHeight + 2 * PHOTO_BORDER_WIDTH);

        final int width = (int) (strokedHeight * sinAngle + strokedWidth * cosAngle);
        final int height = (int) (strokedWidth * sinAngle + strokedHeight * cosAngle);

        final float x = (width - bitmapWidth) / 2.0f;
        final float y = (height - bitmapHeight) / 2.0f;

        final Bitmap decored = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(decored);

        canvas.rotate(angle, width / 2.0f, height / 2.0f);
        canvas.drawBitmap(bitmap, x, y, sPaint);
        canvas.drawRect(x, y, x + bitmapWidth, y + bitmapHeight, sStrokePaint);

        return decored;
    }

    /**
     * Scales the specified Bitmap to fit within the specified dimensions. After scaling,
     * a frame is overlaid on top of the scaled image.
     *
     * This method is not thread safe.
     *
     * @param bitmap The Bitmap to scale to fit the specified dimensions and to apply
     *               a frame onto.
     * @param width The maximum width of the new Bitmap.
     * @param height The maximum height of the new Bitmap.
     *
     * @return A scaled version of the original bitmap, whose dimension are less than or
     *         equal to the specified width and height.
     */
    static Bitmap scaleAndFrame(Bitmap bitmap, int width, int height) {
        final int bitmapWidth = bitmap.getWidth();
        final int bitmapHeight = bitmap.getHeight();

        final float scale = Math.min((float) width / (float) bitmapWidth, 
                (float) height / (float) bitmapHeight);

        final int scaledWidth = (int) (bitmapWidth * scale);
        final int scaledHeight = (int) (bitmapHeight * scale);

        final Bitmap decored = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);
        final Canvas canvas = new Canvas(decored);

        final int offset = (int) (PHOTO_BORDER_WIDTH / 2);
        sStrokePaint.setAntiAlias(false);
        canvas.drawRect(offset, offset, scaledWidth - offset - 1,
                scaledHeight - offset - 1, sStrokePaint);
        sStrokePaint.setAntiAlias(true);

        return decored;

    }
    
    
}
