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
 * The app's version, as the user should see it — e.g. "0.99 (49)".
 *
 * Shown at the foot of the settings screen, which is the one place a version
 * earns its keep: it is what someone reads back when reporting a problem. Which
 * is exactly why both platforms have to say the same thing, and why this is not
 * asked of the platform any more.
 *
 * Each used to answer for itself — Android from its package, iOS from its
 * bundle — and they disagreed, because only one of them had anywhere to read it
 * from that the build actually sets. Both now come from the same two lines in
 * gradle.properties, by way of the generated [VERSION_NAME] and [VERSION_CODE].
 */
fun appVersion(): String = "$VERSION_NAME ($VERSION_CODE)"
