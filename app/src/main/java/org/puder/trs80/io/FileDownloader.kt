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

import android.util.Log
import com.google.common.base.Optional
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.zip.ZipInputStream

private const val TAG = "FileDownloader"

/**
 * Downloads files and optionally extracts entries from a ZIP file.
 */
class FileDownloader {

    /**
     * Download the given URL and if needed, extracts a file from the ZIP file.
     *
     * @param urlStr the URL to download.
     * @param fileInZip if the URL is a ZIP file, specify which file to extract from it.
     * @return The file contents or absent, if the file could not be downloaded or extracted.
     */
    fun download(urlStr: String, fileInZip: String?): Optional<ByteArray> = try {
        BufferedInputStream(URL(urlStr).openConnection().inputStream).use { input ->
            Optional.fromNullable(
                    if (fileInZip == null) input.readBytes() else extractFromZip(input, fileInZip))
        }
    } catch (e: IOException) {
        Log.e(TAG, "Could not load data", e)
        Optional.absent<ByteArray>()
    }

    /**
     * @return The contents of the entry named [fileInZip], or null if the archive has no such
     * entry.
     */
    private fun extractFromZip(input: InputStream, fileInZip: String): ByteArray? =
            ZipInputStream(input).use { zip ->
                // readBytes() stops at the end of the entry the stream is positioned on.
                generateSequence { zip.nextEntry }
                        .firstOrNull { it.name == fileInZip }
                        ?.let { zip.readBytes() }
            }
}
