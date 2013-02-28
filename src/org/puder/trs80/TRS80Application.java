package org.puder.trs80;

import android.app.Application;
import android.content.Context;

public class TRS80Application extends Application {

    private static Context context;

    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    public static Context getAppContext() {
        return context;
    }
}
