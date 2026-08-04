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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * What decides whether opening the tutorial machine starts the tour.
 *
 * It has to be per machine and it has to survive the app being closed, because
 * the thing it prevents -- the tour restarting a machine out from under work
 * already done on it -- happens on the way back in, days later.
 */
class TutorialHistoryTest {

    @Test
    fun aMachineThatHasNeverRunItStartsIt() {
        val history = TutorialHistory(MapSettings())

        assertFalse(history.hasRun(7))
    }

    @Test
    fun aMachineIsRememberedOnceItHas() {
        val history = TutorialHistory(MapSettings())

        history.markRun(7)

        assertTrue(history.hasRun(7))
    }

    /** One machine's tour is not another's: a copy starts fresh. */
    @Test
    fun eachMachineIsRememberedSeparately() {
        val history = TutorialHistory(MapSettings())

        history.markRun(7)

        assertFalse(history.hasRun(8))
    }

    /** The record outlives the object holding it, because the store does. */
    @Test
    fun itIsRememberedAcrossSessions() {
        val settings = MapSettings()
        TutorialHistory(settings).markRun(7)

        assertTrue(TutorialHistory(settings).hasRun(7))
    }
}
