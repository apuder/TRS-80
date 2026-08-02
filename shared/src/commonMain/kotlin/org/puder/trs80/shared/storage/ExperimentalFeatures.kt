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

package org.puder.trs80.shared.storage

import com.russhwolf.settings.Settings

/**
 * How many taps on the wordmark it takes to find the experimental settings.
 *
 * The same number Android's build-number tap has used for years, and the same
 * idea: enough that nobody arrives by accident, few enough that someone who has
 * been told to do it does not give up.
 */
const val TAPS_TO_UNLOCK = 10

/**
 * How long a run of taps stays open, in milliseconds.
 *
 * Without this the count is cumulative forever, and ten separate visits to the
 * library over a month would unlock it. Long enough not to punish a slow hand.
 */
const val TAP_RUN_TIMEOUT_MILLIS = 3000L

/**
 * The features that are not finished, and how they are reached.
 *
 * Two gates, not one. Tapping the wordmark reveals that experimental settings
 * exist; each feature is then off until it is turned on. So finding the door is
 * not the same as walking through it, and someone who taps out of curiosity does
 * not silently acquire half-built behaviour.
 *
 * Both gates persist, because a setting that resets on relaunch is a setting the
 * user has to keep re-finding.
 */
class ExperimentalFeatures(private val settings: Settings) {

    /** Whether the experimental section is shown in settings at all. */
    val isUnlocked: Boolean
        get() = settings.getBoolean(StorageKeys.EXPERIMENTAL_UNLOCKED, false)

    /**
     * Whether sharing a machine's state to RetroStore is offered.
     *
     * Never true while locked: turning a feature on and then hiding the section
     * that turned it on would leave no way back.
     */
    val isShareEnabled: Boolean
        get() = isUnlocked && settings.getBoolean(StorageKeys.EXPERIMENTAL_SHARE, false)

    fun setShareEnabled(enabled: Boolean) =
        settings.putBoolean(StorageKeys.EXPERIMENTAL_SHARE, enabled)

    /** Opens the section. Idempotent. */
    fun unlock() = settings.putBoolean(StorageKeys.EXPERIMENTAL_UNLOCKED, true)
}

/**
 * Counts a run of taps and says when it has gone far enough.
 *
 * Kept apart from the stored flag so the counting can be tested without a store
 * and without a clock: the caller supplies the time, which is also what lets the
 * timeout be exercised rather than waited out.
 */
class TapRun(
    private val target: Int = TAPS_TO_UNLOCK,
    private val timeoutMillis: Long = TAP_RUN_TIMEOUT_MILLIS,
) {
    private var count = 0
    private var lastAt = 0L

    /** How many more taps are needed, once enough have landed to be worth saying. */
    var remaining: Int = target
        private set

    /**
     * Records a tap.
     *
     * @param now the current time in milliseconds.
     * @return whether this tap completed the run. Only true once per run.
     */
    fun tap(now: Long): Boolean {
        count = if (lastAt != 0L && now - lastAt > timeoutMillis) 1 else count + 1
        lastAt = now
        remaining = (target - count).coerceAtLeast(0)
        if (count < target) {
            return false
        }
        // Reset, so a run that keeps going does not report success on every tap
        // after the tenth.
        count = 0
        lastAt = 0L
        remaining = target
        return true
    }
}
