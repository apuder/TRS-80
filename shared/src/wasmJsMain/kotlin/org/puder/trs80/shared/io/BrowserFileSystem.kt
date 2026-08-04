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
import kotlinx.browser.localStorage
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.Sink
import okio.fakefilesystem.FakeFileSystem
import org.puder.trs80.shared.Log

private const val TAG = "BrowserFileSystem"

/** What every stored file's key begins with, so the app's own can be found again. */
private const val PREFIX = "trs80.file:"

/**
 * A file system that survives the tab, built out of one that does not.
 *
 * okio wants a file system it can read and write without waiting, and a browser
 * has nothing of the sort: the Origin Private File System is asynchronous
 * everywhere it is not inside a worker. So the working copy is in memory, where
 * it is fast and synchronous, and every write is mirrored into localStorage --
 * which is synchronous too, and the only storage in a page that is.
 *
 * What that buys is the thing that was missing: a machine made today is still
 * there tomorrow, with its disks, and the ROMs are not fetched again on every
 * load.
 *
 * What it costs is a ceiling. localStorage is about five megabytes per origin
 * and holds strings, so the bytes go in as base64 and take a third more room
 * than they are -- perhaps twenty machines' worth. Past that, writes fail, and
 * they fail the way a full disk does: the file is in memory and works for this
 * session, and the log says it did not persist. IndexedDB is where this goes
 * when twenty is not enough.
 */
class BrowserFileSystem : ForwardingFileSystem(FakeFileSystem()) {

    init {
        hydrate()
    }

    /** Reads back everything a previous session left behind. */
    @OptIn(ExperimentalEncodingApi::class)
    private fun hydrate() {
        var restored = 0
        for (index in 0 until localStorage.length) {
            val key = localStorage.key(index) ?: continue
            if (!key.startsWith(PREFIX)) {
                continue
            }
            val path = key.removePrefix(PREFIX).toPath()
            val encoded = localStorage.getItem(key) ?: continue
            try {
                val bytes = Base64.decode(encoded)
                path.parent?.let { delegate.createDirectories(it) }
                delegate.write(path) { write(bytes) }
                restored++
            } catch (e: Exception) {
                Log.w(TAG, "Could not restore $path: $e")
            }
        }
        if (restored > 0) {
            Log.i(TAG, "Restored $restored file(s) from the last visit.")
        }
    }

    /**
     * Writes go to memory first and to storage when the file is closed.
     *
     * On close rather than as they arrive, because a sink is written to in
     * pieces and only the whole file is worth keeping -- and because reading it
     * back to encode it is only possible once it is there.
     */
    override fun sink(file: Path, mustCreate: Boolean): Sink =
        persisting(super.sink(file, mustCreate), file)

    override fun appendingSink(file: Path, mustExist: Boolean): Sink =
        persisting(super.appendingSink(file, mustExist), file)

    /** [sink], with the file kept once whatever is writing it has finished. */
    private fun persisting(sink: Sink, file: Path): Sink = object : Sink by sink {
        override fun close() {
            sink.close()
            persist(file)
        }
    }

    override fun delete(path: Path, mustExist: Boolean) {
        super.delete(path, mustExist)
        localStorage.removeItem(PREFIX + path)
    }

    override fun atomicMove(source: Path, target: Path) {
        super.atomicMove(source, target)
        localStorage.removeItem(PREFIX + source)
        persist(target)
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun persist(file: Path) {
        try {
            val bytes = delegate.read(file) { readByteArray() }
            localStorage.setItem(PREFIX + file, Base64.encode(bytes))
        } catch (e: Exception) {
            // A quota, in practice. The file is in memory and this session will
            // not notice; the next one will find it missing.
            Log.w(TAG, "Could not keep $file for next time: $e")
        }
    }

    /** Everything the app has stored, forgotten for good. */
    fun forgetEverything() {
        val keys = (0 until localStorage.length).mapNotNull { localStorage.key(it) }
        keys.filter { it.startsWith(PREFIX) }.forEach(localStorage::removeItem)
    }
}

/** The one file system the app uses, as [appFileSystem] hands it out. */
internal val browserFiles: FileSystem = BrowserFileSystem()
