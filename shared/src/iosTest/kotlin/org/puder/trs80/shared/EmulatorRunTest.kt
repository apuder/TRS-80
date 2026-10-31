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

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * What stopping a machine promises the next one: that the last run loop is
 * gone, and that waiting for it cannot hang the app.
 *
 * The core is a stand-in whose loop does only what each test needs, so these
 * are about [runMachine] and nothing under it.
 */
class EmulatorRunTest {

    /**
     * A core whose run loop comes back [lingerMillis] after it is stopped, or
     * never comes back until [released] if [stuck].
     */
    private class StandInCore(
        private val lingerMillis: Long = 0,
        private val stuck: Boolean = false,
    ) : EmulatorCore by IosEmulatorCore {
        private val stopped = CompletableDeferred<Unit>()
        val released = CompletableDeferred<Unit>()
        val returned = CompletableDeferred<Unit>()

        override fun run() = runBlocking {
            stopped.await()
            if (stuck) {
                released.await()
            }
            delay(lingerMillis)
            returned.complete(Unit)
            Unit
        }

        override fun stop() {
            stopped.complete(Unit)
        }
    }

    @Test
    fun stoppingWaitsForTheLoopToComeBack() {
        // Slow to come back, as a loop asleep between timer ticks is.
        val core = StandInCore(lingerMillis = 200)
        val stop = runMachine(core)

        stop()

        // The next machine boots as soon as stopping returns, so the loop has
        // to be gone by then, not merely asked to go.
        assertTrue(core.returned.isCompleted, "Stopping returned before the run loop did.")
    }

    @Test
    fun aLoopThatNeverComesBackDoesNotHangStopping() = runBlocking {
        val core = StandInCore(stuck = true)
        val stop = runMachine(core)

        val stopping = async(Dispatchers.Default) { stop() }
        val finished = withTimeoutOrNull(STOPPING_DEADLINE_MILLIS) { stopping.await() } != null
        // Whatever happened, let the loop go, so that nothing is left waiting.
        core.released.complete(Unit)

        assertTrue(finished, "Stopping waited for a loop that never came back.")
    }

    private companion object {
        /** Well past the second stopping allows itself, and well short of forever. */
        const val STOPPING_DEADLINE_MILLIS = 5_000L
    }
}
