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

package org.retrostore

import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import kotlinx.coroutines.CancellationException
import org.retrostore.client.common.proto.ApiResponseApps
import org.retrostore.client.common.proto.ApiResponseDownloadSystemState
import org.retrostore.client.common.proto.ApiResponseMediaImages
import org.retrostore.client.common.proto.ApiResponseUploadSystemState
import org.retrostore.client.common.proto.App
import org.retrostore.client.common.proto.DownloadSystemStateParams
import org.retrostore.client.common.proto.FetchMediaImagesParams
import org.retrostore.client.common.proto.GetAppParams
import org.retrostore.client.common.proto.ListAppsParams
import org.retrostore.client.common.proto.MediaImage
import org.retrostore.client.common.proto.SystemState
import org.retrostore.client.common.proto.UploadSystemStateParams

/** The API endpoint. The placeholder is replaced with the name of the method being called. */
private const val DEFAULT_SERVER_URL = "https://retrostore.org/api/%s"

/**
 * How a request is made: POST [body] to [url] and hand back what comes back.
 *
 * The one thing this client needs from a platform, and the only reason it would
 * otherwise need an HTTP library of its own.
 */
typealias HttpPost = suspend (url: String, body: ByteArray) -> ByteArray

/**
 * Talks to the RetroStore API.
 *
 * Every method POSTs the serialised params message to `<server>/<method>` and parses the body of
 * the response as the matching `ApiResponse*` message.
 *
 * The transport is injected rather than built here. That keeps this dependency-free — the whole
 * client is Wire messages and one POST — and it is what lets the same code run on both platforms
 * without either an HTTP library that works on one of them or a second implementation.
 *
 * @param serverUrl the endpoint template, with a single `%s` for the method name.
 * @param post makes the request.
 */
class RetrostoreClient(
        private val serverUrl: String = DEFAULT_SERVER_URL,
        private val post: HttpPost) {

    /**
     * @param appId the ID of the app to look up. Must not be empty.
     * @return The app, or null if the store does not know it.
     * @throws ApiException if the request fails or the server reports an error.
     */
    suspend fun getApp(appId: String): App? {
        require(appId.isNotEmpty()) { "appId missing." }
        val response =
                post("getApp", GetAppParams(app_id = appId), ApiResponseApps.ADAPTER)
        checkSuccess(response.success, response.message)
        return response.app.firstOrNull()
    }

    /**
     * Lists apps, starting at [start] and returning at most [num] of them.
     *
     * @throws ApiException if the request fails or the server reports an error.
     */
    suspend fun fetchApps(start: Int, num: Int): List<App> {
        val response = post(
                "listApps", ListAppsParams(start = start, num = num), ApiResponseApps.ADAPTER)
        checkSuccess(response.success, response.message)
        return response.app
    }

    /**
     * @return The media images of the app with the given [appId].
     * @throws ApiException if the request fails or the server reports an error.
     */
    suspend fun fetchMediaImages(appId: String): List<MediaImage> {
        val response = post(
                "fetchMediaImages",
                FetchMediaImagesParams(app_id = appId),
                ApiResponseMediaImages.ADAPTER)
        checkSuccess(response.success, response.message)
        // Only return non-zero images, which is skip zero-size "UNKNOWN" entries.
        return response.mediaImage.filter { it.data_.size > 0 }
    }

    /**
     * Uploads [state] ephemerally.
     *
     * @return The token the state can be downloaded with.
     * @throws ApiException if the request fails or the server reports an error.
     */
    suspend fun uploadState(state: SystemState): Long {
        val response = post(
                "uploadState",
                UploadSystemStateParams(state = state),
                ApiResponseUploadSystemState.ADAPTER)
        checkSuccess(response.success, response.message)
        return response.token
    }

    /**
     * @param token the token the state was uploaded under.
     * @return The state, or null if the response carried none.
     * @throws ApiException if the request fails or the server reports an error.
     */
    suspend fun downloadState(token: Long): SystemState? {
        val response = post(
                "downloadState",
                DownloadSystemStateParams(token = token),
                ApiResponseDownloadSystemState.ADAPTER)
        checkSuccess(response.success, response.message)
        return response.systemState
    }

    /**
     * POSTs [params] to the given API [method] and parses the response with [responseAdapter].
     *
     * @throws ApiException if the request could not be made or the response could not be parsed.
     */
    private suspend fun <T : Any> post(
            method: String,
            params: Message<*, *>,
            responseAdapter: ProtoAdapter<T>): T {
        val url = serverUrl.replace("%s", method)
        val content = try {
            post(url, params.encode())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw ApiException("Unable to make request to server.", e)
        }
        return try {
            responseAdapter.decode(content)
        } catch (e: Exception) {
            throw ApiException("Unable to parse the server's response.", e)
        }
    }
}

/** @throws ApiException carrying the server's [message] if [success] is not set. */
private fun checkSuccess(success: Boolean, message: String) {
    if (!success) {
        throw ApiException("Server reported error: '$message'")
    }
}
