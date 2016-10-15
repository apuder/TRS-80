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

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.ndk.CrashlyticsNdk;

import org.puder.trs80.cast.CastMessageSender;
import org.puder.trs80.cast.RemoteCastScreen;

import io.fabric.sdk.android.Fabric;

public class TRS80Application extends Application {

    private static Context         context;
    private static boolean         hasCrashed = false;


    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        String chromcastAppId = this.getResources().getString(R.string.chromecast_app_id);
        CastMessageSender.initSingleton(chromcastAppId, context);
        RemoteCastScreen.initSingleton(CastMessageSender.get());
        boolean debug = getApplicationContext().getResources().getBoolean(R.bool.debug);
        if (!debug) {
            Fabric.with(this, new Crashlytics(), new CrashlyticsNdk());
        }
    }

    public static Context getAppContext() {
        return context;
    }

    public static boolean hasCrashed() {
        return hasCrashed;
    }

    public static void setCrashedFlag(boolean flag) {
        hasCrashed = flag;
    }
}
