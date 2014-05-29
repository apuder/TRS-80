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

#include <SDL/SDL.h>
#include <pthread.h>
#include <errno.h>
#include <android/log.h>
#include "atrs.h"

#define DEBUG_TAG "TRS80"

#define NOT_IMPLEMENTED() \
    char buf[1024]; \
    sprintf(buf, "%s:%d", __FILE__, __LINE__); \
    __android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NOT_IMPLEMENTED: %s", buf); \
    not_implemented(buf);

static struct timeval start_tv;
static SDL_bool ticks_started = SDL_FALSE;
static pthread_mutex_t mutex_kb = PTHREAD_MUTEX_INITIALIZER;

typedef struct {
    Uint16 event;
    Uint16 sym;
    Uint16 key;
} KeyBuffer;

#define MAX_KEY_BUFFER 30

static int keyBufferFirst = 0;
static int keyBufferLast = 0;
static KeyBuffer keyBuffer[MAX_KEY_BUFFER];

void add_key_event(Uint16 event, Uint16 sym, Uint16 key)
{
    pthread_mutex_lock(&mutex_kb);
    KeyBuffer nextKey;
    nextKey.event = event;
    nextKey.sym = sym;
    nextKey.key = key;
    keyBuffer[keyBufferLast] = nextKey;
    keyBufferLast = (keyBufferLast + 1) % MAX_KEY_BUFFER;
    if (keyBufferFirst == keyBufferLast) {
        keyBufferFirst = (keyBufferFirst + 1) % MAX_KEY_BUFFER;
    }
    pthread_mutex_unlock(&mutex_kb);
}

int SDLCALL SDL_PollEvent(SDL_Event *event)
{
    pthread_mutex_lock(&mutex_kb);
    if (keyBufferFirst == keyBufferLast) {
        pthread_mutex_unlock(&mutex_kb);
        return 0;
    }
    KeyBuffer* nextKey = &keyBuffer[keyBufferFirst];
    keyBufferFirst = (keyBufferFirst + 1) % MAX_KEY_BUFFER;
    event->type = nextKey->event;
    event->key.keysym.mod = 0;//nextKey->mod;
    event->key.keysym.sym = nextKey->sym;
    event->key.keysym.scancode = nextKey->key;
    event->key.keysym.unicode = nextKey->key;
    pthread_mutex_unlock(&mutex_kb);
    return 1;
}

int SDL_NumJoysticks()
{
    return 0;
}

SDL_Joystick* SDL_JoystickOpen(int num_joy_stick)
{
    return (SDL_Joystick*) 0;
}

void SDL_JoystickClose(SDL_Joystick* joy_stick)
{
    // Do nothing
}

Uint32 SDLCALL SDL_MapRGB
(const SDL_PixelFormat * const format,
 const Uint8 r, const Uint8 g, const Uint8 b)
{
    NOT_IMPLEMENTED();
}

Uint32 SDLCALL SDL_MapRGBA(const SDL_PixelFormat * format,
                                           Uint8 r, Uint8 g, Uint8 b,
                                           Uint8 a)
{
    NOT_IMPLEMENTED();
}

int SDLCALL SDL_LockSurface(SDL_Surface *surface)
{
    NOT_IMPLEMENTED();
}

void SDLCALL SDL_UnlockSurface(SDL_Surface *surface)
{
    NOT_IMPLEMENTED();
}

int SDLCALL SDL_FillRect
        (SDL_Surface *dst, SDL_Rect *dstrect, Uint32 color)
{
    NOT_IMPLEMENTED();
}

void SDLCALL SDL_UpdateRects
        (SDL_Surface *screen, int numrects, SDL_Rect *rects)
{
    NOT_IMPLEMENTED();
}

void SDLCALL SDL_UpdateRect
        (SDL_Surface *screen, Sint32 x, Sint32 y, Uint32 w, Uint32 h)
{
    NOT_IMPLEMENTED();
}

SDL_Surface * SDLCALL SDL_CreateRGBSurface
            (Uint32 flags, int width, int height, int depth,
            Uint32 Rmask, Uint32 Gmask, Uint32 Bmask, Uint32 Amask)
{
    NOT_IMPLEMENTED();
}

