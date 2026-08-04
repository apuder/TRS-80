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
 * Nothing runs yet.
 *
 * The browser has one thread and Compose is drawing on it, so a call that does
 * not return cannot be made from here at all. When the core arrives -- built
 * with Emscripten, which is the only way C gets into a page -- it will run in a
 * worker and this will be what starts it, or the C loop will be cut into slices
 * driven by the frame callback and this will be what schedules the first one.
 * Either way the decision belongs here, which is why this file exists before
 * there is anything to put in it.
 */
actual fun runMachine(core: EmulatorCore): () -> Unit = {}
