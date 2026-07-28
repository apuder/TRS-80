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

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Announces that a configuration's screenshot has been rewritten, so that any list showing it can
 * refresh that entry.
 *
 * The emulator saves its screenshot as it stops, which happens *after* the configuration list has
 * already resumed and drawn — so the list cannot simply re-read on resume and has to be told.
 *
 * This is an event rather than state: a [SharedFlow] with no replay, so a collector that starts
 * later does not see a stale notification.
 */
object ScreenshotEvents {

    private val screenshotTaken = MutableSharedFlow<Int>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** Emits the configuration ID whose screenshot was just saved. */
    val screenshots: SharedFlow<Int> = screenshotTaken.asSharedFlow()

    /** Announces a new screenshot for [configurationId]. Safe to call from any thread. */
    fun notifyScreenshotTaken(configurationId: Int) {
        screenshotTaken.tryEmit(configurationId)
    }
}
