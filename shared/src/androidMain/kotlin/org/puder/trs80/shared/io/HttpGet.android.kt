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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val CONNECT_TIMEOUT_MS = 30_000
private const val READ_TIMEOUT_MS = 60_000

actual suspend fun httpGetBytes(url: String): ByteArray = withContext(Dispatchers.IO) {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = CONNECT_TIMEOUT_MS
    connection.readTimeout = READ_TIMEOUT_MS
    try {
        val status = connection.responseCode
        if (status !in 200..299) {
            throw IOException("GET $url failed with HTTP $status")
        }
        connection.inputStream.use { it.readBytes() }
    } catch (e: java.io.IOException) {
        throw IOException("GET $url failed", e)
    } finally {
        connection.disconnect()
    }
}
