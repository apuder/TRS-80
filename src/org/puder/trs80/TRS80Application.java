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
