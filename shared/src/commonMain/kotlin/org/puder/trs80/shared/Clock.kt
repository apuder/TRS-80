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
 * The wall clock, in milliseconds since the epoch.
 *
 * Only used to order the library by when a machine was last run, so it needs to
 * be monotonic-ish across launches rather than precise. A seam rather than a
 * direct call so that ordering can be tested without waiting for time to pass.
 */
expect fun currentTimeMillis(): Long
