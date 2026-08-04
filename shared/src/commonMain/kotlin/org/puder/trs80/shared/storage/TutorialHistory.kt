/*
 * Copyright The TRS-80 App Authors
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

package org.puder.trs80.shared.storage

import com.russhwolf.settings.Settings

/**
 * Which machines have been taken through the tour.
 *
 * The tour starts itself the first time its machine is opened, because somebody
 * who opened that machine came for it. The second time is a different matter:
 * they have been through it, or started it and stopped, and either way the
 * machine now has whatever they did on it. Starting the tour again would type a
 * DIR over the top of that -- and, since the tour needs the machine at the DOS
 * prompt, restart the machine to do it.
 *
 * So it is remembered, per machine, and after the first time the tour is
 * something asked for from the menu rather than something that happens.
 */
class TutorialHistory(private val settings: Settings) {

    /** Whether the tour has already been started on the machine with [configurationId]. */
    fun hasRun(configurationId: Int): Boolean =
        settings.getBoolean(key(configurationId), false)

    /** Records that it has. Idempotent. */
    fun markRun(configurationId: Int) =
        settings.putBoolean(key(configurationId), true)

    private fun key(configurationId: Int) =
        StorageKeys.configuration(configurationId, StorageKeys.CONFIG_TUTORIAL_RUN)
}
