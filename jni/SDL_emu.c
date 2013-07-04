/*
 * Copyright 2012-2013, Arno Puder
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

#include <stdlib.h>
#include <pthread.h>
#include "atrs.h"
#include "SDL_emu.h"

static SDL_AudioSpec audioSpec;
static pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;


void fillBuffer(Uint8* stream, int len) {
    pthread_mutex_lock(&mutex);
    audioSpec.callback(audioSpec.userdata, stream, len);
    pthread_mutex_unlock(&mutex);
}

int SDL_OpenAudio(SDL_AudioSpec *desired, SDL_AudioSpec *obtained)
{
    memcpy(obtained, desired, sizeof(SDL_AudioSpec));
    obtained->format = AUDIO_S16;
    obtained->silence = 0;
    memcpy(&audioSpec, obtained, sizeof(SDL_AudioSpec));
    init_audio(obtained->freq, obtained->channels, obtained->format, obtained->samples);
    return 0;
}

void SDL_CloseAudio(void)
{
    deinit_audio();
}

void SDL_PauseAudio(int pause_on)
{
    pause_audio(pause_on);
}

void SDL_LockAudio(void)
{
    pthread_mutex_lock(&mutex);
}

void SDL_UnlockAudio(void)
{
    pthread_mutex_unlock(&mutex);
}



