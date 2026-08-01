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

package org.puder.trs80.shared.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Everywhere the app can be.
 *
 * These are the screens the Android app has today, as Activities: the same set
 * and the same arrangement, because the redesign restyles rather than
 * rearranges. What changes is that a destination is now a value — something that
 * can be put in a list, compared, and restored — instead of a `Class` handed to
 * an `Intent`.
 *
 * Not here: the file browser, which is deleted rather than ported (§7.5), and
 * the outward-facing intents — the Play Store, the community link, sharing —
 * which leave the app rather than navigating within it.
 */
@Serializable
sealed interface Destination : NavKey {

    /**
     * What the user has and what the store offers, on one screen. The root.
     *
     * Replaces the separate configuration list and store browser: the visual
     * spec puts both on one screen, so they are one destination.
     */
    @Serializable
    data object Library : Destination

    /** A running machine. */
    @Serializable
    data class Emulator(val configurationId: Int) : Destination

    /**
     * The editor for one configuration.
     *
     * @property isNew whether the configuration was created to be edited, which
     * decides whether the list animates an insertion or an update when this
     * returns.
     */
    @Serializable
    data class EditConfiguration(
        val configurationId: Int,
        val isNew: Boolean = false,
    ) : Destination

    /** The app's own settings. */
    @Serializable
    data object Settings : Destination

    /** One app in the RetroStore catalog. */

    /** The blank-disk-image creator. */
    @Serializable
    data object CreateDisk : Destination
}

/**
 * What a destination hands back to whatever opened it.
 *
 * Only three things travel backwards, and each is an action the *caller* has to
 * take rather than a value it wants — which is why this is a result at all
 * instead of the caller simply re-reading the domain when it resumes.
 */
sealed interface NavigationResult {

    /**
     * The editor was left by going back, which keeps the edits.
     *
     * The caller drops its backup, throws away any saved emulator state — the
     * machine may now have different disks — and rewinds the cassette.
     */
    data class ConfigurationEdited(val configurationId: Int, val isNew: Boolean) : NavigationResult

    /**
     * The editor was left through Cancel, which discards the edits.
     *
     * The caller restores the backup it took before opening the editor. It has
     * to, because the editor writes every change straight into storage as it is
     * made: there is nothing to *not* commit. That is worth fixing when the
     * editor is redesigned, but it is the behavior being ported, so it is the
     * behavior modeled here.
     */
    data class ConfigurationEditCanceled(val configurationId: Int) : NavigationResult

    /** A blank disk image was created at this path. */
    data class DiskCreated(val path: String) : NavigationResult
}
