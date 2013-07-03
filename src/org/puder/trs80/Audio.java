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

import java.util.Vector;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class Audio implements Runnable {

    final private static int DEFAULT_SAMPLE_RATE = 44100;

    final int                BUF_SIZE            = 1024;

    private AudioTrack       audioTrack;

    private Thread           audioThread;

    private Vector<byte[]>   queue;

    private boolean          isRunning;

    private byte[]           data                = null;

    public Audio() {
        data = new byte[1024];
        XTRS.setAudioBuffer(data);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, DEFAULT_SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_8BIT, BUF_SIZE,
                AudioTrack.MODE_STREAM);
        audioTrack.play();
        queue = new Vector<byte[]>();
        isRunning = true;
        audioThread = new Thread(this);
        audioThread.start();
    }

    public synchronized void playSound() {
        queue.add(data.clone());
        this.notify();
    }

    public void stop() {
        isRunning = false;
    }

    @Override
    public synchronized void run() {
        while (isRunning) {
            try {
                this.wait();
                while (queue.size() > 0) {
                    byte[] data = queue.firstElement();
                    queue.remove(0);
                    audioTrack.write(data, 0, data.length);
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
