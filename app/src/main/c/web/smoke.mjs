/*
 * Boots the core, in WebAssembly, and reads what the machine puts on screen.
 *
 * The smallest end-to-end proof that the emulator survived the crossing: no
 * Kotlin, no Compose, no browser. Build the core first, then run this against a
 * Model III ROM -- it should print `Cass?`, which is what a Model III with no
 * disk says while it waits for a cassette.
 *
 *   emcmake cmake -S app/src/main/c -B build/webcore -DCMAKE_BUILD_TYPE=Release
 *   cmake --build build/webcore --parallel
 *   cd build/webcore && node smoke.mjs /path/to/model3.rom
 *
 * Two things it exists to remember. trs80_run() does nothing at all unless
 * trs80_set_running(1) was called first, which looks exactly like a machine
 * that will not boot. And the run loop returns control immediately rather than
 * blocking: its frame pause is an emscripten_sleep, so it yields to whatever
 * event loop is hosting it -- node's here, a browser's in the app.
 */
import fs from 'node:fs';
import Trs80Core from './trs80core.js';

const Module = await Trs80Core();
Module.FS.writeFile('/model3.rom', fs.readFileSync(process.argv[2]));

// struct trs80_config { int model; const char *rom; const char *cassette;
//                       const char *disk[4]; unsigned short entry; }
const romPath = Module._malloc(32);
{   // A C string, written by hand: stringToUTF8 is not among the exports.
    const path = '/model3.rom';
    const heap = new Uint8Array(Module.HEAPU8.buffer, romPath, 32);
    for (let i = 0; i < path.length; i++) heap[i] = path.charCodeAt(i);
    heap[path.length] = 0;
}
const config = Module._malloc(32);
new Uint8Array(Module.HEAPU8.buffer, config, 32).fill(0);
const words = new Int32Array(Module.HEAPU8.buffer, config, 8);
words[0] = 3;          // model
words[1] = romPath;    // rom_path

console.log('init:', Module._trs80_init(config));
Module._trs80_set_cell_size(8, 12);

// run() does not return; ASYNCIFY turns its frame pause into a yield, so it
// gives node's event loop the same chance it would give a browser's.
Module._trs80_set_running(1);   // run() returns at once without this
const started = Date.now();
const running = Module.ccall('trs80_run', null, [], [], { async: true });
console.log('run() returned control after', Date.now() - started, 'ms (0 means it yielded)');
await new Promise(resolve => setTimeout(resolve, 3000));
Module._trs80_set_running(0);
await running;

console.log('render says changed:', Module._trs80_render(),
            'pixels:', Module._trs80_pixel_width() + 'x' + Module._trs80_pixel_height());
const screen = Module._trs80_screen_buffer();
console.log('screen buffer at', screen, 'first 24 bytes:',
    Array.from(new Uint8Array(Module.HEAPU8.buffer, screen, 24)).join(','));
const bytes = new Uint8Array(Module.HEAPU8.buffer, screen, 64 * 16);
let text = '';
for (let row = 0; row < 16; row++) {
    let line = '';
    for (let col = 0; col < 64; col++) {
        const c = bytes[row * 64 + col];
        line += (c >= 0x20 && c <= 0x7e) ? String.fromCharCode(c) : ' ';
    }
    if (line.trim()) text += line.trimEnd() + '\n';
}
console.log('--- what the machine drew ---');
console.log(text || '(nothing)');
