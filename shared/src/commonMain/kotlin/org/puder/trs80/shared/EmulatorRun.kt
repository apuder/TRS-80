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
 * Sets [core] running, somewhere that is not the thread that draws.
 *
 * A platform question, and only just: [EmulatorCore.run] does not return until
 * the machine is stopped, so the answer is a thread of its own on a device --
 * deliberately not a pool, because a call that never returns permanently takes
 * one of a handful of pooled threads, and on Darwin that pool is a global
 * dispatch queue whose loss breaks things far away from here.
 *
 * The browser has no thread to give. That is the whole of why this is an expect
 * and not four lines in the screen that starts a machine.
 *
 * @return what to call when the machine is finished with: it stops the core and
 * lets go of whatever was running it.
 */
expect fun runMachine(core: EmulatorCore): () -> Unit
