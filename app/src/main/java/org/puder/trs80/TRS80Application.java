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

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.ndk.CrashlyticsNdk;
import io.fabric.sdk.android.Fabric;
import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.puder.trs80.cast.CastMessageSender;
import org.puder.trs80.cast.RemoteCastScreen;
import org.puder.trs80.keyboard.KeyboardManager;

import java.util.HashMap;

@ReportsCrashes(formUri = "")
public class TRS80Application extends Application {

    private static Context         context;
    private static Hardware        hardware;
    private static KeyboardManager keyboard;
    private static Configuration   configuration;
    private static Bitmap          screenshot;
    private static boolean         hasCrashed = false;


    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics(), new CrashlyticsNdk());
        context = getApplicationContext();
        String chromcastAppId = this.getResources().getString(R.string.chromecast_app_id);
        CastMessageSender.initSingleton(chromcastAppId, context);
        RemoteCastScreen.initSingleton(CastMessageSender.get());
        boolean debug = getApplicationContext().getResources().getBoolean(R.bool.debug);
        if (!debug) {
            ACRA.init(this);
            HashMap<String, String> ACRAData = new HashMap<>();
            ACRA.getErrorReporter().setReportSender(new ACRAPostSender(ACRAData));
        }
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

    public static void setKeyboardManager(KeyboardManager theKeyboard) {
        keyboard = theKeyboard;
    }

    public static KeyboardManager getKeyboardManager() {
        return keyboard;
    }

    public static boolean hasCrashed() {
        return hasCrashed;
    }

    public static void setCrashedFlag(boolean flag) {
        hasCrashed = flag;
    }
}