/** @sa SDL_CreateRGBSurface */
SDL_Surface * SDLCALL SDL_CreateRGBSurfaceFrom(void *pixels,
            int width, int height, int depth, int pitch,
            Uint32 Rmask, Uint32 Gmask, Uint32 Bmask, Uint32 Amask)
{
    NOT_IMPLEMENTED();
}

void SDLCALL SDL_FreeSurface(SDL_Surface *surface)
{
    NOT_IMPLEMENTED();
}

int SDLCALL SDL_UpperBlit
            (SDL_Surface *src, SDL_Rect *srcrect,
             SDL_Surface *dst, SDL_Rect *dstrect)
{
    NOT_IMPLEMENTED();
}

SDL_Surface * SDLCALL SDL_SetVideoMode
            (int width, int height, int bpp, Uint32 flags)
{
    NOT_IMPLEMENTED();
}

int SDLCALL SDL_SetPalette(SDL_Surface *surface, int flags,
                   SDL_Color *colors, int firstcolor,
                   int ncolors)
{
    NOT_IMPLEMENTED();
}

int SDLCALL SDL_EnableKeyRepeat(int delay, int interval)
{
    NOT_IMPLEMENTED();
}

int SDLCALL SDL_WaitEvent(SDL_Event *event)
{
    NOT_IMPLEMENTED();
}

Uint8 SDLCALL SDL_GetMouseState(int *x, int *y)
{
    return 0;
}

void SDLCALL SDL_WarpMouse(Uint16 x, Uint16 y)
{
    NOT_IMPLEMENTED();
}

void
SDL_InitTicks(void)
{
    if (ticks_started) {
        return;
    }
    ticks_started = SDL_TRUE;

    /* Set first ticks value */
    gettimeofday(&start_tv, NULL);
}

Uint32 SDLCALL SDL_GetTicks(void)
{
    Uint32 ticks;
    if (!ticks_started) {
        SDL_InitTicks();
    }

    struct timeval now;

    gettimeofday(&now, NULL);
    ticks =
            (now.tv_sec - start_tv.tv_sec) * 1000 + (now.tv_usec -
                                                  start_tv.tv_usec) / 1000;
    return (ticks);
}

void SDLCALL SDL_Delay(Uint32 ms)
{
    int was_error;

#if HAVE_NANOSLEEP
    struct timespec elapsed, tv;
#else
    struct timeval tv;
    Uint32 then, now, elapsed;
#endif

    /* Set the timeout interval */
#if HAVE_NANOSLEEP
    elapsed.tv_sec = ms / 1000;
    elapsed.tv_nsec = (ms % 1000) * 1000000;
#else
    then = SDL_GetTicks();
#endif
    do {
        errno = 0;

#if HAVE_NANOSLEEP
        tv.tv_sec = elapsed.tv_sec;
        tv.tv_nsec = elapsed.tv_nsec;
        was_error = nanosleep(&tv, &elapsed);
#else
        /* Calculate the time interval left (in case of interrupt) */
        now = SDL_GetTicks();
        elapsed = (now - then);
        then = now;
        if (elapsed >= ms) {
            break;
        }
        ms -= elapsed;
        tv.tv_sec = ms / 1000;
        tv.tv_usec = (ms % 1000) * 1000;

        was_error = select(0, NULL, NULL, NULL, &tv);
#endif /* HAVE_NANOSLEEP */
    } while (was_error && (errno == EINTR));
}

void SDLCALL SDL_WM_SetCaption(const char *title, const char *icon)
{
    NOT_IMPLEMENTED();
}

int SDLCALL SDL_ShowCursor(int toggle)
{
    NOT_IMPLEMENTED();
}

/* Audio */

static SDL_AudioSpec audioSpec;
static pthread_mutex_t mutex_snd = PTHREAD_MUTEX_INITIALIZER;


void fillBuffer(Uint8* stream, int len) {
    pthread_mutex_lock(&mutex_snd);
    audioSpec.callback(audioSpec.userdata, stream, len);
    pthread_mutex_unlock(&mutex_snd);
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
    close_audio();
}

void SDL_PauseAudio(int pause_on)
{
    pause_audio(pause_on);
}

void SDL_LockAudio(void)
{
    pthread_mutex_lock(&mutex_snd);
}

void SDL_UnlockAudio(void)
{
    pthread_mutex_unlock(&mutex_snd);
}
