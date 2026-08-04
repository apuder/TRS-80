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

/*
 * A machine that makes no sound.
 *
 * The browser's backend, for now. Audio in a page is an AudioWorklet fed from
 * another thread, and a worklet cannot be started at all until the user has
 * interacted with the page -- so it is a piece of work with a shape of its own,
 * and one the emulator does not have to wait for. The core asks for a sink,
 * this says there is none, and everything else about a running machine works.
 *
 * Saying no rather than accepting and discarding: trs80_audio_init() returning
 * zero is how the core is told there is no sound, which is the same thing it is
 * told when a device's audio hardware will not open.
 */

#include "trs80_audio.h"

int trs80_audio_init(trs80_audio_fill fill)
{
    (void) fill;
    return 0;
}

void trs80_audio_shutdown(void)
{
}
