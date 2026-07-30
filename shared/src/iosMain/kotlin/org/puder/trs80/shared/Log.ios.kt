/*
 * Copyright 2025, Arno Puder
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

import platform.Foundation.NSLog

/**
 * NSLog, so lines reach the device console and `simctl launch --console` alike.
 *
 * The level is spelled into the message because NSLog has no notion of one.
 */
actual object Log {

    actual fun i(tag: String, message: String) {
        NSLog("I/%s: %s", tag, message)
    }

    actual fun w(tag: String, message: String) {
        NSLog("W/%s: %s", tag, message)
    }

    actual fun e(tag: String, message: String) {
        NSLog("E/%s: %s", tag, message)
    }

    actual fun e(tag: String, message: String, error: Throwable) {
        NSLog("E/%s: %s: %s", tag, message, error.toString())
    }
}
