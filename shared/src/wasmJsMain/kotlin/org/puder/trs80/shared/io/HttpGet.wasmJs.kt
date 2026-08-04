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

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.Promise
import kotlinx.coroutines.await
import okio.IOException

/**
 * The page's own fetch, with the bytes crossing as base64.
 *
 * A few instructions more than handing over an ArrayBuffer, and a boundary that
 * cannot be got subtly wrong: no view onto memory the other side owns, no
 * question of who copies what, and the same shape in both directions.
 *
 * No headers are set, and that is the interesting part. A cross-origin POST that
 * names its content type stops being a "simple" request, and the browser then
 * insists on a preflight -- which retrostore.org answers 200 but without the
 * header that would allow the request through. Sent plainly there is no
 * preflight, and the reply carries `access-control-allow-origin: *`, which is
 * what makes a web app possible here at all. The server reads the body as bytes
 * and never asks what it was called.
 */
private fun fetchBase64(url: String): Promise<JsString> = js(
    """fetch(url).then(function (response) {
        if (!response.ok) throw new Error('HTTP ' + response.status + ' from ' + url);
        return response.arrayBuffer();
    }).then(function (buffer) {
        var bytes = new Uint8Array(buffer);
        var binary = '';
        for (var i = 0; i < bytes.length; i++) binary += String.fromCharCode(bytes[i]);
        return btoa(binary);
    })"""
)

private fun postBase64(url: String, body: String): Promise<JsString> = js(
    """(function () {
        var binary = atob(body);
        var bytes = new Uint8Array(binary.length);
        for (var i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
        return fetch(url, { method: 'POST', body: bytes }).then(function (response) {
            if (!response.ok) throw new Error('HTTP ' + response.status + ' from ' + url);
            return response.arrayBuffer();
        }).then(function (buffer) {
            var out = new Uint8Array(buffer);
            var text = '';
            for (var i = 0; i < out.length; i++) text += String.fromCharCode(out[i]);
            return btoa(text);
        });
    })()"""
)

/**
 * Runs [request] and turns whatever the page throws into what callers expect.
 *
 * The contract on the expect is okio's IOException, and everything upstream is
 * written to it: a ROM download that fails is meant to leave the setup panel
 * offering another go. A raw JavaScript error is not that -- it goes straight
 * past the catch and out through the effect that started it, which is a blank
 * page instead of a message. Losing a ROM to a flaky network should not lose
 * the app.
 */
private suspend fun asIo(url: String, request: () -> Promise<JsString>): ByteArray = try {
    Base64.decode(request().await().toString())
} catch (e: IOException) {
    throw e
} catch (e: Throwable) {
    throw IOException("Could not fetch $url: ${e.message}")
}

@OptIn(ExperimentalEncodingApi::class)
actual suspend fun httpGetBytes(url: String): ByteArray = asIo(url) { fetchBase64(url) }

@OptIn(ExperimentalEncodingApi::class)
actual suspend fun httpPostBytes(url: String, body: ByteArray): ByteArray =
    asIo(url) { postBase64(url, Base64.encode(body)) }
