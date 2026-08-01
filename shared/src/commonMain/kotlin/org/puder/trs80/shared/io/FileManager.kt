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
import okio.IOException
import okio.Path
import org.puder.trs80.shared.Log

private const val TAG = "FileManager"

/**
 * The directory, inside the app's own data directory, that everything lives in.
 *
 * Was the `trs80_dir` string resource, and its value must not change: existing
 * installs have their disk images, cassettes and saved states under this name,
 * and nothing looks anywhere else for them.
 */
const val TRS80_DIRECTORY = "TRS-80"

/**
 * File I/O for the app, rooted at one directory.
 *
 * Was `org.puder.trs80.io.FileManager` and is unchanged in behavior: the same
 * base-directory-plus-filename model, the same swallow-and-log error handling.
 * What changed is that it is written against okio rather than `java.io.File`,
 * and that the base directory arrives as a [Path] instead of being fetched from
 * a global `Context`.
 */
class FileManager private constructor(
    /** Exposed so callers holding absolute paths can ask about the same storage. */
    internal val fileSystem: FileSystem,
    private val baseDir: Path,
) {

    /**
     * Creator for [FileManager] instances, all rooted under one app directory.
     *
     * @param fileSystem which file system to work on. Defaults to the real one;
     * tests pass a fake.
     */
    class Creator(
        private val appBaseDir: Path,
        private val fileSystem: FileSystem = appFileSystem,
    ) {

        /** A manager for the base directory the app stores its data in. */
        @Throws(IOException::class)
        fun forAppBaseDir(): FileManager = createForAppDir(null)

        /** A manager for the sub-directory belonging to the configuration with this ID. */
        @Throws(IOException::class)
        fun createForAppSubDir(configId: Int): FileManager = createForAppDir(configId.toString())

        /** A manager for the named sub-directory of the app's base directory. */
        @Throws(IOException::class)
        fun createForAppSubDir(dirName: String): FileManager = createForAppDir(dirName)

        /**
         * Creates a manager for the app's base directory, or for [dirName] within
         * it. The directory is created if it does not exist yet.
         */
        @Throws(IOException::class)
        private fun createForAppDir(dirName: String?): FileManager {
            val dir = dirName?.let { appBaseDir / it } ?: appBaseDir
            try {
                fileSystem.createDirectories(dir)
            } catch (e: IOException) {
                throw IOException("Cannot create local store directory: $dir", e)
            }
            return FileManager(fileSystem, dir)
        }

        companion object {
            /** A creator rooted at this app's own data directory. */
            fun get(): Creator = Creator(appDataDirectory() / TRS80_DIRECTORY)
        }
    }

    /** The absolute path of a file of this name within this manager's directory. */
    fun getAbsolutePathForFile(filename: String): String = (baseDir / filename).toString()

    /**
     * Ensures there is a ".nomedia" file in this manager's directory, which is
     * what keeps Android's media scanner out of the disk images.
     */
    fun ensureNoMedia(): Boolean {
        val noMedia = baseDir / ".nomedia"
        return try {
            fileSystem.exists(noMedia) || run {
                fileSystem.write(noMedia) { }
                true
            }
        } catch (e: IOException) {
            Log.e(TAG, "Cannot create nomedia file: $noMedia", e)
            false
        }
    }

    /**
     * Writes or overwrites a file in this manager's directory.
     *
     * @return whether the file was written.
     */
    fun writeFile(filename: String, content: ByteArray): Boolean {
        val file = baseDir / filename
        Log.i(TAG, "About to write to $file")
        return try {
            fileSystem.write(file) { write(content) }
            true
        } catch (e: IOException) {
            Log.e(TAG, "Cannot write file $filename", e)
            false
        }
    }

    /** @return the contents of the named file, or null if it could not be read. */
    fun readFile(filename: String): ByteArray? = try {
        fileSystem.read(baseDir / filename) { readByteArray() }
    } catch (e: IOException) {
        null
    }

    /**
     * Deletes a file in this manager's directory.
     *
     * @return whether the file is gone afterwards, which includes never having
     * been there.
     */
    fun deleteFile(filename: String): Boolean {
        val file = baseDir / filename
        return try {
            fileSystem.delete(file, mustExist = false)
            true
        } catch (e: IOException) {
            !fileSystem.exists(file)
        }
    }

    /** Deletes this manager's directory and everything in it. */
    fun delete() {
        try {
            fileSystem.deleteRecursively(baseDir, mustExist = false)
        } catch (e: IOException) {
            Log.e(TAG, "Cannot delete $baseDir", e)
        }
    }

    /** @return how many entries this manager's directory holds. */
    fun fileCount(): Int = names().size

    /** @return whether this manager's directory holds a file of this name. */
    fun hasFile(filename: String): Boolean = filename in names()

    /**
     * The entries in this manager's directory, empty when it does not exist.
     *
     * `listOrNull` rather than `list`, because the original tolerated a missing
     * directory here and an ACRA report says it does happen.
     */
    private fun names(): List<String> =
        fileSystem.listOrNull(baseDir).orEmpty().map { it.name }
}
