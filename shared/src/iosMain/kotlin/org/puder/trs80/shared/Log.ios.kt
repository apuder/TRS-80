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

package org.puder.trs80.shared

/**
 * Standard output, which Xcode and `simctl launch --console` both show.
 *
 * The level is spelled into the line because there is no level to set.
 *
 * Deliberately *not* NSLog. NSLog is variadic, and Kotlin/Native does not bridge
 * Kotlin values into C variadic arguments — a Kotlin String arrives as a raw
 * pointer, so `%s` reads an object as characters and `%@` sends `description` to
 * something that is not an object. Both crash, and not at the call: the first
 * few lines print correctly and the process dies later, on another thread, deep
 * inside the Kotlin runtime.
 */
actual object Log {

    actual fun i(tag: String, message: String) = write("I", tag, message)

    actual fun w(tag: String, message: String) = write("W", tag, message)

    actual fun e(tag: String, message: String) = write("E", tag, message)

    actual fun e(tag: String, message: String, error: Throwable) =
        write("E", tag, "$message: $error")

    private fun write(level: String, tag: String, message: String) {
        println("$level/$tag: $message")
    }
}
