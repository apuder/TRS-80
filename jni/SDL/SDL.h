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

#ifndef SDL_H_
#define SDL_H_

#include <SDL/SDL_timer.h>
#include <SDL/SDL_video.h>
#include <SDL/SDL_events.h>


#define SDL_HAT_CENTERED   0x00
#define SDL_HAT_UP         0x01
#define SDL_HAT_RIGHT      0x02
#define SDL_HAT_DOWN       0x04
#define SDL_HAT_LEFT       0x08
#define SDL_HAT_RIGHTUP    (SDL_HAT_RIGHT|SDL_HAT_UP)
#define SDL_HAT_RIGHTDOWN  (SDL_HAT_RIGHT|SDL_HAT_DOWN)
#define SDL_HAT_LEFTUP     (SDL_HAT_LEFT|SDL_HAT_UP)
#define SDL_HAT_LEFTDOWN   (SDL_HAT_LEFT|SDL_HAT_DOWN)

/*
typedef void SDL_Joystick;
int SDL_NumJoysticks();
SDL_Joystick* SDL_JoystickOpen(int num_joy_stick);
void SDL_JoystickClose(SDL_Joystick* joy_stick);
*/


#define DEFAULT_SAMPLE_RATE 44100
#define AUDIO_U8 0
#define AUDIO_S16 1

typedef struct{
  int freq;
  Uint16 format;
  Uint8 channels;
  Uint8 silence;
  Uint16 samples;
  Uint32 size;
  void (*callback)(void *userdata, Uint8 *stream, int len);
  void *userdata;
} SDL_AudioSpec;

int SDL_OpenAudio(SDL_AudioSpec *desired, SDL_AudioSpec *obtained);
void SDL_CloseAudio(void);
void SDL_PauseAudio(int pause_on);
void SDL_LockAudio(void);
void SDL_UnlockAudio(void);

#endif /* SDL_H_ */
