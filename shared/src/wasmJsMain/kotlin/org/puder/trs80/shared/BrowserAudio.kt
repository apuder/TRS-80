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

package org.puder.trs80.shared

/**
 * The machine's sound, in a page.
 *
 * Backwards from how a device does it. There the audio hardware calls the core
 * when it wants more; here the core cannot be called from the audio thread at
 * all -- a worklet runs in its own scope with no access to the emulator's
 * memory -- so the page pulls samples on the main thread, where the emulator
 * is, and posts them across. The worklet's own queue is what absorbs the
 * difference between the two clocks.
 *
 * Started with the machine and stopped with it. An AudioContext that nobody has
 * spoken to yet begins suspended, because a page may not make noise before the
 * user has touched it -- so the first pointer or key resumes it, which in this
 * app is the tap that started the machine or the first key typed at it.
 */
internal fun startAudio(core: JsAny) {
    js(
        """{
        if (window.trs80Audio) return;
        var context = new (window.AudioContext || window.webkitAudioContext)({ sampleRate: 44100 });
        var state = { context: context, node: null, timer: 0, buffer: 0 };
        window.trs80Audio = state;

        // A page may not make a sound until the user has done something. Both
        // are removed once they have fired: resuming a running context is
        // harmless, but a listener that outlives its reason is a leak.
        var wake = function () {
            context.resume();
            document.removeEventListener('pointerdown', wake);
            document.removeEventListener('keydown', wake);
        };
        document.addEventListener('pointerdown', wake);
        document.addEventListener('keydown', wake);

        context.audioWorklet.addModule('./trs80audio.js').then(function () {
            var node = new AudioWorkletNode(context, 'trs80-audio', { outputChannelCount: [1] });
            node.connect(context.destination);
            state.node = node;

            // Forty milliseconds of samples, forty times a second: the machine
            // is asked for a little more than is played, which is what keeps
            // the worklet's queue from running dry between wake-ups. The queue
            // has a ceiling, so the surplus is dropped rather than becoming
            // delay.
            var samples = 1800;
            state.buffer = core._malloc(samples * 2);
            state.timer = setInterval(function () {
                if (context.state !== 'running' || !state.node) return;
                var got = core._trs80_audio_pull(state.buffer, samples);
                if (!got) return;
                var pcm = new Int16Array(core.HEAPU8.buffer, state.buffer, got);
                var out = new Float32Array(got);
                var quiet = true;
                for (var i = 0; i < got; i++) {
                    out[i] = pcm[i] / 32768;
                    if (pcm[i] !== 0) quiet = false;
                }
                state.silent = quiet;
                // Transferred, not copied: the array is no use here afterwards.
                state.node.port.postMessage(out, [out.buffer]);
            }, 25);
        }).catch(function (e) {
            console.error('No sound: the audio worklet would not load. ' + e);
        });
    }"""
    )
}

/** Lets go of the audio when the machine does. */
internal fun stopAudio(core: JsAny) {
    js(
        """{
        var state = window.trs80Audio;
        if (!state) return;
        if (state.timer) clearInterval(state.timer);
        if (state.node) state.node.disconnect();
        if (state.buffer) core._free(state.buffer);
        state.context.close();
        window.trs80Audio = null;
    }"""
    )
}
