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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LibraryContentsTest {

    private fun card(
        name: String,
        lastUsed: Long = 0L,
        storeId: String? = null,
        isCustom: Boolean = false,
    ) = ConfigurationCard(
        id = name.hashCode(),
        name = name,
        model = "Model III",
        diskCount = 1,
        cassetteRewound = true,
        soundMuted = false,
        hasSavedState = false,
        hasXrayState = false,
        screenshot = null,
        isCustom = isCustom,
        lastUsed = lastUsed,
        storeId = storeId,
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
    fun searchMatchesCatalogByTitleOrAuthor() {
        val entries = listOf(app("Sea Dragon"), app("Breakdown")).asCatalog(emptyList())

        assertContentEquals(listOf("Sea Dragon"), entries.matchingEntries("sea").map { it.title })
        assertEquals(2, entries.matchingEntries("someone").size)
    }

    @Test
    fun catalogFindsTheCleanMachineByStoreId() {
        val entries = listOf(app("Breakdown", id = "b"), app("Cosmic Fighter", id = "c"))
            .asCatalog(installed = listOf(card("Breakdown", storeId = "b")))

        assertEquals("Breakdown".hashCode(), entries.single { it.id == "b" }.cleanId)
        assertNull(entries.single { it.id == "c" }.cleanId)
    }

    /** The whole reason the ID is stored: a rename must not break the link. */
    @Test
    fun renamingAMachineKeepsItLinkedToItsEntry() {
        val entries = listOf(app("Breakdown", id = "b"))
            .asCatalog(installed = listOf(card("Breakdown, my way", storeId = "b")))

        assertEquals("Breakdown, my way".hashCode(), entries.single().cleanId)
    }

    /** A machine with the same name but from nowhere is not the entry's. */
    @Test
    fun aMachineWithoutAStoreIdIsNobodysCopy() {
        val entries = listOf(app("Breakdown", id = "b"))
            .asCatalog(installed = listOf(card("Breakdown")))

        assertNull(entries.single().cleanId)
        assertTrue(entries.single().versions.isEmpty())
    }

    /** Editing a machine takes it out of the clean slot and into the list. */
    @Test
    fun anEditedMachineBecomesAVersionRatherThanTheCleanCopy() {
        val entries = listOf(app("Breakdown", id = "b"))
            .asCatalog(installed = listOf(card("Breakdown", storeId = "b", isCustom = true)))

        val entry = entries.single()
        assertNull(entry.cleanId)
        assertContentEquals(listOf("Breakdown"), entry.versions.map { it.name })
    }

    @Test
    fun versionsComeMostRecentlyUsedFirst() {
        val entry = listOf(app("Breakdown", id = "b")).asCatalog(
            installed = listOf(
                card("Old", 100, storeId = "b", isCustom = true),
                card("Newest", 300, storeId = "b", isCustom = true),
                card("Middle", 200, storeId = "b", isCustom = true),
                card("Clean", 400, storeId = "b"),
            )
        ).single()

        assertEquals("Clean".hashCode(), entry.cleanId)
        assertContentEquals(listOf("Newest", "Middle", "Old"), entry.versions.map { it.name })
    }

    /** Adopting older installs can leave two clean machines; one has to win. */
    @Test
    fun theMostRecentlyUsedCleanMachineWins() {
        val entry = listOf(app("Breakdown", id = "b")).asCatalog(
            installed = listOf(
                card("First", 100, storeId = "b"),
                card("Later", 900, storeId = "b"),
            )
        ).single()

        assertEquals("Later".hashCode(), entry.cleanId)
    }

    @Test
    fun aSelectionIsWhatThePaneShows() {
        val holding = paneContentFor("b", listOf(card("Breakdown", 500)))

        assertEquals(PaneContent.Entry("b"), holding)
    }

    /** Nothing picked: the machine they were last in is why they opened the app. */
    @Test
    fun withNoSelectionThePaneOffersTheLastMachineRun() {
        val holding = paneContentFor(
            selectedId = null,
            yours = listOf(card("Old", 100), card("Newest", 300), card("Middle", 200)),
        )

        assertEquals("Newest", (holding as PaneContent.Resume).card.name)
    }

    /**
     * A machine that has never run is not somewhere to return to. The bundled
     * sample is exactly this on a first run, and offering to "resume" it would
     * be the app's first sentence to a new user.
     */
    @Test
    fun aMachineNeverRunIsNotWhereYouLeftOff() {
        val holding = paneContentFor(selectedId = null, yours = listOf(card("Bundled sample")))

        assertEquals(PaneContent.FirstRun, holding)
    }

    @Test
    fun anEmptyLibraryWithNoSelectionIsAFirstRun() {
        assertEquals(PaneContent.FirstRun, paneContentFor(null, emptyList()))
    }

    @Test
    fun catalogMarksWhatIsDownloadingNow() {
        val entries = listOf(app("Breakdown", id = "b"))
            .asCatalog(installed = emptyList(), installing = setOf("b"))

        assertTrue(entries.single().installing)
    }
}
