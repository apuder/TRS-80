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

package org.puder.trs80.shared

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * How long stopping waits for the run loop to come back. It takes at most a
 * timer tick, 25 or 33 ms, which is how long the loop sleeps between bursts.
 */
private const val STOP_WAIT_MILLIS = 1_000L

/** A thread of its very own; see the expect for why not a pooled one. */
@OptIn(DelicateCoroutinesApi::class)
actual fun runMachine(core: EmulatorCore): () -> Unit {
    val cpu = newSingleThreadContext("trs80-cpu")
    val machine = CoroutineScope(cpu).launch { core.run() }
    return {
        core.stop()
        // The next machine boots into the same core, so this one has to be
        // gone first: booting under a loop that is still executing resets its
        // state from beneath it, and it closes the audio the next machine has
        // just opened. Bounded, so that a loop that never comes back costs a
        // second rather than the app.
        val returned = runBlocking {
            withTimeoutOrNull(STOP_WAIT_MILLIS) { machine.join() } != null
        }
        // Not if it is stuck: on iOS, closing waits for the thread, without a
        // limit. A loop that did not come back is left to itself instead.
        if (returned) {
            cpu.close()
        }
    }
}
