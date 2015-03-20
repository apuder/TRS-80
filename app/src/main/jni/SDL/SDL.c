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
#include "opensl.h"

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
 const Uint8 red, const Uint8 green, const Uint8 blue)
{
    // Convert to RGB_565
    unsigned short b = (blue >> 3) & 0x001f;
    unsigned short g = ((green >> 2) & 0x003f) << 6;
    unsigned short r = ((red >> 3) & 0x001f) << 11;

    return (Uint32) (r | g | b);
}

Uint32 SDLCALL SDL_MapRGBA(const SDL_PixelFormat * format,
                                           Uint8 r, Uint8 g, Uint8 b,
                                           Uint8 a)
{
    // Ignore alpha
    return SDL_MapRGB(format, r, g, b);
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
    int x, y;
    Uint8 r = color >> 16;
    Uint8 g = color >> 8;
    Uint8 b = color & 0xff;
    Uint16 dot = SDL_MapRGB(NULL, r, g, b);
    Uint16* pixels = (Uint16*) dst->pixels;
    int dstrect_x, dstrect_y, dstrect_w, dstrect_h;
    if (dstrect == NULL) {
        dstrect_x = 0;
        dstrect_y = 0;
        dstrect_w = dst->w;
        dstrect_h = dst->h;
    } else {
        dstrect_x = dstrect->x;
        dstrect_y = dstrect->y;
        dstrect_w = dstrect->w;
        dstrect_h = dstrect->h;
    }
    for (x = 0; x < dstrect_w; x++) {
        for (y = 0; y < dstrect_h; y++) {
            int i = (x + dstrect_x) + (y + dstrect_y) * dst->w;
            pixels[i] = dot;
        }
    }
    return 0;
}

void trigger_sdl_screen_update(void* pixels, int nbytes);
void SDLCALL SDL_UpdateRects
        (SDL_Surface *screen, int numrects, SDL_Rect *rects)
{
    trigger_sdl_screen_update(screen->pixels, screen->w * screen->h * 2);
    // NOT_IMPLEMENTED();
}

void SDLCALL SDL_UpdateRect
        (SDL_Surface *screen, Sint32 x, Sint32 y, Uint32 w, Uint32 h)
{
    SDL_Rect rect;
    if ((x | y | w | h) == 0) {
        rect.x = 0;
        rect.y = 0;
        rect.w = screen->w;
        rect.h = screen->h;
    } else {
        rect.x = x;
        rect.y = y;
        rect.w = w;
        rect.h = h;
    }
    SDL_UpdateRects(screen, 1, &rect);
}


static void* convert_pixels(void* pixels, int width, int height, int depth)
{
    if (depth != 32) {
        return pixels;
    }
    // sdltrs uses depth == 32 for font surfaces
    Uint16* dst_pixels = (Uint16*) malloc(width * height * 2);
    Uint32* src_pixels = (Uint32*) pixels;
    int i;
    for (i = 0; i < height * width; i++) {
        Uint32 pixel = src_pixels[i];
        Uint8 r = pixel >> 16;
        Uint8 g = pixel >> 8;
        Uint8 b = pixel & 0xff;
        dst_pixels[i] = SDL_MapRGB(NULL, r, g, b);
    }
    return dst_pixels;
}

static SDL_Surface* create_sdl_surface(void* pixels, int width, int height, int depth,
        Uint32 Rmask, Uint32 Gmask, Uint32 Bmask, Uint32 Amask)
{
    SDL_Palette* palette = (SDL_Palette*) malloc(sizeof(SDL_Palette));
    SDL_PixelFormat *format = (SDL_PixelFormat*) malloc(sizeof(SDL_PixelFormat));
    SDL_Surface* surface = (SDL_Surface*) malloc(sizeof(SDL_Surface));
    memset(palette, 0, sizeof(SDL_Palette));
    memset(format, 0, sizeof(SDL_PixelFormat));
    memset(surface, 0, sizeof(SDL_Surface));
    format->BitsPerPixel = 8;
    format->BytesPerPixel = 2;
    format->Rmask = Rmask;
    format->Gmask = Gmask;
    format->Bmask = Bmask;
    format->Amask = Amask;
    format->palette = palette;
    surface->format = format;
    surface->w = width;
    surface->h = height;
    if (pixels != NULL) {
        surface->provided_pixels = 1;
        surface->pixels = convert_pixels(pixels, width, height, depth);
    } else {
        surface->pixels = malloc(width * height * 2 /* RGB_565 */);
    }
    return surface;
}

SDL_Surface * SDLCALL SDL_CreateRGBSurface
            (Uint32 flags, int width, int height, int depth,
            Uint32 Rmask, Uint32 Gmask, Uint32 Bmask, Uint32 Amask)
{
    return create_sdl_surface(NULL, width, height, depth, Rmask, Gmask, Bmask, Amask);
}


