/*
 * trs80_audio.h - the audio sink the emulator core plays through.
 *
 * The core generates 16-bit signed mono samples at 44.1 kHz and hands them to
 * whatever sink the platform provides. Each platform supplies one
 * implementation of this header: audio_opensl.c on Android, and an
 * AudioQueue/AVAudioEngine equivalent on iOS.
 *
 * The sink pulls: it calls back into `fill` whenever it needs another buffer.
 */

#ifndef TRS80_AUDIO_H
#define TRS80_AUDIO_H

#ifdef __cplusplus
extern "C" {
#endif

/* Called by the sink to request buffer_size bytes of samples. */
typedef void (*trs80_audio_fill)(char *buffer, int buffer_size);

/* Start playback. Returns 1 on success, 0 on failure. */
int trs80_audio_init(trs80_audio_fill fill);

/* Stop playback and release the device. Safe to call when not started. */
void trs80_audio_shutdown(void);

#ifdef __cplusplus
}
#endif

#endif /* TRS80_AUDIO_H */
