/*
 * Copyright 2014, Sascha Haeberling
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

import org.puder.trs80.configuration.Configuration
import java.util.Locale

private const val TYPE_START_SESSION = 1
private const val TYPE_END_SESSION = 2
private const val TYPE_SEND_SCREEN_BUFFER = 3
private const val TYPE_SEND_CONFIGURATION = 4
private const val MESSAGE_FORMAT = "%d:%s"
private const val SCREEN_BUFFER_FORMAT = "%s:%s"

/**
 * A remote display channel that controls a remote screen using the Google Cast protocol.
 */
class RemoteCastScreen private constructor(private val sender: CastMessageSender) :
    RemoteDisplayChannel {

    override fun startSession() = sendMessage(TYPE_START_SESSION)

    override fun endSession() = sendMessage(TYPE_END_SESSION)

    override fun sendScreenBuffer(expandedMode: Boolean, buffer: String) = sendMessage(
        TYPE_SEND_SCREEN_BUFFER,
        String.format(Locale.US, SCREEN_BUFFER_FORMAT, if (expandedMode) "1" else "0", buffer),
        wait = true
    )

    override fun sendConfiguration(configuration: Configuration) = sendMessage(
        TYPE_SEND_CONFIGURATION,
        colorToWebFormat(configuration.characterColorAsRGB) + ':' +
                colorToWebFormat(configuration.screenColorAsRGB)
    )

    private fun sendMessage(type: Int, payload: Any? = null, wait: Boolean = false) {
        if (!sender.isReadyToSend) {
            return
        }
        sender.sendMessage(createMessage(type, payload), wait)
    }

    companion object {
        private lateinit var instance: RemoteCastScreen

        /** Creates the process-wide instance that [get] hands out. */
        @JvmStatic
        fun initSingleton(sender: CastMessageSender) {
            instance = RemoteCastScreen(sender)
        }

        /** Returns the singleton created by [initSingleton]. */
        @JvmStatic
        fun get(): RemoteDisplayChannel = instance
    }
}

private fun createMessage(type: Int, payload: Any?) =
    String.format(Locale.US, MESSAGE_FORMAT, type, payload?.toString() ?: "")

private fun colorToWebFormat(color: Int) = String.format("#%06X", 0xFFFFFF and color)
