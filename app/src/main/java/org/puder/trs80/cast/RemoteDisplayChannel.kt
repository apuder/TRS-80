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

package org.puder.trs80.cast

import org.puder.trs80.shared.configuration.Configuration

/**
 * Defines a protocol for communicating with a remote TRS80 display.
 */
interface RemoteDisplayChannel {
    /**
     * Session is being started. Causes the splash screen to disappear and the screen to show.
     */
    fun startSession()

    /**
     * Ends the session which will cause the monitor content to hide and the splash screen to
     * show.
     */
    fun endSession()

    /**
     * Updates the screen buffer. Only has an effect during an active session.
     *
     * @param expandedMode whether the buffer is to be displayed in expanded mode (wide
     * characters).
     * @param buffer the contents of the screen, row-priority.
     */
    fun sendScreenBuffer(expandedMode: Boolean, buffer: String)

    /**
     * Sends an initial configuration to the remote display.
     *
     * @param configuration the configuration of the emulator currently running or about to be
     * run. Contains information such as the background and foreground colors.
     */
    fun sendConfiguration(configuration: Configuration)
}
