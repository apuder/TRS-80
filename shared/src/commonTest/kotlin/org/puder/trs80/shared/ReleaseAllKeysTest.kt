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
 * What [releaseAllKeys] sends, and why it has to be exactly that.
 *
 * The core reads a key release as "let go of whatever was last pressed at this
 * scancode", and for a scancode it has never seen anything pressed at, that
 * comes out as every key up. So the whole trick rests on the scancode being one
 * no key in the mapping uses -- which is what these two tests hold in place. If
 * a key is ever given the code zero, the first of them fails and this stops
 * being a way to clear the keyboard.
 */
class ReleaseAllKeysTest {

    @Test
    fun noKeyUsesTheScancodeThatMeansAllKeysUp() {
        val zeroes = KeyboardMapping.entries.filter { it.key == 0 }

        assertTrue(zeroes.isEmpty(), "These keys would be released instead of all of them: $zeroes")
    }

    @Test
    fun releasingEverythingSendsOneReleaseAtThatScancode() {
        val core = FakeEmulatorCore()

        core.releaseAllKeys()

        assertEquals(listOf(Triple(false, 0, 0)), core.keyEvents)
    }
}
