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

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExperimentalFeaturesTest {

    private val settings = MapSettings()
    private val features = ExperimentalFeatures(settings)

    @Test
    fun everythingIsClosedUntilItIsFound() {
        assertFalse(features.isUnlocked)
        assertFalse(features.isShareEnabled)
    }

    /** Finding the door is not the same as walking through it. */
    @Test
    fun unlockingDoesNotTurnAnythingOn() {
        features.unlock()

        assertTrue(features.isUnlocked)
        assertFalse(features.isShareEnabled)
    }

    @Test
    fun aFeatureIsOnOnceUnlockedAndSwitchedOn() {
        features.unlock()
        features.setShareEnabled(true)

        assertTrue(features.isShareEnabled)
    }

    /**
     * A feature left on cannot outlive the section that switches it. Otherwise
     * a store wiped of the one flag but not the other would leave an
     * experimental feature running with nothing on screen to turn it off.
     */
    @Test
    fun aFeatureIsOffWhileTheSectionIsHidden() {
        features.setShareEnabled(true)

        assertFalse(features.isShareEnabled)
    }

    @Test
    fun tenTapsOpenIt() {
        val run = TapRun()

        repeat(TAPS_TO_UNLOCK - 1) { assertFalse(run.tap(it.toLong())) }

        assertTrue(run.tap(TAPS_TO_UNLOCK.toLong()))
    }

    /** Ten visits to the library over a month must not add up to a run. */
    @Test
    fun aPauseStartsTheCountAgain() {
        val run = TapRun()
        repeat(TAPS_TO_UNLOCK - 1) { assertFalse(run.tap(it.toLong())) }

        // Long enough after the last one that the run has lapsed.
        assertFalse(run.tap(TAP_RUN_TIMEOUT_MILLIS * 2))

        repeat(TAPS_TO_UNLOCK - 2) { assertFalse(run.tap(TAP_RUN_TIMEOUT_MILLIS * 2 + it)) }
        assertTrue(run.tap(TAP_RUN_TIMEOUT_MILLIS * 2 + TAPS_TO_UNLOCK))
    }

    /** Carrying on tapping must not report success over and over. */
    @Test
    fun aRunReportsSuccessOnce() {
        val run = TapRun()
        repeat(TAPS_TO_UNLOCK - 1) { run.tap(it.toLong()) }
        assertTrue(run.tap(TAPS_TO_UNLOCK.toLong()))

        repeat(TAPS_TO_UNLOCK - 1) {
            assertFalse(run.tap(TAPS_TO_UNLOCK + 1L + it), "tap ${it + 1} after the run")
        }
    }

    @Test
    fun theCountdownSaysHowManyAreLeft() {
        val run = TapRun()

        run.tap(0)
        assertEquals(TAPS_TO_UNLOCK - 1, run.remaining)
        run.tap(1)
        assertEquals(TAPS_TO_UNLOCK - 2, run.remaining)
    }
}
