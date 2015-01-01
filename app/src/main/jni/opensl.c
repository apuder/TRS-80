// Minimal audio streaming using OpenSL.
//
// Loosely based on the Android NDK sample code.
// Hardcoded to 44.1kHz stereo 16-bit audio, because as far as I'm concerned,
// that's the only format that makes any sense.

// Adapted from https://gist.github.com/hrydgard/3072540

#include <assert.h>
#include <string.h>

// for native audio
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

#include "opensl.h"

// This is kinda ugly, but for simplicity I've left these as globals just like in the sample,
// as there's not really any use case for this where we have multiple audio devices yet.

// engine interfaces
static SLObjectItf engineObject;
static SLEngineItf engineEngine;
static SLObjectItf outputMixObject;

// buffer queue player interfaces
static SLObjectItf bqPlayerObject = NULL;
static SLPlayItf bqPlayerPlay;
static SLAndroidSimpleBufferQueueItf bqPlayerBufferQueue;
static SLMuteSoloItf bqPlayerMuteSolo;
static SLVolumeItf bqPlayerVolume;

#define BUFFER_SIZE (1024)
#define NUM_BUFFERS 2

// Double buffering.
static char buffer[NUM_BUFFERS][BUFFER_SIZE];
static int curBuffer = 0;

static AndroidAudioCallback audioCallback;

// This callback handler is called every time a buffer finishes playing.
// The documentation available is very unclear about how to best manage buffers.
// I've chosen to this approach: Instantly enqueue a buffer that was rendered to the last time,
// and then render the next. Hopefully it's okay to spend time in this callback after having enqueued.
static void bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *context) {
  assert(bq == bqPlayerBufferQueue);
  assert(NULL == context);

  char *nextBuffer = buffer[curBuffer];

  SLresult result;
  result = (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue, nextBuffer, BUFFER_SIZE);

  // Comment from sample code:
  // the most likely other result is SL_RESULT_BUFFER_INSUFFICIENT,
  // which for this code example would indicate a programming error
  assert(SL_RESULT_SUCCESS == result);

  curBuffer = (curBuffer + 1) % NUM_BUFFERS;  // Switch buffer
  // Render to the fresh buffer
  audioCallback(buffer[curBuffer], BUFFER_SIZE);
}

// create the engine and output mix objects
int OpenSLWrap_Init(AndroidAudioCallback cb) {
  audioCallback = cb;

  SLresult result;
  // create engine
  result = slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
  if(SL_RESULT_SUCCESS != result) {
    goto FAIL;
  }
  result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
  if(SL_RESULT_SUCCESS != result) {
    goto FAIL;
  }
  result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);
  if(SL_RESULT_SUCCESS != result) {
    goto FAIL;
  }
  result = (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 0, 0, 0);
  if(SL_RESULT_SUCCESS != result) {
    goto FAIL;
  }
  result = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
  if(SL_RESULT_SUCCESS != result) {
    goto FAIL;
  }

  SLDataLocator_AndroidSimpleBufferQueue loc_bufq = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2};
  SLDataFormat_PCM format_pcm = {
    SL_DATAFORMAT_PCM,
    1,//2,
    SL_SAMPLINGRATE_44_1,
    SL_PCMSAMPLEFORMAT_FIXED_16,
    SL_PCMSAMPLEFORMAT_FIXED_16,
    SL_SPEAKER_FRONT_CENTER,//SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT,
    SL_BYTEORDER_LITTLEENDIAN
  };

  SLDataSource audioSrc = {&loc_bufq, &format_pcm};

  // configure audio sink
  SLDataLocator_OutputMix loc_outmix = {SL_DATALOCATOR_OUTPUTMIX, outputMixObject};
  SLDataSink audioSnk = {&loc_outmix, NULL};

  // create audio player
  const SLInterfaceID ids[2] = {SL_IID_BUFFERQUEUE, SL_IID_VOLUME};
  const SLboolean req[2] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};
  result = (*engineEngine)->CreateAudioPlayer(engineEngine, &bqPlayerObject, &audioSrc, &audioSnk, 2, ids, req);
  if(SL_RESULT_SUCCESS != result) {
    goto FAIL;
  }

  result = (*bqPlayerObject)->Realize(bqPlayerObject, SL_BOOLEAN_FALSE);
  if(SL_RESULT_SUCCESS != result) {
    goto FAIL;
  }
  result = (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_PLAY, &bqPlayerPlay);
  if(SL_RESULT_SUCCESS != result) {
    goto FAIL;
  }
  result = (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_BUFFERQUEUE,
    &bqPlayerBufferQueue);
  if(SL_RESULT_SUCCESS != result) {
    goto FAIL;
  }
  result = (*bqPlayerBufferQueue)->RegisterCallback(bqPlayerBufferQueue, bqPlayerCallback, NULL);
  if(SL_RESULT_SUCCESS != result) {
    goto FAIL;
  }
  result = (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_VOLUME, &bqPlayerVolume);
  if(SL_RESULT_SUCCESS != result) {
    goto FAIL;
  }
  result = (*bqPlayerPlay)->SetPlayState(bqPlayerPlay, SL_PLAYSTATE_PLAYING);
  if(SL_RESULT_SUCCESS != result) {
    goto FAIL;
  }

  // Render and enqueue a first buffer. (or should we just play the buffer empty?)
  curBuffer = 0;
  audioCallback(buffer[curBuffer], BUFFER_SIZE);

  result = (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue, buffer[curBuffer], BUFFER_SIZE);
  if (SL_RESULT_SUCCESS != result) {
    goto FAIL;
  }
  SUCCESS:
  return 1;
  FAIL:
  return 0;
}

// shut down the native audio system
void OpenSLWrap_Shutdown() {
  if (bqPlayerObject != NULL) {
    (*bqPlayerObject)->Destroy(bqPlayerObject);
    bqPlayerObject = NULL;
    bqPlayerPlay = NULL;
    bqPlayerBufferQueue = NULL;
    bqPlayerMuteSolo = NULL;
    bqPlayerVolume = NULL;
  }
  if (outputMixObject != NULL) {
    (*outputMixObject)->Destroy(outputMixObject);
    outputMixObject = NULL;
  }
  if (engineObject != NULL) {
    (*engineObject)->Destroy(engineObject);
    engineObject = NULL;
    engineEngine = NULL;
  }
}
