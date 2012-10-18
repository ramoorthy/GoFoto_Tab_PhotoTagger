package com.poggled.android.phototagger.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.PopupWindow;

import com.poggled.android.phototagger.R;


public class TagableImageView extends ImageView {

    private static final int INVALID_POINTER_ID = -1;

    private Drawable mImage;
    private float mPosX;
    private float mPosY;
    
    private int mHalfImageWidth;
    private int mHalfImageHeight;
    public boolean mTagActive;
    
    private float mLastTouchX;
    private float mLastTouchY;
    private int mActivePointerId = INVALID_POINTER_ID;

    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 1.f;

    public TagableImageView(Context context) {
        this(context, null, 0);
       
    }

    public TagableImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TagableImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mImage = getResources().getDrawable(R.drawable.tag_cursor);
        mImage.setBounds(0, 0, mImage.getIntrinsicWidth(), mImage.getIntrinsicHeight());
     // TODO: these values are calculated assuming mdpi and could be invalid on different screen sizes
        mHalfImageWidth = mImage.getIntrinsicWidth() / 2;
        mHalfImageHeight = mImage.getIntrinsicHeight() / 2;
        
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Let the ScaleGestureDetector inspect all events.
        mScaleDetector.onTouchEvent(ev);
        
        
        final int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN: {
            
            mTagActive = true;
            
            final float x = ev.getX();
            final float y = ev.getY();
            
            if (!mScaleDetector.isInProgress()) {
                
                mPosX = x - mHalfImageWidth;
                mPosY = y - mHalfImageHeight;
                
                //check if we going outside of the bounds of the image
                float[] values = keepInImageBoundaries(x, y, mPosX, mPosY);
                mPosX = values[0];
                mPosY = values[1];
                
                invalidate();
            }
            mLastTouchX = x;
            mLastTouchY = y;
            mActivePointerId = ev.getPointerId(0);
            break;
        }

        case MotionEvent.ACTION_MOVE: {
            final int pointerIndex = ev.findPointerIndex(mActivePointerId);
            final float x = ev.getX(pointerIndex);
            final float y = ev.getY(pointerIndex);
            
            // Only move if the ScaleGestureDetector isn't processing a gesture.
            if (!mScaleDetector.isInProgress()) {
                
                final float dx = x - mLastTouchX;
                
                mPosX += dx;
                
            
                final float dy = y - mLastTouchY;
                
                mPosY += dy;
                
                //Log.d("DEBUG", "X: "+x+ " mPosX: " + mPosX + " mLastTouchX: " + mLastTouchX);
                //Log.d("DEBUG", "Y: "+ y + " mPosY: " + mPosY + " mLastTouchY: " + mLastTouchY);
                
                //check if we going outside of the bounds of the image
                float[] values = keepInImageBoundaries(x, y, mPosX, mPosY);
                mPosX = values[0];
                mPosY = values[1];
                
                invalidate();
            }
            
            mLastTouchX = x;
            
            mLastTouchY = y;
            

            break;
        }

        case MotionEvent.ACTION_UP: {
            mActivePointerId = INVALID_POINTER_ID;
            break;
        }

        case MotionEvent.ACTION_CANCEL: {
            mActivePointerId = INVALID_POINTER_ID;
            break;
        }

        case MotionEvent.ACTION_POINTER_UP: {
            final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) 
                    >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                    final int pointerId = ev.getPointerId(pointerIndex);
                    if (pointerId == mActivePointerId) {
                        // This was our active pointer going up. Choose a new
                        // active pointer and adjust accordingly.
                        final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                        mLastTouchX = ev.getX(newPointerIndex);
                        mLastTouchY = ev.getY(newPointerIndex);
                        mActivePointerId = ev.getPointerId(newPointerIndex);
                    }
                    break;
        }
        }

        return true;
    }
    
    //TODO: this could probably be refactored
    private float[] keepInImageBoundaries(float x, float y, float mPosX, float mPosY) {
        
        float[] positions = new float[] {mPosX, mPosY};
        
        final Drawable drawable = getDrawable();

        final int intrinsicHeight = drawable.getIntrinsicHeight();
        final int intrinsicWidth = drawable.getIntrinsicWidth();

        final Matrix matrix = getImageMatrix();
        float[] values = new float[9];
        matrix.getValues(values);

        final float xScale = values[Matrix.MSCALE_X];
        final float xTrans = values[Matrix.MTRANS_X];
        final float yScale = values[Matrix.MSCALE_Y];
        final float yTrans = values[Matrix.MTRANS_Y];
        
        if(x < xTrans + mHalfImageWidth) {
           positions[0] = xTrans;
        }
        if(x > (xScale * intrinsicWidth) + xTrans - mHalfImageWidth)
            positions[0] = (xScale * intrinsicWidth) + xTrans - (2 * mHalfImageWidth);
        
        if(y < yTrans + mHalfImageHeight)
            positions[1] = yTrans;
        
        if(y > (yScale * intrinsicHeight) + yTrans - mHalfImageHeight)
            positions[1] = (yScale * intrinsicHeight) + yTrans - (2 * mHalfImageHeight);
        
        return positions;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(mTagActive) {
            canvas.save();
            //Log.d("DEBUG", "X: "+mPosX+" Y: "+mPosY);
            canvas.translate(mPosX, mPosY);
            canvas.scale(mScaleFactor, mScaleFactor);
            mImage.draw(canvas);
            canvas.restore();
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 10.0f));

            invalidate();
            return true;
        }
    }
    
    public int getTagPositionX() {
        return getTagPosition(true);
    }
    
    public int getTagPositionY() {
        return getTagPosition(false);
    }
    
    private int getTagPosition(boolean isXPosition) {
        
        int matrixScaleConstant;
        int matrixTranslationConstant;
        int intrinsicSize;
        float mPos; 
        float mHalfSize;
        
        if(isXPosition) {
            matrixScaleConstant = Matrix.MSCALE_X;
            matrixTranslationConstant = Matrix.MTRANS_X;
            intrinsicSize = getDrawable().getIntrinsicWidth();
            mPos = mPosX; 
            mHalfSize = mHalfImageWidth;
        } else {
            matrixScaleConstant = Matrix.MSCALE_Y;
            matrixTranslationConstant = Matrix.MTRANS_Y;
            intrinsicSize = getDrawable().getIntrinsicHeight();
            mPos = mPosY; 
            mHalfSize = mHalfImageHeight;
        }
        
        final Matrix matrix = getImageMatrix();
        float[] values = new float[9];
        matrix.getValues(values);

        final float scale = values[matrixScaleConstant];
        final float translation = values[matrixTranslationConstant];
        
        final float scaledHeight = scale * intrinsicSize;
        float tagPositionAsPercent = (mPos - translation + mHalfSize) / scaledHeight;
        
        //scale as a number between 0 and 100
        tagPositionAsPercent *= 100;
        if(tagPositionAsPercent < 0) return 0;
        if(tagPositionAsPercent > 100) return 100;
        
        return (int) tagPositionAsPercent;
    }
    
    public float getTagCursorPositionX() {
        return mPosX;
    }
    
    public float getTagCursorPositionY() {
        return mPosY;
    }
    
    public float getTagCursorCenterX() {
        return mPosX + mHalfImageWidth;
    }
    
    public float getTagCursorCenterY() {
        return mPosY + + mHalfImageHeight;
    }
    
    public int getTagCursorWidth() {
        return mImage.getIntrinsicWidth();
    }
    
    public int getTagCursorHeight() {
        return mImage.getIntrinsicHeight();
    }
    
}
