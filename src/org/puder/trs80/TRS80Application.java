package org.puder.trs80;

import android.app.Application;
import android.content.Context;

public class TRS80Application extends Application {

    private static Context context;
    private static Memory  memory;

    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    public static Context getAppContext() {
        return context;
    }

    public static void setMemory(Memory mem) {
        memory = mem;
    }

    public static Memory getMemory() {
        return memory;
    }
}
