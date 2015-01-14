/* Respi 5/5
 *
 * Copyright 2014-2015 Julien Viard de Galbert
 *
 * This file is part of Respi 5/5.
 *
 * Respi 5/5 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Respi 5/5 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Respi 5/5.  If not, see <http://www.gnu.org/licenses/>.
 */
package name.vdg.respi55;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * @author SVDG
 *
 */
public class RespiView extends SurfaceView implements SurfaceHolder.Callback {
	private RespiViewThread mThread;
	public static float mWidth;
	public static float mHeight;
	private Paint mCirclePaint;
	private float mMaxRadius;
	private Paint mTextPaint;
	private float mTextY;
	private float mEndRadius;
	private boolean mStarted = false;

	private final Lock mLock = new ReentrantLock();


	/**
	 * @param context
	 * @param attrs
	 */
	public RespiView(Context context, AttributeSet attrs) {
		super(context, attrs);
		getHolder().addCallback(this);
		mCirclePaint = new Paint();
		mCirclePaint.setARGB(255, 251, 251, 226);
		mCirclePaint.setAntiAlias(true);
		mTextPaint = new Paint();
		mTextPaint.setARGB(255, 104, 197, 182);
		mTextPaint.setAntiAlias(true);
		mTextPaint.setTextAlign(Paint.Align.CENTER);
		mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
		mThread = new RespiViewThread(this);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// Create and start new thread
		if (!mThread.isAlive()) {
			mThread = new RespiViewThread(this);
			mThread.setRunning(true);
			mThread.start();
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		// Store new extents
		mWidth = width;
		mHeight = height;
		int size = Math.min(width, height);
		mMaxRadius = size * 0.4f;
		mEndRadius = size / 12.0f;
		updateDrawingValues();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// Stop thread
		if (mThread.isAlive()) {
			mThread.setRunning(false);
		}
	}

	public void start() {
		mStarted = true;
		updateDrawingValues();
	}

	public void stop() {
		mStarted = false;
		updateDrawingValues();
	}

	private void updateDrawingValues() {
		mLock.lock();  // block until condition holds
		try {
			float size = Math.min(mWidth, mHeight) * 0.8f;
			Rect rect = new Rect();
			// Set text size to 80% of screen short edge length
			mTextPaint.setTextSize(size);
			mTextPaint.getTextBounds("5/5", 0, 3, rect);
			// Center vertically
			mTextY = (mHeight + rect.height())/2;
			if (!mStarted) {
				mTextPaint.setAlpha(255);
				// making sure the text width fits 64% of screen short edge length
				mTextPaint.setTextSize(0.8f * size * size / rect.width());
				mTextPaint.getTextBounds("5/5", 0, 3, rect);
				mTextY = (mHeight + rect.height())/2;
			}
		}
		finally {
				mLock.unlock();
		}
	}

	public void doDraw(Canvas canvas, long elapsed) {
		mLock.lock();  // block until condition holds
		try {
			boolean started = RespiStateManager.isStarted();
			if (mStarted != started)
			{
				mStarted = started;
				updateDrawingValues();
			}			canvas.drawColor(Color.BLACK);
			float perriod = 10; // in sec
			float endPerriod = 5 * 60; // in sec
			float wave=(float)(Math.cos(elapsed*2.0*Math.PI/(perriod*1000000000.0)));
			if ( !started ) {
				canvas.drawCircle(mWidth/2, mHeight/2, mMaxRadius*(0.95f-0.05f*wave), mCirclePaint);
				canvas.drawText("5/5", mWidth/2, mTextY, mTextPaint);
			} else {
				int endCount = (int) (elapsed/(endPerriod*1000000000));
				float endX = mEndRadius;
				float endY = mEndRadius;
				for (int i=0; i<endCount; i++) {
					canvas.drawCircle(endX, endY, 0.8f * mEndRadius, mCirclePaint);
					endX += 2 * mEndRadius;
					if (endX + 0.8f * mEndRadius > mWidth) {
						endX = mEndRadius;
						endY += 2 * mEndRadius;
						if (endY + 0.8f * mEndRadius > mHeight) break;
					}
				}
				canvas.drawCircle(mWidth/2, mHeight/2, mMaxRadius*(0.55f-0.45f*wave), mCirclePaint);
				if (RespiStateManager.isDigitDisplayed()) {
					int number=(int) ((elapsed/1000000000) % 5 + 1);
					mTextPaint.setAlpha((int) Math.ceil(255*Math.cos((elapsed%1000000000)*Math.PI/2000000000.0)));
					canvas.drawText(Integer.toString(number), mWidth/2, mTextY, mTextPaint);
				}
			}
			// debug:
			//canvas.drawText("T: " + elapsed/1000000000.0, 50, 10, mCirclePaint);
		}
		finally {
			mLock.unlock();
		}
	}
}
