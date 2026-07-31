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

import org.retrostore.client.common.proto.App
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LibraryContentsTest {

    private fun card(name: String, lastUsed: Long = 0L) = ConfigurationCard(
        id = name.hashCode(),
        name = name,
        model = "Model III",
        diskCount = 1,
        cassetteRewound = true,
        soundMuted = false,
        keyboardPortrait = "Orig",
        keyboardLandscape = "Orig",
        hasSavedState = false,
        hasXrayState = false,
        screenshot = null,
        lastUsed = lastUsed,
    )

    private fun app(name: String, id: String = name) =
        App(id = id, name = name, author = "Someone", release_year = 1982)

    @Test
    fun lastUsedPutsTheMostRecentFirst() {
        val sorted = listOf(card("Old", 100), card("Newest", 300), card("Middle", 200))
            .sortedFor(LibrarySort.LastUsed)

        assertContentEquals(listOf("Newest", "Middle", "Old"), sorted.map { it.name })
    }

    /** Never-run machines have no timestamp, and must not scatter. */
    @Test
    fun neverRunMachinesFallToTheEndInNameOrder() {
        val sorted = listOf(card("Zeta"), card("Used", 500), card("Alpha"))
            .sortedFor(LibrarySort.LastUsed)

        assertContentEquals(listOf("Used", "Alpha", "Zeta"), sorted.map { it.name })
    }

    @Test
    fun alphabeticalIgnoresCase() {
        val sorted = listOf(card("zeta"), card("Alpha"), card("beta"))
            .sortedFor(LibrarySort.Alphabetical)

        assertContentEquals(listOf("Alpha", "beta", "zeta"), sorted.map { it.name })
    }

    @Test
    fun searchMatchesMachinesByName() {
        val cards = listOf(card("Sea Dragon"), card("Breakdown"))

        assertContentEquals(listOf("Sea Dragon"), cards.matching("dragon").map { it.name })
        assertEquals(2, cards.matching("   ").size)
    }

    @Test
    fun searchMatchesCatalogueByTitleOrAuthor() {
        val entries = listOf(app("Sea Dragon"), app("Breakdown")).asCatalogue(emptyList())

        assertContentEquals(listOf("Sea Dragon"), entries.matchingEntries("sea").map { it.title })
        assertEquals(2, entries.matchingEntries("someone").size)
    }

    /** An app already installed offers play rather than download. */
    @Test
    fun catalogueMarksWhatIsAlreadyInstalled() {
        val entries = listOf(app("Breakdown"), app("Cosmic Fighter"))
            .asCatalogue(installed = listOf(card("breakdown ")))

        assertTrue(entries.single { it.title == "Breakdown" }.installed)
        assertFalse(entries.single { it.title == "Cosmic Fighter" }.installed)
    }

    @Test
    fun catalogueMarksWhatIsDownloadingNow() {
        val entries = listOf(app("Breakdown", id = "b"))
            .asCatalogue(installed = emptyList(), installing = setOf("b"))

        assertTrue(entries.single().installing)
    }
}
