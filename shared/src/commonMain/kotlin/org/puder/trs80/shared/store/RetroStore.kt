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

package org.puder.trs80.shared.store

import org.puder.trs80.shared.io.httpPostBytes
import org.retrostore.RetrostoreClient

/**
 * The RetroStore client, over the platform's own HTTP.
 *
 * The client itself carries no transport: it is Wire messages and one POST, and
 * it is handed the POST so that it needs no HTTP library of its own. This is
 * where the platform's is supplied.
 */
val retroStore: RetrostoreClient by lazy {
    RetrostoreClient(post = ::httpPostBytes)
}
