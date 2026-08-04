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
 * Starts the machine on the thread that is already here.
 *
 * The other platforms hand [EmulatorCore.run] a thread because it does not
 * return until the machine stops. In a browser it returns immediately: the run
 * loop's own frame pause is an emscripten_sleep, ASYNCIFY turns that into a
 * yield, and what comes back is a promise that settles when the machine stops.
 * So there is nothing to give it a thread for -- it hands this one straight
 * back, between every frame, which is what lets Compose keep drawing.
 */
actual fun runMachine(core: EmulatorCore): () -> Unit {
    core.run()
    return { core.stop() }
}
