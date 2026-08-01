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

package org.puder.trs80.shared.ui

import kotlinx.coroutines.test.runTest
import org.retrostore.client.common.proto.App
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CatalogTest {

    private fun app(name: String) = App(id = name, name = name)

    @Test
    fun fetchesOnceAndReusesWhatItGot() = runTest {
        var calls = 0
        val catalog = Catalog { calls++; listOf(app("Sea Dragon")) }

        catalog.loadOnce()
        catalog.loadOnce()

        assertEquals(1, calls)
        assertEquals(listOf("Sea Dragon"), assertIs<StoreState.Loaded>(catalog.state).apps.map { it.name })
    }

    @Test
    fun refreshingAsksAgain() = runTest {
        var calls = 0
        val catalog = Catalog { calls++; listOf(app("Sea Dragon")) }

        catalog.loadOnce()
        catalog.refresh()

        assertEquals(2, calls)
    }

    @Test
    fun aFailedFirstLoadSaysSo() = runTest {
        val catalog = Catalog { throw RuntimeException("no network") }

        catalog.loadOnce()

        assertEquals("no network", assertIs<StoreState.Failed>(catalog.state).message)
    }

    /**
     * Losing the catalog because the network dropped for a moment is worse
     * than showing one a few minutes old.
     */
    @Test
    fun aFailedRefreshKeepsTheCatalogItAlreadyHas() = runTest {
        var fail = false
        val catalog = Catalog {
            if (fail) throw RuntimeException("dropped") else listOf(app("Sea Dragon"))
        }

        catalog.loadOnce()
        fail = true
        catalog.refresh()

        assertEquals(
            listOf("Sea Dragon"),
            assertIs<StoreState.Loaded>(catalog.state).apps.map { it.name },
        )
    }

    @Test
    fun aFailedLoadCanBeRetried() = runTest {
        var fail = true
        val catalog = Catalog {
            if (fail) throw RuntimeException("dropped") else listOf(app("Sea Dragon"))
        }

        catalog.loadOnce()
        assertIs<StoreState.Failed>(catalog.state)

        fail = false
        catalog.refresh()

        assertTrue(catalog.state is StoreState.Loaded)
    }
}
