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

private fun consoleInfo(line: String) { js("console.info(line)") }
private fun consoleWarn(line: String) { js("console.warn(line)") }
private fun consoleError(line: String) { js("console.error(line)") }

/** The browser's console, which is where a page says things. */
actual object Log {
    actual fun i(tag: String, message: String) = consoleInfo("$tag: $message")
    actual fun w(tag: String, message: String) = consoleWarn("$tag: $message")
    actual fun e(tag: String, message: String) = consoleError("$tag: $message")
    actual fun e(tag: String, message: String, error: Throwable) =
        consoleError("$tag: $message -- $error")
}
