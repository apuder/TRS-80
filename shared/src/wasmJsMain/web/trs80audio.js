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
 * What the machine sounds like, on the audio thread.
 *
 * A worklet runs where nothing else does -- not the emulator, not Compose --
 * and it is asked for exactly 128 samples at a time, on time, forever. So it
 * holds no opinions: the page pushes chunks of samples in, this hands them out
 * in order, and when there are none it plays silence rather than repeating the
 * last thing it had, which is what a stall should sound like.
 *
 * The queue is capped. If the page ever produces faster than the hardware
 * consumes -- a tab in the background, a machine running ahead -- unbounded
 * buffering would turn into unbounded delay, and sound arriving a second late
 * is worse than sound that skipped.
 */
const MAX_CHUNKS = 24;

class Trs80Audio extends AudioWorkletProcessor {

    constructor() {
        super();
        this.queue = [];
        this.offset = 0;
        this.port.onmessage = (event) => {
            this.queue.push(event.data);
            while (this.queue.length > MAX_CHUNKS) {
                this.queue.shift();
                this.offset = 0;
            }
        };
    }

    process(inputs, outputs) {
        const channel = outputs[0][0];
        for (let i = 0; i < channel.length; i++) {
            const chunk = this.queue[0];
            if (chunk === undefined) {
                channel[i] = 0;
                continue;
            }
            channel[i] = chunk[this.offset++];
            if (this.offset >= chunk.length) {
                this.queue.shift();
                this.offset = 0;
            }
        }
        // Never done: the machine may make a sound at any time, and a processor
        // that returns false is taken away and not asked again.
        return true;
    }
}

registerProcessor('trs80-audio', Trs80Audio);
