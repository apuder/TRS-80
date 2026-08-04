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
import okio.Path.Companion.toPath
import org.puder.trs80.shared.Log

private const val TAG = "FileDownloader"

/** Where a ZIP is put down so it can be opened as a file system. */
private const val TEMP_ARCHIVE = "download.zip.tmp"

/**
 * Downloads files, extracting one entry when what arrives is a ZIP.
 *
 * Was blocking, on `java.net.URL` and `java.util.zip`; it is now suspending, and
 * the archive is read with okio. The caller already ran it on an IO dispatcher,
 * so suspending is what it wanted anyway.
 *
 * @param fetch how bytes are got from a URL, defaulting to the platform's own
 * HTTP client — see [httpGetBytes] for why that is not Ktor. Injectable so the
 * archive handling below can be tested without a network.
 * @param fileSystem where the temporary archive is written, and [scratchDir]
 * the directory it goes in. A ZIP has to be a file to be read as one: okio
 * reads entries through the central directory at its end, so a stream is no use.
 */
class FileDownloader(
    private val fetch: suspend (String) -> ByteArray = ::httpGetBytes,
    private val fileSystem: FileSystem = appFileSystem,
    private val scratchDir: Path = appDataDirectory(),
) {

    /**
     * Downloads [urlStr], and extracts [fileInZip] from it when that is given.
     *
     * @return The file contents, or null if it could not be downloaded or
     * extracted. Failure is logged and swallowed, as it was before: the caller
     * reports "could not load data" and carries on to the next item.
     */
    suspend fun download(urlStr: String, fileInZip: String?): ByteArray? {
        val body = try {
            fetch(urlStr)
        } catch (e: IOException) {
            Log.e(TAG, "Could not load data from $urlStr", e)
            return null
        }
        return if (fileInZip == null) body else extractFromZip(body, fileInZip)
    }

    /**
     * @return The contents of the entry named [fileInZip], or null if the
     * archive has no such entry or could not be read.
     */
    private fun extractFromZip(archive: ByteArray, fileInZip: String): ByteArray? {
        val tempFile = scratchDir / TEMP_ARCHIVE
        return try {
            fileSystem.createDirectories(scratchDir)
            fileSystem.write(tempFile) { write(archive) }
            val zip = fileSystem.openArchive(tempFile)
            if (zip == null) {
                Log.e(TAG, "This platform cannot read archives; '$fileInZip' stays in the ZIP.")
                return null
            }
            val entry = zip.listRecursively("/".toPath())
                .firstOrNull { it.name == fileInZip }
            if (entry == null) {
                Log.e(TAG, "No entry named '$fileInZip' in the archive.")
                null
            } else {
                zip.read(entry) { readByteArray() }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Could not extract '$fileInZip' from the archive.", e)
            null
        } finally {
            try {
                fileSystem.delete(tempFile, mustExist = false)
            } catch (e: IOException) {
                Log.w(TAG, "Could not remove the temporary archive: $tempFile")
            }
        }
    }
}
