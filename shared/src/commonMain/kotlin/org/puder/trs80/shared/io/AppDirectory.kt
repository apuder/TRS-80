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

import okio.FileSystem
import okio.Path

/**
 * Where this app may keep its own files: `filesDir` on Android, and the
 * sandbox's Documents directory on iOS.
 *
 * Both are private to the app and survive updates, and neither needs a
 * permission. This is the only thing the file code has to ask the platform for
 * — everything below it is an okio path.
 *
 * On Android this has to be given the `Context` before anything reads it, which
 * `TRS80Application` does; see the Android actual.
 */
expect fun appDataDirectory(): Path

/**
 * The file system the app's own files live on.
 *
 * okio declares `FileSystem.SYSTEM` per platform rather than in common — a
 * browser has none — so it has to be reached through a seam even though both
 * hosts here have exactly the one.
 */
expect val appFileSystem: FileSystem
