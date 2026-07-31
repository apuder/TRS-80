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
 * The iOS implementation of trs80_audio.h, on AudioQueue.
 *
 * The counterpart of audio_opensl.c on Android, and deliberately the same
 * shape: two small buffers cycling, refilled from the core on the callback.
 * AudioQueue is a pull API already, so the sink's contract maps onto it
 * directly.
 */

#include <AudioToolbox/AudioToolbox.h>
#include <string.h>

#include "trs80_audio.h"

/* Matches audio_opensl.c, so both platforms buffer the same amount. */
#define BUFFER_SIZE 1024
#define NUM_BUFFERS 2

#define SAMPLE_RATE 44100
#define BITS_PER_CHANNEL 16
#define BYTES_PER_FRAME (BITS_PER_CHANNEL / 8)

static AudioQueueRef audio_queue = NULL;
static trs80_audio_fill fill_callback = NULL;

/*
 * Refills a drained buffer and hands it straight back to the queue. Called on
 * an AudioQueue-owned thread.
 */
static void refill(void *user_data, AudioQueueRef queue, AudioQueueBufferRef buffer)
{
    (void)user_data;

    trs80_audio_fill fill = fill_callback;
    if (fill == NULL) {
        /* Shutting down: hand back silence rather than stale samples. */
        memset(buffer->mAudioData, 0, BUFFER_SIZE);
    } else {
        fill((char *)buffer->mAudioData, BUFFER_SIZE);
    }
    buffer->mAudioDataByteSize = BUFFER_SIZE;
    AudioQueueEnqueueBuffer(queue, buffer, 0, NULL);
}

int trs80_audio_init(trs80_audio_fill fill)
{
    if (audio_queue != NULL) {
        return 1;
    }

    AudioStreamBasicDescription format;
    memset(&format, 0, sizeof(format));
    format.mSampleRate = SAMPLE_RATE;
    format.mFormatID = kAudioFormatLinearPCM;
    format.mFormatFlags = kAudioFormatFlagIsSignedInteger
            | kAudioFormatFlagsNativeEndian
            | kAudioFormatFlagIsPacked;
    format.mChannelsPerFrame = 1;
    format.mBitsPerChannel = BITS_PER_CHANNEL;
    format.mBytesPerFrame = BYTES_PER_FRAME;
    format.mFramesPerPacket = 1;
    format.mBytesPerPacket = BYTES_PER_FRAME;

    fill_callback = fill;

    /* A NULL run loop asks AudioQueue for its own callback thread, which is
       what we want: the core's CPU thread must not be blocked on audio. */
    if (AudioQueueNewOutput(&format, refill, NULL, NULL, NULL, 0, &audio_queue) != noErr) {
        audio_queue = NULL;
        fill_callback = NULL;
        return 0;
    }

    for (int i = 0; i < NUM_BUFFERS; i++) {
        AudioQueueBufferRef buffer;
        if (AudioQueueAllocateBuffer(audio_queue, BUFFER_SIZE, &buffer) != noErr) {
            trs80_audio_shutdown();
            return 0;
        }
        /* Prime it, so playback starts with samples rather than a gap. */
        refill(NULL, audio_queue, buffer);
    }

    if (AudioQueueStart(audio_queue, NULL) != noErr) {
        trs80_audio_shutdown();
        return 0;
    }
    return 1;
}

void trs80_audio_shutdown(void)
{
    if (audio_queue == NULL) {
        return;
    }
    /* Clear the callback first: refill() may already be running on the queue's
       thread, and must not call into the core once teardown has begun. */
    fill_callback = NULL;

    AudioQueueRef queue = audio_queue;
    audio_queue = NULL;

    AudioQueueStop(queue, true);
    AudioQueueDispose(queue, true);
}