/** @sa SDL_CreateRGBSurface */
SDL_Surface * SDLCALL SDL_CreateRGBSurfaceFrom(void *pixels,
            int width, int height, int depth, int pitch,
            Uint32 Rmask, Uint32 Gmask, Uint32 Bmask, Uint32 Amask)
{
    SDL_Surface* surface = create_sdl_surface(pixels, width, height, depth, Rmask, Gmask, Bmask, Amask);
    surface->pitch = pitch;

    return surface;
}

void SDLCALL SDL_FreeSurface(SDL_Surface *surface)
{
    if (surface->format->palette->colors != NULL) {
        free(surface->format->palette->colors);
    }
    free(surface->format->palette);
    free(surface->format);
    if (!surface->provided_pixels) {
        free(surface->pixels);
    }
    free(surface);
}

int SDLCALL SDL_UpperBlit
            (SDL_Surface *src, SDL_Rect *srcrect,
             SDL_Surface *dst, SDL_Rect *dstrect)
{
    Uint16* src_pixels = (Uint16*) src->pixels;
    Uint16* dst_pixels = (Uint16*) dst->pixels;
    int src_x, src_y, src_w, src_h;
    if (srcrect == NULL) {
        src_x = src_y = 0;
        src_w = src->w;
        src_h = src->h;
    } else {
        src_x = srcrect->x;
        src_y = srcrect->y;
        src_w = srcrect->w;
        src_h = srcrect->h;
    }

    int dst_x = 0, dst_y = 0;
    if (dstrect != NULL) {
        dst_x = dstrect->x;
        dst_y = dstrect->y;
    }

    int x, y;
    for (x = 0; x < src_w; x++) {
        for (y = 0; y < src_h; y++) {
            int i = (x + src_x) + (y + src_y) * src->w;
            int j = (x + dst_x) + (y + dst_y) * dst->w;
            dst_pixels[j] = src_pixels[i];
        }
    }
    return 0;
}


static SDL_Surface* screen_surface = NULL;

SDL_Surface * SDLCALL SDL_SetVideoMode
            (int width, int height, int bpp, Uint32 flags)
{
    if (flags != SDL_ANYFORMAT) {
        NOT_IMPLEMENTED();
    }
    if (screen_surface != NULL) {
        SDL_FreeSurface(screen_surface);
    }
    screen_surface = create_sdl_surface(NULL, width, height, 0, 0, 0, 0, 0);
    // screen_surface->format->BytesPerPixel = bpp;
    screen_surface->flags = flags;
    return screen_surface;
}

int SDLCALL SDL_SetPalette(SDL_Surface *surface, int flags,
                   SDL_Color *colors, int firstcolor,
                   int ncolors)
{
    if (surface->format->palette->colors != NULL) {
        free(surface->format->palette->colors);
    }
    int n = sizeof(SDL_Color) * ncolors;
    surface->format->palette->colors = (SDL_Color*) malloc(n);
    memcpy(surface->format->palette->colors, colors + firstcolor, n);
    surface->format->palette->ncolors = ncolors;
    return 1;
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
    // Do nothing
}

int SDLCALL SDL_ShowCursor(int toggle)
{
    return -1;
}

/* Audio */

int sdl_audio_muted = 0;

static SDL_AudioSpec audioSpec;
static pthread_mutex_t mutex_snd = PTHREAD_MUTEX_INITIALIZER;
static pthread_mutex_t mutex_engine = PTHREAD_MUTEX_INITIALIZER;

static void fillBuffer(char *buffer, int buffer_size) {
    pthread_mutex_lock(&mutex_snd);
    audioSpec.callback(audioSpec.userdata, buffer, buffer_size);
    pthread_mutex_unlock(&mutex_snd);
}

int SDL_OpenAudio(SDL_AudioSpec *desired, SDL_AudioSpec *obtained)
{
    memcpy(obtained, desired, sizeof(SDL_AudioSpec));
    obtained->format = AUDIO_S16;
    obtained->channels = 1;
    obtained->silence = 0;
    memcpy(&audioSpec, obtained, sizeof(SDL_AudioSpec));
    if (sdl_audio_muted) {
        return 0;
    }
    pthread_mutex_lock(&mutex_engine);
    int result = OpenSLWrap_Init(fillBuffer) == 1 ? 0 : -1;
    pthread_mutex_unlock(&mutex_engine);
    return result;
}

void SDL_CloseAudio(void)
{
    // Can be called from different threads
    pthread_mutex_lock(&mutex_engine);
    OpenSLWrap_Shutdown();
    pthread_mutex_unlock(&mutex_engine);
}

void SDL_PauseAudio(int pause_on)
{
    // Do nothing
}

void SDL_LockAudio(void)
{
    pthread_mutex_lock(&mutex_snd);
}

void SDL_UnlockAudio(void)
{
    pthread_mutex_unlock(&mutex_snd);
}
