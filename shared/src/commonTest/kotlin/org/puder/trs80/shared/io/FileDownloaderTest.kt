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

import kotlinx.coroutines.test.runTest
import okio.IOException
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class FileDownloaderTest {

    private val fileSystem = FakeFileSystem()
    private val scratch = "/data/TRS-80".toPath()

    private fun downloaderReturning(body: ByteArray) = FileDownloader(
        fetch = { body },
        fileSystem = fileSystem,
        scratchDir = scratch,
    )

    @Test
    fun aPlainDownloadIsReturnedAsIs() = runTest {
        val downloader = downloaderReturning(byteArrayOf(1, 2, 3))

        assertContentEquals(
            byteArrayOf(1, 2, 3),
            downloader.download("https://example.invalid/model3.rom", null),
        )
    }

    @Test
    fun anEntryIsExtractedFromAZip() = runTest {
        val zip = zipOf("ld3-531.dsk" to byteArrayOf(9, 8, 7), "readme.txt" to byteArrayOf(1))
        val downloader = downloaderReturning(zip)

        val extracted = downloader.download("https://example.invalid/ld3-531.zip", "ld3-531.dsk")

        if (!readsArchives) {
            // A browser's okio ships without ZIP support, and the downloader
            // says so rather than pretending the archive was empty. Which of
            // those two this is, is the platform's answer and not this test's.
            assertNull(extracted)
            return@runTest
        }
        assertContentEquals(byteArrayOf(9, 8, 7), extracted)
    }

    /** Whether this platform can open an archive at all; see [openArchive]. */
    private val readsArchives: Boolean
        get() {
            val probe = "/probe.zip".toPath()
            fileSystem.write(probe) { write(zipOf("a" to byteArrayOf(1))) }
            return fileSystem.openArchive(probe) != null
        }

    @Test
    fun anAbsentZipEntryGivesNull() = runTest {
        val downloader = downloaderReturning(zipOf("other.dsk" to byteArrayOf(1)))

        assertNull(downloader.download("https://example.invalid/a.zip", "wanted.dsk"))
    }

    /** The scratch file must not be left behind, whatever happened. */
    @Test
    fun theTemporaryArchiveIsAlwaysCleanedUp() = runTest {
        val downloader = downloaderReturning(zipOf("a.dsk" to byteArrayOf(1)))

        downloader.download("https://example.invalid/a.zip", "a.dsk")
        assertFalse(fileSystem.exists(scratch / "download.zip.tmp"))

        downloaderReturning(byteArrayOf(0, 0, 0)).download("https://example.invalid/bad.zip", "x")
        assertFalse(fileSystem.exists(scratch / "download.zip.tmp"))
    }

    @Test
    fun somethingThatIsNotAZipGivesNull() = runTest {
        val downloader = downloaderReturning(byteArrayOf(0, 1, 2, 3, 4))

        assertNull(downloader.download("https://example.invalid/a.zip", "a.dsk"))
    }

    /** A failed request must be reported as null, not thrown at the caller. */
    @Test
    fun aFailedRequestGivesNull() = runTest {
        val downloader = FileDownloader(
            fetch = { throw IOException("GET failed with HTTP 404") },
            fileSystem = fileSystem,
            scratchDir = scratch,
        )

        assertNull(downloader.download("https://example.invalid/gone.rom", null))
    }
}

/**
 * Builds a ZIP archive in memory.
 *
 * Written by hand because there is no multiplatform ZIP *writer* — okio reads
 * them but does not create them. Stored entries only, so there is no compressor
 * to implement: a local header and data per entry, then the central directory
 * that okio actually reads.
 */
private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
    val out = mutableListOf<Byte>()
    val centralDirectory = mutableListOf<Byte>()

    fun MutableList<Byte>.u16(v: Int) {
        add((v and 0xFF).toByte()); add(((v shr 8) and 0xFF).toByte())
    }

    fun MutableList<Byte>.u32(v: Int) {
        u16(v and 0xFFFF); u16((v shr 16) and 0xFFFF)
    }

    for ((name, data) in entries) {
        val nameBytes = name.encodeToByteArray()
        val offset = out.size
        val crc = crc32(data)

        out.u32(0x04034b50)                 // local file header signature
        out.u16(10)                         // version needed
        out.u16(0)                          // flags
        out.u16(0)                          // method: stored
        out.u16(0); out.u16(0)              // mod time, mod date
        out.u32(crc)
        out.u32(data.size); out.u32(data.size)
        out.u16(nameBytes.size); out.u16(0)
        out.addAll(nameBytes.toList())
        out.addAll(data.toList())

        centralDirectory.u32(0x02014b50)    // central directory header signature
        centralDirectory.u16(20); centralDirectory.u16(10)
        centralDirectory.u16(0); centralDirectory.u16(0)
        centralDirectory.u16(0); centralDirectory.u16(0)
        centralDirectory.u32(crc)
        centralDirectory.u32(data.size); centralDirectory.u32(data.size)
        centralDirectory.u16(nameBytes.size)
        centralDirectory.u16(0); centralDirectory.u16(0)
        centralDirectory.u16(0); centralDirectory.u16(0)
        centralDirectory.u32(0)             // external attributes
        centralDirectory.u32(offset)
        centralDirectory.addAll(nameBytes.toList())
    }

    val directoryOffset = out.size
    out.addAll(centralDirectory)
    out.u32(0x06054b50)                     // end of central directory
    out.u16(0); out.u16(0)
    out.u16(entries.size); out.u16(entries.size)
    out.u32(centralDirectory.size)
    out.u32(directoryOffset)
    out.u16(0)                              // comment length
    return out.toByteArray()
}

private fun crc32(data: ByteArray): Int {
    var crc = 0xFFFFFFFFu
    for (byte in data) {
        crc = crc xor (byte.toUInt() and 0xFFu)
        repeat(8) {
            crc = if (crc and 1u != 0u) (crc shr 1) xor 0xEDB88320u else crc shr 1
        }
    }
    return (crc xor 0xFFFFFFFFu).toInt()
}
