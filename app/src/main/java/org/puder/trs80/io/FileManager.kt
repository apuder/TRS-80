/*
 * Copyright 2017, Sascha Haeberling
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

package org.puder.trs80.io

import android.content.res.Resources
import android.util.Log
import org.puder.trs80.R
import org.puder.trs80.StrUtil
import org.puder.trs80.TRS80Application
import java.io.File
import java.io.IOException

private const val TAG = "FileManager"

/**
 * File I/O functionality for the app.
 */
class FileManager private constructor(private val baseDir: File) {

    /** Creator for [FileManager] instances. */
    class Creator private constructor(private val appBaseDir: String) {

        /**
         * Initialize a file manager for the base directory in which the app stores data.
         */
        @Throws(IOException::class)
        fun forAppBaseDir(): FileManager = createForAppDir(null)

        /**
         * Initialize a file manager for the sub-directory belonging to the given configuration.
         */
        @Throws(IOException::class)
        fun createForAppSubDir(configId: Int): FileManager = createForAppDir(configId.toString())

        /**
         * Initialize a file manager for the given sub-directory of the app's base directory.
         */
        @Throws(IOException::class)
        fun createForAppSubDir(dirName: String): FileManager = createForAppDir(dirName)

        /**
         * Creates a file manager for the app's base directory, or for [dirName] within it when
         * a sub-directory name is given. The directory is created if it does not exist yet.
         */
        @Throws(IOException::class)
        private fun createForAppDir(dirName: String?): FileManager {
            val appDir = TRS80Application.getAppContext().filesDir.resolve(appBaseDir)
            val localStoreDir = dirName?.let { appDir.resolve(it) } ?: appDir
            if (!localStoreDir.exists() && !localStoreDir.mkdirs()) {
                throw IOException(StrUtil.form("Cannot create local store directory: %s",
                        localStoreDir.absolutePath))
            }
            return forBaseDir(localStoreDir)
        }

        /**
         * Initialize a file manager for the given base directory.
         */
        @Throws(IOException::class)
        private fun forBaseDir(baseDir: File): FileManager {
            if (!baseDir.exists() && !baseDir.mkdirs()) {
                throw IOException("Cannot create dir: " + baseDir.absolutePath)
            }
            return FileManager(baseDir)
        }

        companion object {
            /** Creates a creator rooted at the app's data directory. */
            @JvmStatic
            fun get(res: Resources): Creator = Creator(res.getString(R.string.trs80_dir))
        }
    }

    /**
     * @return The absolute name of a file with the given name within this manager's base path.
     */
    fun getAbsolutePathForFile(filename: String): String = baseDir.resolve(filename).absolutePath

    /**
     * Ensures that there is a ".nomedia" file in the base dir of this manager.
     */
    fun ensureNoMedia(): Boolean {
        val noMediaFile = baseDir.resolve(".nomedia")
        return try {
            noMediaFile.exists() || noMediaFile.createNewFile()
        } catch (ex: IOException) {
            Log.e(TAG, "Cannot create nomedia file: " + noMediaFile.absolutePath)
            false
        }
    }

    /**
     * Writes or overwrites a file within the base path of this manager.
     *
     * @param filename the filename to use for the entry.
     * @param content the byte content of the new entry.
     * @return Whether the file was successfully added.
     */
    fun writeFile(filename: String, content: ByteArray): Boolean {
        val newFile = baseDir.resolve(filename)
        Log.i(TAG, "About to write to " + newFile.absolutePath)
        return try {
            newFile.writeBytes(content)
            true
        } catch (e: IOException) {
            Log.e(TAG, "Cannot write file $filename", e)
            false
        }
    }

    /**
     * Reads a file within the base path of this manager.
     *
     * @param filename the filename to read
     * @return The contents of the file, or null if it could not be read.
     */
    fun readFile(filename: String): ByteArray? = try {
        baseDir.resolve(filename).readBytes()
    } catch (e: IOException) {
        null
    }

    /**
     * Deletes a file within the base path of this manager.
     *
     * @param filename the name of the file.
     * @return Whether the file is non-existent after calling this method.
     */
    fun deleteFile(filename: String): Boolean {
        val toDelete = baseDir.resolve(filename)
        return !toDelete.exists() || toDelete.delete()
    }

    /** Deletes this directory and all contents. */
    fun delete() {
        // The children should not be null, but we got an ACRA report saying otherwise.
        baseDir.list()?.forEach { baseDir.resolve(it).delete() }
        baseDir.delete()
    }

    /**
     * @return How many files/directories are in the base dir of this manager.
     */
    fun fileCount(): Int = if (baseDir.exists()) baseDir.list().size else 0

    /** @return Whether the base dir of this manager contains a file with the given name. */
    fun hasFile(filename: String): Boolean = baseDir.exists() && baseDir.list().contains(filename)
}
