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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext

/** A thread of its very own; see the expect for why not a pooled one. */
@OptIn(DelicateCoroutinesApi::class)
actual fun runMachine(core: EmulatorCore): () -> Unit {
    val cpu = newSingleThreadContext("trs80-cpu")
    CoroutineScope(cpu).launch { core.run() }
    return {
        core.stop()
        cpu.close()
    }
}
