/*
 * Copyright 2025, Arno Puder
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
 * Fetches the bytes at [url].
 *
 * A deliberately tiny seam. The obvious choice was Ktor, and the plan said so —
 * but Ktor 3.3–3.5 cannot go in this framework: 3.4.0 and later are built with
 * Kotlin 2.3, whose klib ABI the 2.2.10 compiler AGP pins us to will not read,
 * and 3.3.x links but segfaults at start-up inside Kotlin/Native's worker
 * runtime before any Ktor code runs — merely linking `ktor-client-core` is
 * enough. Since the app makes exactly one kind of request, an unauthenticated
 * GET of a whole file, the platform's own client costs less than the dependency
 * and none of the version pinning. Revisit when Kotlin moves.
 *
 * @throws okio.IOException if the request fails or the response is not a success.
 */
expect suspend fun httpGetBytes(url: String): ByteArray
