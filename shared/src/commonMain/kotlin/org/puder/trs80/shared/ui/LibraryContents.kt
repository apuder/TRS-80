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
 * Turns the store's apps into catalogue entries, marking the ones already
 * installed.
 *
 * "Already installed" is matched on name, which is what the app has to go on:
 * a configuration records what it was made from only by the name it was given.
 * Matching on the store's ID would be exact, and wants a field on the
 * configuration that does not exist yet.
 */
fun List<App>.asCatalogue(
    installed: List<ConfigurationCard>,
    installing: Set<String> = emptySet(),
): List<CatalogueEntry> {
    val names = installed.map { it.name.trim().lowercase() }.toSet()
    return map { app ->
        CatalogueEntry(
            id = app.id,
            title = app.name,
            author = app.author,
            year = app.release_year,
            artUrl = app.screenshot_url.firstOrNull(),
            installed = app.name.trim().lowercase() in names,
            installing = app.id in installing,
        )
    }
}

/** Keeps the catalogue entries matching [query]; everything when it is blank. */
fun List<CatalogueEntry>.matchingEntries(query: String): List<CatalogueEntry> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) {
        return this
    }
    return filter {
        it.title.contains(trimmed, ignoreCase = true) ||
            it.author.contains(trimmed, ignoreCase = true)
    }
}
