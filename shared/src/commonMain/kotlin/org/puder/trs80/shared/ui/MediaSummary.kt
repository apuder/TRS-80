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

/**
 * How the record table states what a program is made of: "2 disks · 37.5K".
 *
 * Sizes are in the units the machine's own documentation used — K, and M only
 * when a number would otherwise run to four figures. Nothing here is ever
 * large enough to need more.
 */
fun mediaSummary(diskCount: Int, totalBytes: Long): String? {
    if (diskCount <= 0) {
        return null
    }
    val disks = if (diskCount == 1) "1 disk" else "$diskCount disks"
    if (totalBytes <= 0) {
        return disks
    }
    return "$disks · ${byteSize(totalBytes)}"
}

/** A byte count, short enough to sit at the end of a row. */
fun byteSize(bytes: Long): String {
    if (bytes < 1024) {
        return "${bytes}B"
    }
    val kilobytes = bytes / 1024.0
    if (kilobytes < 1000) {
        return "${round(kilobytes)}K"
    }
    return "${round(kilobytes / 1024.0)}M"
}

/** One decimal place, and none at all once the whole number carries the sense. */
private fun round(value: Double): String {
    val tenths = ((value * 10) + 0.5).toLong()
    val whole = tenths / 10
    val fraction = tenths % 10
    return if (fraction == 0L || whole >= 100) "$whole" else "$whole.$fraction"
}
