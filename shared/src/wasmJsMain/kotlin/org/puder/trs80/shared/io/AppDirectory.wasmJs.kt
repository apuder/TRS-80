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
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

/**
 * In memory, for now, and gone when the tab closes.
 *
 * A browser has no file system to hand okio. What it has is the Origin Private
 * File System, which is asynchronous where okio is not, so bridging the two is
 * a piece of work in itself -- and until the emulator core is here there is
 * nothing to keep anyway. This makes the storage layer run unchanged, which is
 * what the rest of the app is written against; what it does not do is remember
 * anything.
 */
private val APP_DIRECTORY: Path = "/trs80".toPath()

private val browserFileSystem = FakeFileSystem().apply { createDirectories(APP_DIRECTORY) }

actual fun appDataDirectory(): Path = APP_DIRECTORY

actual val appFileSystem: FileSystem = browserFileSystem
