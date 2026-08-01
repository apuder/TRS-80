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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.puder.trs80.shared.Log
import org.puder.trs80.shared.store.retroStore
import org.retrostore.client.common.proto.App

private const val TAG = "Catalog"

/**
 * How many entries to ask the store for.
 *
 * The whole catalog is a few dozen programs and fits comfortably; if it ever
 * outgrows this the list will simply stop at the limit, so raise it or page.
 */
private const val CATALOG_LIMIT = 500

/**
 * The store's catalog, held for as long as the app is running.
 *
 * Held above the library rather than inside it, because the library is
 * composed and disposed every time a machine is run and come back from —
 * fetching there meant a network round trip and a visibly empty list on every
 * return, for a catalog that changes perhaps monthly.
 *
 * @param fetch how to get the catalog; replaceable so this can be tested
 * without a network.
 */
class Catalog(
    private val fetch: suspend () -> List<App> = {
        retroStore.fetchApps(0, CATALOG_LIMIT)
    },
) {
    var state: StoreState by mutableStateOf(StoreState.Loading)
        private set

    /** Whether a fetch is in flight, so the refresh control can say so. */
    var refreshing: Boolean by mutableStateOf(false)
        private set

    /** Fetches the first time only; later visits to the library reuse this. */
    suspend fun loadOnce() {
        if (state is StoreState.Loaded || refreshing) {
            return
        }
        load()
    }

    /** Fetches again, at the user's asking. */
    suspend fun refresh() {
        if (refreshing) {
            return
        }
        load()
    }

    private suspend fun load() {
        refreshing = true
        try {
            state = StoreState.Loaded(withContext(Dispatchers.Default) { fetch() })
        } catch (e: Exception) {
            Log.e(TAG, "Could not fetch the store catalog.", e)
            // A catalog already on screen stays there. Losing the list because
            // the network dropped for a moment would be worse than showing one
            // that is a few minutes old, and the machines the user has installed
            // are read from the device and never depended on this at all.
            if (state !is StoreState.Loaded) {
                state = StoreState.Failed(e.message.orEmpty())
            }
        } finally {
            refreshing = false
        }
    }
}
