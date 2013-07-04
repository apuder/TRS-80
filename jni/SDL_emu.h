/*
 * SDL_emu.h
 *
 *  Created on: Jul 3, 2013
 *      Author: arno
 */

#ifndef SDL_EMU_H_
#define SDL_EMU_H_

#define DEFAULT_SAMPLE_RATE 44100
#define AUDIO_U8 0
#define AUDIO_S16 1

typedef unsigned char Uint8;
typedef unsigned short Uint16;
typedef unsigned int Uint32;

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

#endif /* SDL_EMU_H_ */
