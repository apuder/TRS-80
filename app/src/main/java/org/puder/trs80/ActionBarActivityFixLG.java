package org.puder.trs80;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.ViewConfiguration;

import java.lang.reflect.Field;

/**
 * Fix for: https://code.google.com/p/android/issues/detail?id=78154
 */
@SuppressLint("Registered")
public class ActionBarActivityFixLG extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (isMenuWorkaroundRequired()) {
            forceOverflowMenu();
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return (keyCode == KeyEvent.KEYCODE_MENU && isMenuWorkaroundRequired())
                || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU && isMenuWorkaroundRequired()) {
            openOptionsMenu();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    public static boolean isMenuWorkaroundRequired() {
        return VERSION.SDK_INT < VERSION_CODES.KITKAT
                && VERSION.SDK_INT > VERSION_CODES.GINGERBREAD_MR1
                && ("LGE".equalsIgnoreCase(Build.MANUFACTURER) || "E6710"
                        .equalsIgnoreCase(Build.DEVICE));
    }

    /**
     * Modified from: http://stackoverflow.com/a/13098824
     */
    private void forceOverflowMenu() {
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (/*IllegalAccessException | NoSuchFieldException*/ Exception e) {
            // Log.w(TAG, "Failed to force overflow menu.");
        }
    }
}
