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

/**
 * Logging, in the shape the code being moved into `commonMain` already used.
 *
 * Deliberately the same three calls `android.util.Log` offers rather than a
 * logging framework: the point is that ported code keeps reading the way it
 * did, and every host already has somewhere for a tagged line to go.
 */
expect object Log {

    fun i(tag: String, message: String)

    fun w(tag: String, message: String)

    fun e(tag: String, message: String)

    fun e(tag: String, message: String, error: Throwable)
}
