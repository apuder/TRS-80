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

import com.google.common.base.Optional
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import org.retrostore.ApiException
import org.retrostore.RetrostoreClient
import org.retrostore.client.common.proto.App
import org.retrostore.client.common.proto.MediaImage
import java.util.concurrent.Executor

/** The index of the first app the list request asks for. */
private const val PAGE_START = 0

/**
 * How many apps a single list request asks for. Note that there is no paging, so this is also the
 * hard upper bound on the number of apps the store screen can ever show.
 */
private const val PAGE_SIZE = 100

/**
 * Fetches data from the RetroStore.
 */
class DataFetcher private constructor(
        private val client: RetrostoreClient,
        private val requestExecutor: Executor) {

    private val appCache = mutableMapOf<String, App>()

    /**
     * Turns the synchronous API request into an asynchronous one and returns a listenable future
     * with the result. The apps are also added to the cache backing [getFromCache].
     */
    fun getAppsAsync(): ListenableFuture<List<App>> = submit {
        val apps: List<App> = client.fetchApps(PAGE_START, PAGE_SIZE)
        updateCache(apps)
        apps
    }

    /**
     * Asynchronously fetches and returns all media images associated with the app with the given
     * [appId].
     */
    fun fetchMediaImages(appId: String): ListenableFuture<List<MediaImage>> = submit {
        client.fetchMediaImages(appId)
    }

    /** @return The app with the given [id], or absent if it is not in the cache. */
    fun getFromCache(id: String): Optional<App> = Optional.fromNullable(appCache[id])

    /**
     * Runs [request] on the request executor and completes the returned future with its result, or
     * with the [ApiException] it failed with.
     */
    private fun <T> submit(request: () -> T): ListenableFuture<T> =
            SettableFuture.create<T>().also { future ->
                requestExecutor.execute {
                    try {
                        future.set(request())
                    } catch (e: ApiException) {
                        future.setException(e)
                    }
                }
            }

    private fun updateCache(apps: List<App>) {
        apps.forEach { appCache[it.id] = it }
    }

    companion object {
        private var instance: DataFetcher? = null

        /**
         * @return The shared fetcher, or absent if [initialize] has not been called yet. It might
         * be absent if the app got cleaned up but the details activity was resumed.
         */
        @JvmStatic
        fun get(): Optional<DataFetcher> = Optional.fromNullable(instance)

        /**
         * Creates the shared fetcher on the first call and returns it. Subsequent calls return the
         * existing instance and ignore [client] and [executor].
         */
        @JvmStatic
        fun initialize(client: RetrostoreClient, executor: Executor): DataFetcher =
                instance ?: DataFetcher(client, executor).also { instance = it }
    }
}
