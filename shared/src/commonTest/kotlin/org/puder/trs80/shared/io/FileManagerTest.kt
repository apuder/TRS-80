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

import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileManagerTest {

    private val fileSystem = FakeFileSystem()
    private val creator = FileManager.Creator("/data/TRS-80".toPath(), fileSystem)

    @AfterTest
    fun tearDown() {
        fileSystem.checkNoOpenFiles()
    }

    @Test
    fun writesAndReadsBack() {
        val files = creator.forAppBaseDir()

        assertTrue(files.writeFile("disk.dsk", byteArrayOf(1, 2, 3)))
        assertContentEquals(byteArrayOf(1, 2, 3), files.readFile("disk.dsk"))
    }

    @Test
    fun readingSomethingThatIsNotThereGivesNull() {
        assertNull(creator.forAppBaseDir().readFile("absent.dsk"))
    }

    @Test
    fun aConfigurationGetsItsOwnDirectory() {
        val one = creator.createForAppSubDir(1)
        val two = creator.createForAppSubDir(2)

        one.writeFile("disk.dsk", byteArrayOf(1))
        two.writeFile("disk.dsk", byteArrayOf(2))

        assertContentEquals(byteArrayOf(1), one.readFile("disk.dsk"))
        assertContentEquals(byteArrayOf(2), two.readFile("disk.dsk"))
        assertTrue(one.getAbsolutePathForFile("disk.dsk").endsWith("/1/disk.dsk"))
    }

    /** Deleting a configuration has to take its disk images with it. */
    @Test
    fun deleteRemovesTheDirectoryAndItsContents() {
        val files = creator.createForAppSubDir(7)
        files.writeFile("a.dsk", byteArrayOf(1))
        files.writeFile("b.dsk", byteArrayOf(2))

        files.delete()

        assertFalse(fileSystem.exists("/data/TRS-80/7".toPath()))
    }

    @Test
    fun deletingOneFileLeavesTheOthers() {
        val files = creator.forAppBaseDir()
        files.writeFile("a.dsk", byteArrayOf(1))
        files.writeFile("b.dsk", byteArrayOf(2))

        assertTrue(files.deleteFile("a.dsk"))

        assertFalse(files.hasFile("a.dsk"))
        assertTrue(files.hasFile("b.dsk"))
    }

    /** The old implementation returned true for an absent file; keep that. */
    @Test
    fun deletingSomethingAbsentSucceeds() {
        assertTrue(creator.forAppBaseDir().deleteFile("neverThere.dsk"))
    }

    @Test
    fun countingAndFindingFiles() {
        val files = creator.forAppBaseDir()
        assertEquals(0, files.fileCount())

        files.writeFile("a.dsk", byteArrayOf(1))
        files.writeFile("b.dsk", byteArrayOf(2))

        assertEquals(2, files.fileCount())
        assertTrue(files.hasFile("a.dsk"))
        assertFalse(files.hasFile("c.dsk"))
    }

    /**
     * A directory that was deleted underneath a live manager must not throw --
     * the original tolerated it because an ACRA report showed it happening.
     */
    @Test
    fun aVanishedDirectoryReadsAsEmpty() {
        val files = creator.createForAppSubDir(3)
        files.writeFile("a.dsk", byteArrayOf(1))
        fileSystem.deleteRecursively("/data/TRS-80/3".toPath())

        assertEquals(0, files.fileCount())
        assertFalse(files.hasFile("a.dsk"))
    }

    @Test
    fun ensureNoMediaIsIdempotent() {
        val files = creator.forAppBaseDir()

        assertTrue(files.ensureNoMedia())
        assertTrue(files.ensureNoMedia())
        assertTrue(files.hasFile(".nomedia"))
    }
}
