
#ifndef __OPENSL_H__
#define __OPENSL_H__

typedef void (*AndroidAudioCallback)(char *buffer, int buffer_size);

int OpenSLWrap_Init(AndroidAudioCallback cb);
void OpenSLWrap_Shutdown();

#endif
