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

/**
 * Orders the user's own machines.
 *
 * Last-used first is the default because it is what the screen is for: the
 * thing you were doing is the thing you are most likely to want again. Machines
 * that have never run have no timestamp, so they fall to the end in name order
 * rather than sorting arbitrarily among themselves.
 */
fun List<ConfigurationCard>.sortedFor(sort: LibrarySort): List<ConfigurationCard> = when (sort) {
    LibrarySort.Alphabetical -> sortedBy { it.name.lowercase() }
    LibrarySort.LastUsed -> sortedWith(
        compareByDescending<ConfigurationCard> { it.lastUsed }
            .thenBy { it.name.lowercase() }
    )
}

/** Keeps the machines whose name matches [query]; everything when it is blank. */
fun List<ConfigurationCard>.matching(query: String): List<ConfigurationCard> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) {
        return this
    }
    return filter { it.name.contains(trimmed, ignoreCase = true) }
}

/**
 * Turns the store's apps into catalog entries, each carrying the machines the
 * user has made from it.
 *
 * Matched on the store's ID, which every machine installed from the catalog now
 * records. Names were what this used to match on, and they cannot answer the
 * question: two machines called the same thing resolve to whichever came last,
 * and renaming one breaks the link entirely.
 *
 * The split is what the entry's screen is built on. Exactly one machine can be
 * the entry's clean copy — carrying its ID and never edited — and every other
 * machine carrying that ID is a version of the user's own. Editing the clean one
 * makes it a version and leaves the entry without a clean copy, which is what
 * lets the next Play make a fresh one rather than handing back a modified
 * machine.
 */
fun List<App>.asCatalog(
    installed: List<ConfigurationCard>,
    installing: Set<String> = emptySet(),
): List<CatalogEntry> {
    val mine = installed.filter { it.storeId != null }.groupBy { it.storeId }
    return map { app ->
        val fromThis = mine[app.id].orEmpty()
        CatalogEntry(
            id = app.id,
            title = app.name,
            author = app.author,
            year = app.release_year,
            artUrl = app.screenshot_url.firstOrNull(),
            // Normally there is at most one, since a clean machine is only ever
            // made when the entry has none. Adopting older installs can leave
            // two, so the most recently used wins rather than the first found.
            cleanId = fromThis.filterNot { it.isCustom }.maxByOrNull { it.lastUsed }?.id,
            versions = fromThis.filter { it.isCustom }
                .sortedWith(
                    compareByDescending<ConfigurationCard> { it.lastUsed }
                        .thenBy { it.name.lowercase() }
                )
                .map { CatalogVersion(id = it.id, name = it.name, model = it.model) },
            installing = app.id in installing,
        )
    }
}

/** Keeps the catalog entries matching [query]; everything when it is blank. */
fun List<CatalogEntry>.matchingEntries(query: String): List<CatalogEntry> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) {
        return this
    }
    return filter {
        it.title.contains(trimmed, ignoreCase = true) ||
            it.author.contains(trimmed, ignoreCase = true)
    }
}
