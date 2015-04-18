/*
 * Copyright 2012-2013, Arno Puder
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.puder.trs80;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class ScreenshotView extends FrameLayout {

    final private static float ASPECT_RATIO        = 0.75f;

    private boolean            hasScreenshot       = false;
    private ImageView          screenshot;
    private ProgressBar        spinner;
    private static Bitmap      startEmulatorBitmap = null;


    public ScreenshotView(Context context, AttributeSet attrs) {
        super(context, attrs);
        screenshot = new ImageView(context);
        screenshot.setVisibility(View.GONE);
        addView(screenshot);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.CENTER;
        spinner = new ProgressBar(context);
        spinner.setLayoutParams(params);
        addView(spinner);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height;
        if (hasScreenshot) {
            Drawable d = screenshot.getDrawable();
            height = (int) Math.ceil((float) width * (float) d.getIntrinsicHeight()
                    / (float) d.getIntrinsicWidth());
        } else {
            height = (int) Math.ceil((float) width * ASPECT_RATIO);
            if (width != 0
                    && (startEmulatorBitmap == null || startEmulatorBitmap.getWidth() != width)) {
                Bitmap icon = BitmapFactory.decodeResource(getResources(),
                        R.drawable.start_emulator_icon);
                startEmulatorBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                Canvas c = new Canvas(startEmulatorBitmap);
                c.drawColor(Color.BLACK);
                c.drawBitmap(icon, (int) ((width - icon.getWidth()) / 2.0),
                        (int) ((height - icon.getHeight()) / 2.0), null);
            }
            if (startEmulatorBitmap != null) {
                screenshot.setImageBitmap(startEmulatorBitmap);
            }
        }
        setMeasuredDimension(width, height);
        int mode = hasScreenshot ? MeasureSpec.EXACTLY : MeasureSpec.AT_MOST;
        measureChildren(MeasureSpec.makeMeasureSpec(width, mode),
                MeasureSpec.makeMeasureSpec(height, mode));
    }

    public void setScreenshotBitmap(Bitmap img) {
        if (img != null) {
            screenshot.setImageBitmap(img);
        }
        hasScreenshot = img != null;
        screenshot.setVisibility(hasScreenshot ? VISIBLE : GONE);
        spinner.setVisibility(hasScreenshot ? GONE : VISIBLE);
        requestLayout();
    }
}