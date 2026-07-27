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

package org.puder.trs80

import android.app.Application
import android.content.Context
import org.puder.trs80.cast.CastMessageSender
import org.puder.trs80.cast.RemoteCastScreen

/**
 * Application entry point. Publishes the application context and wires up the Cast singletons.
 */
class TRS80Application : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        CastMessageSender.initSingleton(getString(R.string.chromecast_app_id), appContext)
        RemoteCastScreen.initSingleton(CastMessageSender.get())
    }

    companion object {
        private lateinit var appContext: Context
        private var crashed = false

        /**
         * Returns the process-wide application context.
         *
         * Kept as a method rather than a property so the existing `getAppContext()` call sites in
         * Java keep working.
         */
        @JvmStatic
        fun getAppContext(): Context = appContext

        /** Whether the emulator crashed since the flag was last cleared. */
        @JvmStatic
        fun hasCrashed(): Boolean = crashed

        /** Records whether the emulator has crashed. */
        @JvmStatic
        fun setCrashedFlag(flag: Boolean) {
            crashed = flag
        }
    }
}
