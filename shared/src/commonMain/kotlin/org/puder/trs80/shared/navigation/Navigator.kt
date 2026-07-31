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

package org.puder.trs80.shared.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Where the app is, and how it gets somewhere else.
 *
 * The back stack is a plain observable list that this owns and hands out — not a
 * graph declared up front and driven by a library. That is deliberate: it is the
 * shape Navigation 3 expects, so when there are screens to render this can be
 * handed to a `NavDisplay` rather than replaced by one, and it is also the shape
 * that makes the whole thing testable without a UI, which is what the tests
 * below rely on.
 *
 * The Android app currently expresses all of this as `Intent`s,
 * `startActivityForResult` and request codes. Two things get better in the
 * translation and are worth naming, because they are bugs today rather than
 * stylistic differences: a destination is a typed value instead of a bag of
 * extras that can silently be missing, and a result is delivered exactly once to
 * the caller rather than to whichever Activity instance happens to exist.
 */
class Navigator(private val stack: MutableList<Destination>) {

    /** For tests and for hosts that do not need the stack restored. */
    constructor(root: Destination = Destination.ConfigurationList) :
        this(mutableStateListOf(root))

    private var result by mutableStateOf<NavigationResult?>(null)

    /**
     * The stack, oldest first. Always has at least the root in it.
     *
     * Observable, so reading it in a composable recomposes when it changes —
     * which is the point of it. Note it does *not* compare equal to an ordinary
     * list of the same contents, so compare contents rather than the lists.
     */
    val backStack: List<Destination> get() = stack

    /** Where the app is now. */
    val current: Destination get() = stack.last()

    /** Whether there is anywhere to go back to. False at the root. */
    val canGoBack: Boolean get() = stack.size > 1

    /**
     * Goes to [destination].
     *
     * Asking for the destination the app is already on does nothing. That is not
     * a nicety: every one of these is reached by tapping something, and a second
     * tap that lands before the first has drawn would otherwise push a duplicate
     * that the user then has to dismiss twice.
     */
    fun goTo(destination: Destination) {
        if (destination == current) {
            return
        }
        stack.add(destination)
    }

    /**
     * Goes back one step, optionally leaving [result] for the destination
     * underneath.
     *
     * @return whether it went anywhere. False at the root, which is what a host
     * needs in order to let the platform handle Back itself — closing the app on
     * Android, and doing nothing on iOS.
     */
    fun goBack(result: NavigationResult? = null): Boolean {
        if (!canGoBack) {
            return false
        }
        stack.removeAt(stack.lastIndex)
        // After the pop, so a destination reading its result is already the
        // current one by the time it can see it.
        this.result = result
        return true
    }

    /** Goes back to the root, discarding everything above it. */
    fun goBackToRoot() {
        if (canGoBack) {
            stack.subList(1, stack.size).clear()
        }
    }

    /**
     * Takes the result left by whatever was last dismissed, if there is one.
     *
     * One-shot: a second call gives null. Results are actions the caller has to
     * perform — restore a backup, drop a saved state — and performing them twice
     * is worse than not at all.
     */
    fun takeResult(): NavigationResult? = result.also { result = null }
}
