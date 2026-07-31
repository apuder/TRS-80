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

package org.puder.trs80.shared.io

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.IOException
import platform.Foundation.NSData
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.setHTTPBody
import platform.Foundation.setHTTPMethod
import platform.Foundation.create
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.dataTaskWithURL
import platform.posix.memcpy
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class)
actual suspend fun httpGetBytes(url: String): ByteArray =
    suspendCancellableCoroutine { continuation ->
        val target = NSURL.URLWithString(url)
            ?: return@suspendCancellableCoroutine continuation.resumeWithException(
                IOException("Not a URL: $url")
            )

        val task = NSURLSession.sharedSession.dataTaskWithURL(target) { data, response, error ->
            when {
                error != null ->
                    continuation.resumeWithException(
                        IOException("GET $url failed: ${error.localizedDescription}")
                    )

                else -> {
                    val status = (response as? NSHTTPURLResponse)?.statusCode?.toInt() ?: 0
                    if (status !in 200..299) {
                        continuation.resumeWithException(
                            IOException("GET $url failed with HTTP $status")
                        )
                    } else {
                        continuation.resume(data?.toByteArray() ?: ByteArray(0))
                    }
                }
            }
        }
        // Cancelling the coroutine has to stop the transfer too, or a download
        // abandoned mid-flight keeps running and completes into a dead
        // continuation.
        continuation.invokeOnCancellation { task.cancel() }
        task.resume()
    }

/** Copies an [NSData]'s bytes out into Kotlin's heap. */
@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) {
        return ByteArray(0)
    }
    return ByteArray(size).apply {
        usePinned { memcpy(it.addressOf(0), bytes, length) }
    }
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun httpPostBytes(url: String, body: ByteArray): ByteArray =
    suspendCancellableCoroutine { continuation ->
        val target = NSURL.URLWithString(url)
            ?: return@suspendCancellableCoroutine continuation.resumeWithException(
                IOException("Not a URL: $url")
            )
        val request = NSMutableURLRequest.requestWithURL(target).apply {
            setHTTPMethod("POST")
            setHTTPBody(body.toNSData())
        }

        val task = NSURLSession.sharedSession.dataTaskWithRequest(request) { data, response, error ->
            when {
                error != null -> continuation.resumeWithException(
                    IOException("POST $url failed: ${error.localizedDescription}")
                )

                else -> {
                    val status = (response as? NSHTTPURLResponse)?.statusCode?.toInt() ?: 0
                    if (status !in 200..299) {
                        continuation.resumeWithException(
                            IOException("POST $url failed with HTTP $status")
                        )
                    } else {
                        continuation.resume(data?.toByteArray() ?: ByteArray(0))
                    }
                }
            }
        }
        continuation.invokeOnCancellation { task.cancel() }
        task.resume()
    }

/** Copies a Kotlin array into an [NSData] for the request body. */
@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData = if (isEmpty()) {
    NSData()
} else {
    usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }
}
