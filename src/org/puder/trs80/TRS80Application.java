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

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;

public class TRS80Application extends Application {

    private static Context       context;
    private static Hardware      hardware;
    private static Keyboard      keyboard;
    private static Configuration configuration;
    private static Bitmap        screenshot;

    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    public static Context getAppContext() {
        return context;
    }

    public static void setCurrentConfiguration(Configuration conf) {
        configuration = conf;
    }

    public static Configuration getCurrentConfiguration() {
        return configuration;
    }

    public static void setScreenshot(Bitmap s) {
        screenshot = s;
    }

    public static Bitmap getScreenshot() {
        return screenshot;
    }

    public static void setHardware(Hardware theHardware) {
        hardware = theHardware;
    }

    public static Hardware getHardware() {
        return hardware;
    }

    public static void setKeyboard(Keyboard theKeyboard) {
        keyboard = theKeyboard;
    }

    public static Keyboard getKeyboard() {
        return keyboard;
    }

    public static Typeface getTypeface() {
        return Typeface.createFromAsset(context.getAssets(), "fonts/DejaVuSansMono.ttf");
    }

    public static Typeface getTypefaceBold() {
        return Typeface.createFromAsset(context.getAssets(), "fonts/DejaVuSansMono-Bold.ttf");
    }
}
