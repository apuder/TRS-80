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
import com.google.common.base.Optional
import com.google.common.collect.ImmutableList
import org.retrostore.ApiException
import org.retrostore.RetrostoreClient
import org.retrostore.RetrostoreClientImpl
import org.retrostore.client.common.proto.App
import org.retrostore.client.common.proto.SystemState

private const val TAG = "RetrostoreApi"

/**
 * Main API interface for an Android app to interface with the RetroStore Android components.
 */
object RetrostoreApi {
    private val retrostoreClient: RetrostoreClient = RetrostoreClientImpl.getDefault("n/a")
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
     * Downloads all aspects of an app package. Do not call this on the main thread.
     *
     * @param appId the ID of the app to download. Must not be empty.
     * @return The complete package, or absent if it could not be downloaded.
     */
    fun downloadApp(appId: String): Optional<AppPackage> {
        require(appId.isNotEmpty()) { "appId must not be empty." }
        return try {
            downloadImages(retrostoreClient.getApp(appId))
        } catch (e: ApiException) {
            Log.e(TAG, "Cannot download media images.", e)
            Optional.absent<AppPackage>()
        }
    }

    /**
     * Downloads the media images for [app] and returns the whole package.
     *
     * @return The complete package, or absent if [app] is null or the images could not be
     * downloaded.
     */
    fun downloadImages(app: App?): Optional<AppPackage> {
        if (app == null) {
            return Optional.absent()
        }
        return try {
            val mediaImages = retrostoreClient.fetchMediaImages(app.id)
            Optional.of(AppPackage(app, ImmutableList.copyOf(mediaImages)))
        } catch (e: ApiException) {
            Log.e(TAG, "Cannot download media images.", e)
            Optional.absent<AppPackage>()
        }
    }

    /**
     * Uploads a system state ephemerally to the RetroStore.
     *
     * @return The token identifying the uploaded state, or absent if the upload failed.
     */
    fun uploadSystemState(state: SystemState): Optional<Long> = try {
        Optional.of(retrostoreClient.uploadState(state))
    } catch (e: ApiException) {
        Log.e(TAG, "Cannot upload system state.", e)
        Optional.absent<Long>()
    }

    /**
     * Downloads an ephemeral system state from the RetroStore.
     *
     * @param token the token the state was uploaded under.
     * @return The state, or absent if it could not be downloaded.
     */
    fun downloadSystemState(token: Long): Optional<SystemState> = try {
        Optional.of(retrostoreClient.downloadState(token))
    } catch (e: ApiException) {
        Log.e(TAG, "Cannot download system state.", e)
        Optional.absent<SystemState>()
    }
}
