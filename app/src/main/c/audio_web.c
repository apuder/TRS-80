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
 * The browser's audio sink.
 *
 * A page cannot open an audio output on its own -- one exists only after the
 * user has touched something, and it lives on the audio thread, which is not
 * this one. So nothing here plays anything. What it does is hold the callback
 * the core registers and let JavaScript pull samples out of it whenever the
 * AudioWorklet needs more, which inverts the arrangement the devices use:
 * there the sink calls the core when the hardware is hungry, and here the page
 * asks the core when the worklet is.
 *
 * Signed 16-bit, one channel, 44100 -- the same as the AudioQueue backend on
 * iOS, and what the SDL shim tells the core it obtained.
 */

#include <stddef.h>

#include "trs80_audio.h"

static trs80_audio_fill fill_callback = NULL;

int trs80_audio_init(trs80_audio_fill fill)
{
    fill_callback = fill;
    /* One means the core has a sink and should go on making sound. */
    return 1;
}

void trs80_audio_shutdown(void)
{
    fill_callback = NULL;
}

/*
 * Fills dest with the next samples, and says how many it wrote.
 *
 * Zero means there is nothing playing -- no sink registered, which is what a
 * muted machine looks like from here -- and the caller should play silence
 * rather than repeat whatever was in the buffer.
 */
int trs80_audio_pull(short *dest, int samples)
{
    if (fill_callback == NULL || dest == NULL || samples <= 0) {
        return 0;
    }
    fill_callback((char *) dest, samples * (int) sizeof(short));
    return samples;
}
