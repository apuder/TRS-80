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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers the version reaching the code that shows it.
 *
 * Not the value -- that is set in gradle.properties and changes every release --
 * but the wiring: a build that stops generating the constants, or generates them
 * empty, produces something like " ()" and nobody notices until a user reads it
 * back off the settings screen.
 */
class AppVersionTest {

    @Test
    fun theVersionIsANumberAndABuild() {
        val version = appVersion()

        assertTrue(
            Regex("""^\d+(\.\d+)*\s\(\d+\)$""").matches(version),
            "Not a version: \"$version\"",
        )
    }

    @Test
    fun itIsTheOneTheBuildWasGiven() {
        assertEquals("$VERSION_NAME ($VERSION_CODE)", appVersion())
        assertTrue(VERSION_CODE > 0, "Play orders releases by this, so it counts.")
        assertTrue(VERSION_NAME.isNotBlank())
    }
}
