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

package org.puder.trs80.shared.navigation

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NavigatorTest {

    private val navigator = Navigator()

    @Test
    fun startsAtTheLibrary() {
        assertEquals(Destination.Library, navigator.current)
        assertFalse(navigator.canGoBack)
    }

    @Test
    fun goingSomewhereAndBackAgain() {
        navigator.goTo(Destination.Settings)

        assertEquals(Destination.Settings, navigator.current)
        assertTrue(navigator.canGoBack)

        assertTrue(navigator.goBack())
        assertEquals(Destination.Library, navigator.current)
    }

    /** The host needs a false here to hand Back to the platform. */
    @Test
    fun goingBackFromTheRootDoesNothing() {
        assertFalse(navigator.goBack())
        assertContentEquals(listOf(Destination.Library), navigator.backStack)
    }

    @Test
    fun destinationsCarryTheirArguments() {
        navigator.goTo(Destination.Emulator(configurationId = 7))

        assertEquals(Destination.Emulator(7), navigator.current)
    }

    /**
     * Two taps landing before the first has drawn must not stack two copies --
     * the user would have to dismiss the same screen twice.
     */
    @Test
    fun askingForWhereItAlreadyIsDoesNothing() {
        navigator.goTo(Destination.Settings)
        navigator.goTo(Destination.Settings)

        assertEquals(2, navigator.backStack.size)
    }

    /** The same screen with different arguments is a different screen. */
    @Test
    fun theSameScreenWithOtherArgumentsStacks() {
        navigator.goTo(Destination.RetroStoreApp("a"))
        navigator.goTo(Destination.RetroStoreApp("b"))

        assertEquals(3, navigator.backStack.size)
        assertEquals(Destination.RetroStoreApp("b"), navigator.current)
    }

    @Test
    fun aResultReachesTheCallerExactlyOnce() {
        navigator.goTo(Destination.EditConfiguration(configurationId = 3, isNew = true))

        navigator.goBack(NavigationResult.ConfigurationEdited(configurationId = 3, isNew = true))

        assertEquals(Destination.Library, navigator.current)
        assertEquals(
            NavigationResult.ConfigurationEdited(3, isNew = true),
            navigator.takeResult(),
        )
        assertNull(navigator.takeResult())
    }

    /** Leaving without one must not deliver the previous screen's result again. */
    @Test
    fun goingBackWithoutAResultClearsTheLastOne() {
        navigator.goTo(Destination.EditConfiguration(1))
        navigator.goBack(NavigationResult.ConfigurationEditCancelled(1))
        assertEquals(NavigationResult.ConfigurationEditCancelled(1), navigator.takeResult())

        navigator.goTo(Destination.Settings)
        navigator.goBack()

        assertNull(navigator.takeResult())
    }

    @Test
    fun goingBackToTheRootDropsEverythingAbove() {
        navigator.goTo(Destination.RetroStoreApp("sea-dragon"))
        navigator.goTo(Destination.EditConfiguration(2, isNew = true))

        navigator.goBackToRoot()

        assertContentEquals(listOf(Destination.Library), navigator.backStack)
        assertFalse(navigator.canGoBack)
    }

    @Test
    fun goingBackToTheRootFromTheRootIsHarmless() {
        navigator.goBackToRoot()

        assertContentEquals(listOf(Destination.Library), navigator.backStack)
    }

    /**
     * The whole edit flow as the app performs it: open the editor for a new
     * configuration, cancel, and the list is told to restore its backup.
     */
    @Test
    fun theEditThenCancelFlow() {
        navigator.goTo(Destination.EditConfiguration(configurationId = 9, isNew = true))
        assertEquals(Destination.EditConfiguration(9, isNew = true), navigator.current)

        navigator.goBack(NavigationResult.ConfigurationEditCancelled(9))

        assertEquals(Destination.Library, navigator.current)
        assertEquals(NavigationResult.ConfigurationEditCancelled(9), navigator.takeResult())
    }

    /**
     * Opening a store app from the library and coming back lands on the library,
     * which is now where the catalogue lives.
     */
    @Test
    fun theRetroStoreBrowseFlow() {
        navigator.goTo(Destination.RetroStoreApp("rear-guard"))
        navigator.goTo(Destination.Emulator(4))

        navigator.goBack()

        assertEquals(Destination.RetroStoreApp("rear-guard"), navigator.current)
        assertTrue(navigator.canGoBack)

        navigator.goBack()
        assertEquals(Destination.Library, navigator.current)
    }
}
