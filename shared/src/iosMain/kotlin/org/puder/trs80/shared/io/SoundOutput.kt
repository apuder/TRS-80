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

package org.puder.trs80.shared.io

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import org.puder.trs80.shared.Log
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.Foundation.NSError

private const val TAG = "SoundOutput"

/**
 * Tells iOS that the machine's sound is what the app is for, so that it plays
 * with the phone in Silent mode.
 *
 * An app that never says gets the category meant for incidental sound, and
 * Silent mode mutes that -- whether it was set by the switch, the Action button
 * or Control Center. Android has nothing that mutes a game, so the same machine
 * that talked there said nothing on an iPhone, and nothing on the phone said
 * why. Playback is the category a video player uses. Somebody who wants a
 * machine quiet has its own mute.
 *
 * Only the category is set here. The session goes live when a machine first
 * makes a sound and the core starts its audio queue, so opening the app does
 * not stop whatever the user was already listening to.
 */
@OptIn(ExperimentalForeignApi::class)
fun playSoundInSilentMode() = memScoped {
    val error = alloc<ObjCObjectVar<NSError?>>()
    if (!AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryPlayback, error.ptr)) {
        Log.e(TAG, "Could not set the audio category: ${error.value?.localizedDescription}")
    }
}
