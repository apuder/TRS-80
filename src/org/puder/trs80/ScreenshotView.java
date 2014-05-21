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
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class ScreenshotView extends ImageView {

    final private static float ASPECT_RATIO = 2.0f / 3.0f;

    public ScreenshotView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height;
        Drawable d = getDrawable();
        if (d != null) {
            height = (int) Math.ceil((float) width * (float) d.getIntrinsicHeight()
                    / (float) d.getIntrinsicWidth());
        } else {
            height = (int) (width * ASPECT_RATIO);
        }
        setMeasuredDimension(width, height);
    }

}