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

import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class RespiViewThread extends Thread {
	private RespiView mView;
	private SurfaceHolder mHolder;
	private RespiStateManager mRespiStateManager;
	private boolean mRun = false;

	public RespiViewThread(RespiView respiview) {
		mView = respiview;
		mHolder = mView.getHolder();
		mRespiStateManager = mView.getRespiStateManager();
	}

	// Set current thread state
	public void setRunning(boolean run) {
		mRun = run;
	}

	@Override
	public void run() {
		Canvas canvas = null;

		// Thread loop
		while (mRun) {
			// Obtain lock on canvas object
			canvas = mHolder.lockCanvas();

			if (canvas != null) {
				// Update state based on elapsed time
				long elapsed = System.nanoTime() - mRespiStateManager.getStartTime();

				// Render updated state
				mView.doDraw(canvas,elapsed);

				// Release lock on canvas object
				mHolder.unlockCanvasAndPost(canvas);
			}
		}
	}
}
