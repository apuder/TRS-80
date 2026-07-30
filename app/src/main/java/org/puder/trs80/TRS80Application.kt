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
import org.puder.trs80.shared.storage.ImportResult
import org.puder.trs80.storage.AppStorage

/**
 * Application entry point. Publishes the application context, brings the stored
 * data up to date, and wires up the Cast singletons.
 */
class TRS80Application : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext

        // Before anything reads a configuration. Held so that MainActivity can
        // tell the user if it failed — Application has nowhere to show that.
        legacyImportResult = AppStorage.init(appContext).importLegacyDataIfNeeded()

        CastMessageSender.initSingleton(getString(R.string.chromecast_app_id), appContext)
        RemoteCastScreen.initSingleton(CastMessageSender.get())
    }

    companion object {
        private lateinit var appContext: Context
        private var crashed = false

        /**
         * What the startup import did, for whoever is in a position to report it.
         *
         * Cleared once read, so a failure is shown once rather than on every
         * return to the configuration list.
         */
        private var legacyImportResult: ImportResult? = null

        /** @return the startup import's outcome, the first time it is asked for. */
        fun consumeLegacyImportResult(): ImportResult? =
            legacyImportResult.also { legacyImportResult = null }

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
