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

package org.puder.trs80;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

class Audio implements Runnable {

    private boolean    isRunning;
    private AudioTrack audioTrack;
    private byte[]     audioBuffer;
    private int        audioBufSize;

    public Audio(int rate, int channels, int encoding, int bufSize) {
        isRunning = true;
        channels = (channels == 1) ? AudioFormat.CHANNEL_CONFIGURATION_MONO
                : AudioFormat.CHANNEL_CONFIGURATION_STEREO;
        encoding = (encoding == 1) ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;

        audioBufSize = bufSize;

        // int min = AudioTrack.getMinBufferSize( rate, channels, encoding );

        if (AudioTrack.getMinBufferSize(rate, channels, encoding) > bufSize)
            bufSize = AudioTrack.getMinBufferSize(rate, channels, encoding);

        audioBuffer = new byte[bufSize];

        XTRS.setAudioBuffer(audioBuffer);

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, rate, channels, encoding, bufSize,
                AudioTrack.MODE_STREAM);
        audioTrack.play();
    }

    public void deinitAudio() {
        setRunning(false);
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
        }
    }

    public void pauseAudioPlayback() {
        if (audioTrack != null) {
            audioTrack.pause();
        }
    }

    public void resumeAudioPlayback() {
        if (audioTrack != null) {
            audioTrack.play();
        }
    }

    public void setRunning(boolean flag) {
        isRunning = flag;
    }

    @Override
    public void run() {
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        while (isRunning) {
            XTRS.fillAudioBuffer();
            audioTrack.write(audioBuffer, 0, audioBufSize);
        }
    }

}
