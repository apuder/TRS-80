/*
 * Copyright 2017, Sascha Haeberling
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

package org.retrostore.android.net

import org.retrostore.RetrostoreClient
import org.retrostore.client.common.proto.App
import org.retrostore.client.common.proto.MediaImage

/** The index of the first app the list request asks for. */
private const val PAGE_START = 0

/**
 * How many apps a single list request asks for. Note that there is no paging, so this is also the
 * hard upper bound on the number of apps the store screen can ever show.
 */
private const val PAGE_SIZE = 100

/**
 * Fetches data from the RetroStore.
 *
 * The requests run on the IO dispatcher and are safe to call from the main thread.
 */
class DataFetcher private constructor(private val client: RetrostoreClient) {

    private val appCache = mutableMapOf<String, App>()

    /**
     * Fetches the list of apps. The apps are also added to the cache backing [getFromCache].
     *
     * @throws org.retrostore.ApiException if the request fails.
     */
    suspend fun getAppsAsync(): List<App> {
        val apps = client.fetchApps(PAGE_START, PAGE_SIZE)
        updateCache(apps)
        return apps
    }

    /**
     * Fetches and returns all media images associated with the app with the given [appId].
     *
     * @throws org.retrostore.ApiException if the request fails.
     */
    suspend fun fetchMediaImages(appId: String): List<MediaImage> = client.fetchMediaImages(appId)

    /** @return The app with the given [id], or null if it is not in the cache. */
    fun getFromCache(id: String): App? = appCache[id]

    private fun updateCache(apps: List<App>) {
        apps.forEach { appCache[it.id] = it }
    }

    companion object {
        private var instance: DataFetcher? = null

        /**
         * @return The shared fetcher, or null if [initialize] has not been called yet. It might be
         * null if the app got cleaned up but the details activity was resumed.
         */
        @JvmStatic
        fun get(): DataFetcher? = instance

        /**
         * Creates the shared fetcher on the first call and returns it. Subsequent calls return the
         * existing instance and ignore [client].
         */
        @JvmStatic
        fun initialize(client: RetrostoreClient): DataFetcher =
                instance ?: DataFetcher(client).also { instance = it }
    }
}
