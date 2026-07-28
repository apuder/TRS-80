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

package org.retrostore.android

import android.util.Log
import org.retrostore.ApiException
import org.retrostore.RetrostoreClient
import org.retrostore.client.common.proto.App
import org.retrostore.client.common.proto.SystemState

private const val TAG = "RetrostoreApi"

/**
 * Main API interface for an Android app to interface with the RetroStore Android components.
 */
object RetrostoreApi {
    private val retrostoreClient: RetrostoreClient = RetrostoreClient.default
    private var installListener: AppInstallListener? = null

    init {
        RetrostoreActivity.setAppInstallListener(
                AppInstallListener { app -> installListener?.onInstallApp(app) })
    }

    /**
     * Returns the singleton instance.
     *
     * Kept as a method so the existing `RetrostoreApi.get()` call sites in Java keep working.
     */
    @JvmStatic
    fun get(): RetrostoreApi = this

    /** Registers [listener] to be notified whenever the user asks for an app to be installed. */
    fun registerAppInstallListener(listener: AppInstallListener) {
        installListener = listener
    }

    /**
     * Downloads all aspects of an app package. The network calls run on the IO dispatcher, so this
     * is safe to call from the main thread.
     *
     * @param appId the ID of the app to download. Must not be empty.
     * @return The complete package, or null if it could not be downloaded.
     */
    suspend fun downloadApp(appId: String): AppPackage? {
        require(appId.isNotEmpty()) { "appId must not be empty." }
        return try {
            packageOf(retrostoreClient.getApp(appId))
        } catch (e: ApiException) {
            Log.e(TAG, "Cannot download media images.", e)
            null
        }
    }

    /**
     * Downloads the media images for [app] and returns the whole package. The network call runs on
     * the IO dispatcher, so this is safe to call from the main thread.
     *
     * @return The complete package, or null if [app] is null or the images could not be
     * downloaded.
     */
    suspend fun downloadImages(app: App?): AppPackage? {
        if (app == null) {
            return null
        }
        return try {
            packageOf(app)
        } catch (e: ApiException) {
            Log.e(TAG, "Cannot download media images.", e)
            null
        }
    }

    /**
     * Uploads a system state ephemerally to the RetroStore. The network call runs on the IO
     * dispatcher, so this is safe to call from the main thread.
     *
     * @return The token identifying the uploaded state, or null if the upload failed.
     */
    suspend fun uploadSystemState(state: SystemState): Long? = try {
        retrostoreClient.uploadState(state)
    } catch (e: ApiException) {
        Log.e(TAG, "Cannot upload system state.", e)
        null
    }

    /**
     * Downloads an ephemeral system state from the RetroStore. The network call runs on the IO
     * dispatcher, so this is safe to call from the main thread.
     *
     * @param token the token the state was uploaded under.
     * @return The state, or null if it could not be downloaded.
     */
    suspend fun downloadSystemState(token: Long): SystemState? = try {
        retrostoreClient.downloadState(token)
    } catch (e: ApiException) {
        Log.e(TAG, "Cannot download system state.", e)
        null
    }

    /** Fetches the media images belonging to [app] and bundles both into a package. */
    private suspend fun packageOf(app: App?): AppPackage? =
            app?.let { AppPackage(it, retrostoreClient.fetchMediaImages(it.id)) }
}
