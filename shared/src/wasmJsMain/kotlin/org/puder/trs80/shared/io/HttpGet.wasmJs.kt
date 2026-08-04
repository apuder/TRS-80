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

/**
 * Not yet, and this is the one that decides whether a web app is possible.
 *
 * fetch() is easy enough to reach from here. What is not up to us is whether
 * retrostore.org sends the CORS headers that let a page read the reply: a
 * browser will make the request and then refuse to hand it over, and no amount
 * of code on this side changes that. If it does not, the web app needs a proxy
 * of its own, and that is a deployment decision rather than a Kotlin one.
 *
 * Until it is answered, the catalog says the store is unreachable, which is
 * true and is a state the library already draws.
 */
actual suspend fun httpGetBytes(url: String): ByteArray =
    throw UnsupportedOperationException("HTTP from the browser is not wired up yet: $url")

actual suspend fun httpPostBytes(url: String, body: ByteArray): ByteArray =
    throw UnsupportedOperationException("HTTP from the browser is not wired up yet: $url")
