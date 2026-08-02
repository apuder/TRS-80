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

package org.puder.trs80.shared.ui

/** The extension a disk image is given if the name does not already carry one. */
private const val DISK_SUFFIX = ".dsk"

/**
 * The file name characters a disk image may be made of.
 *
 * The same set the Android app allowed. Narrow on purpose: the name reaches the
 * emulator core as a C string and is opened by path, so anything that would
 * need quoting or escaping has no business in it.
 */
private val LEGAL_NAME = Regex("[-_.A-Za-z0-9]+")

/**
 * The layouts a blank image can be written in.
 *
 * JV1 is the plainest and what most software expects; JV3 carries per-sector
 * flags, so it can hold mixed densities and deleted-data marks; DMK is a
 * track-level image and the only one with anything else to configure.
 */
enum class DiskFormat { JV1, JV3, DMK }

/**
 * What to write into a blank disk image.
 *
 * A plain value, so the screen that collects it has nothing to decide and this
 * has no drawing to test. The DMK parameters are carried whatever the format
 * is, and simply ignored by the other two — keeping them means switching format
 * to look at DMK and back does not silently forget what was set.
 */
data class DiskImageSpec(
    val name: String = "",
    val format: DiskFormat = DiskFormat.JV1,
    /** 1 or 2. DMK only. */
    val sides: Int = 1,
    /** DMK only. */
    val doubleDensity: Boolean = false,
    /** An 8-inch disk rather than 5¼. DMK only. */
    val eightInch: Boolean = false,
    /**
     * Lets the machine read a track at either density. DMK only.
     *
     * A real controller cares; some copy-protected software relies on it not
     * caring, which is what this is for.
     */
    val ignoreDensity: Boolean = false,
) {
    /** Whether [name] is something this app is willing to create a file called. */
    val nameIsLegal: Boolean get() = LEGAL_NAME.matches(name)

    /** Whether the DMK parameters have any effect on what would be written. */
    val dmkApplies: Boolean get() = format == DiskFormat.DMK

    /**
     * The file this would be written as, or null if the name is not usable.
     *
     * The suffix is added rather than required, because it is the app's
     * convention and not the user's problem.
     */
    val filename: String?
        get() {
            if (!nameIsLegal) {
                return null
            }
            return if (name.endsWith(DISK_SUFFIX, ignoreCase = true)) name else name + DISK_SUFFIX
        }

    /** The density as the core counts it: 1 single, 2 double. */
    val densityCode: Int get() = if (doubleDensity) 2 else 1
}

/** What came of asking for a blank disk image. */
sealed interface DiskCreation {
    /** Written, and here is where it went. */
    data class Created(val path: String) : DiskCreation

    /** Something of that name is already in this machine's folder. */
    data object NameTaken : DiskCreation

    /** The core would not write it. */
    data object Failed : DiskCreation
}
