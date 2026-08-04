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

import kotlin.js.Promise
import org.khronos.webgl.Int8Array
import org.khronos.webgl.toByteArray
import org.khronos.webgl.toInt8Array
import okio.Path.Companion.toPath
import org.puder.trs80.shared.io.appFileSystem
import org.puder.trs80.shared.ui.DiskImageSpec

private const val TAG = "BrowserCore"

/**
 * The emulator, which the page began fetching before the app started.
 *
 * Asynchronous, and nothing else here is: a page fetches its WebAssembly over
 * the network, so the app waits for this before it draws anything. That is the
 * one thing a browser's app does that a device's does not.
 *
 * The import is in index.html rather than here, and the reason is webpack: it
 * resolves every import it can read at build time, and Emscripten's module is
 * not its to resolve. Hiding the path from it -- a magic comment, a computed
 * specifier -- did not work, because it reads through both. Letting the page do
 * it is not a workaround so much as the honest arrangement: the emulator is
 * something the page provides, like the canvas.
 */
fun loadEmulator(): Promise<JsAny> = js("window.trs80Core")

/*
 * The boundary, which is JavaScript because it has to be.
 *
 * Kotlin/Wasm cannot call another WebAssembly module's exports directly -- each
 * module has its own memory and its own instance, and what they share is the
 * JavaScript that instantiated them both. So every call below hops through a
 * `js()` function taking the module as its first argument. It is a boundary
 * worth keeping thin and in one file rather than spread through the app.
 */

private fun jsInit(
    core: JsAny,
    model: Int,
    romPath: String,
    disk0: String,
    disk1: String,
    disk2: String,
    disk3: String,
    entry: Int,
): Int = js(
    """(function () {
        // struct trs80_config { int model; const char *rom; const char *cassette;
        //                       const char *disk[4]; unsigned short entry; }
        var text = function (s) {
            if (!s) return 0;
            var p = core._malloc(s.length + 1);
            var heap = core.HEAPU8;
            for (var i = 0; i < s.length; i++) heap[p + i] = s.charCodeAt(i);
            heap[p + s.length] = 0;
            return p;
        };
        var config = core._malloc(32);
        var words = new Int32Array(core.HEAPU8.buffer, config, 8);
        words.fill(0);
        words[0] = model;
        words[1] = text(romPath);
        words[2] = 0;
        words[3] = text(disk0);
        words[4] = text(disk1);
        words[5] = text(disk2);
        words[6] = text(disk3);
        new Uint16Array(core.HEAPU8.buffer, config + 28, 1)[0] = entry;
        return core._trs80_init(config);
    })()"""
)

/** Puts [bytes] where the C can fopen() it, making the directories on the way. */
private fun jsWriteFile(core: JsAny, path: String, bytes: Int8Array) {
    js(
        """{
        var slash = path.lastIndexOf('/');
        if (slash > 0) core.FS.mkdirTree(path.substring(0, slash));
        core.FS.writeFile(path, new Uint8Array(bytes.buffer, bytes.byteOffset, bytes.length));
    }"""
    )
}

private fun jsScreen(core: JsAny): Int8Array =
    js("new Int8Array(core.HEAPU8.buffer, core._trs80_screen_buffer(), 64 * 16)")

private fun jsPixels(core: JsAny): Int8Array = js(
    """new Int8Array(core.HEAPU8.buffer, core._trs80_pixel_buffer(),
        core._trs80_pixel_width() * core._trs80_pixel_height())"""
)

private fun jsPixelWidth(core: JsAny): Int = js("core._trs80_pixel_width()")
private fun jsPixelHeight(core: JsAny): Int = js("core._trs80_pixel_height()")
private fun jsSetCellSize(core: JsAny, width: Int, height: Int) { js("core._trs80_set_cell_size(width, height)") }
private fun jsRender(core: JsAny): Int = js("core._trs80_render()")
private fun jsInvalidateRender(core: JsAny) { js("core._trs80_invalidate_render()") }
private fun jsIsExpanded(core: JsAny): Int = js("core._trs80_is_expanded_mode()")
private fun jsSetRunning(core: JsAny, running: Int) { js("core._trs80_set_running(running)") }
private fun jsReset(core: JsAny) { js("core._trs80_reset()") }
private fun jsKeyEvent(core: JsAny, event: Int, sym: Int, key: Int) { js("core._trs80_add_key_event(event, sym, key)") }
private fun jsSetSoundMuted(core: JsAny, muted: Int) { js("core._trs80_set_sound_muted(muted)") }
private fun jsRewindCassette(core: JsAny) { js("core._trs80_rewind_cassette()") }
private fun jsCassettePosition(core: JsAny): Double = js("core._trs80_cassette_position()")

/**
 * Starts the machine, and hands the thread straight back.
 *
 * `trs80_run` does not return until the machine is stopped, and ASYNCIFY is what
 * makes that possible on the one thread a page has: the loop's own frame pause
 * became a yield, so calling it through ccall's async form starts it and returns
 * a promise that settles whenever the machine stops. Nothing awaits that
 * promise -- there is nothing to do with it -- but the catch matters, or a
 * machine that fails takes the page's console with it.
 */
private fun jsRun(core: JsAny) {
    js(
        """core.ccall('trs80_run', null, [], [], { async: true }).catch(function (e) {
        console.error('The machine stopped badly: ' + e);
    })"""
    )
}

private fun jsPaste(core: JsAny, text: String) {
    js(
        """{
        var bytes = core._malloc(text.length + 1);
        var heap = core.HEAPU8;
        for (var i = 0; i < text.length; i++) heap[bytes + i] = text.charCodeAt(i);
        heap[bytes + text.length] = 0;
        core._trs80_paste(bytes, text.length);
        core._free(bytes);
    }"""
    )
}

private fun jsStatePath(core: JsAny, path: String, save: Boolean) {
    js(
        """{
        var p = core._malloc(path.length + 1);
        var heap = core.HEAPU8;
        for (var i = 0; i < path.length; i++) heap[p + i] = path.charCodeAt(i);
        heap[p + path.length] = 0;
        var slash = path.lastIndexOf('/');
        if (save && slash > 0) core.FS.mkdirTree(path.substring(0, slash));
        if (save) core._trs80_save_state(p); else core._trs80_load_state(p);
        core._free(p);
    }"""
    )
}

/**
 * The machine, in a browser.
 *
 * The same C the phone runs, compiled to WebAssembly and reached through the
 * JavaScript above. What is different here is where the files are: the C opens
 * paths, and a page has no file system to open them from, so what the app has
 * written -- ROMs, disk images -- is copied into the emulator's own in-memory
 * one on the way into [boot]. It is a copy per boot of a few tens of kilobytes,
 * which is nothing beside fetching them in the first place.
 *
 * @param core the loaded module; see [loadEmulator].
 */
class BrowserCore(private val core: JsAny) : EmulatorCore {

    override val isExpandedMode: Boolean
        get() = jsIsExpanded(core) != 0

    /**
     * Read a byte at a time, because that is what the screen is read for.
     *
     * Only the tutorial and the tests look at video RAM, and they look at one
     * line of it. The picture takes the other path -- see [copyPixelsInto],
     * which crosses in one copy.
     */
    override val screenBuffer = object : ScreenBuffer {
        override fun get(index: Int): Byte = jsScreen(core).toByteArray()[index]
    }

    override val pixelBuffer = object : ScreenBuffer {
        override fun get(index: Int): Byte = jsPixels(core).toByteArray()[index]
    }

    override val pixelWidth: Int get() = jsPixelWidth(core)
    override val pixelHeight: Int get() = jsPixelHeight(core)

    override val romCellWidth = 8
    override val romCellHeight = 12

    override fun setCellSize(width: Int, height: Int) = jsSetCellSize(core, width, height)

    override fun render(): Boolean = jsRender(core) != 0

    override fun invalidateRender() = jsInvalidateRender(core)

    /**
     * One frame, in one copy.
     *
     * The view is made fresh each time rather than kept: the module's memory
     * grows, and when it does every view onto the old buffer is detached and
     * reads as zero -- a black screen, from a machine that is running perfectly.
     */
    override fun copyPixelsInto(destination: ByteArray) {
        val frame = jsPixels(core).toByteArray()
        frame.copyInto(destination, endIndex = minOf(frame.size, destination.size))
    }

    override fun boot(
        model: Int,
        romPath: String,
        diskPaths: List<String?>,
        cassettePath: String?,
        entryAddress: Int,
    ): Boolean {
        copyIn(romPath)
        val disks = List(4) { index -> diskPaths.getOrNull(index)?.also(::copyIn).orEmpty() }
        val result = jsInit(
            core,
            model,
            romPath,
            disks[0],
            disks[1],
            disks[2],
            disks[3],
            entryAddress,
        )
        if (result != 0) {
            Log.e(TAG, "The machine would not boot: trs80_init returned $result.")
        }
        return result == 0
    }

    /** Copies what the app has stored at [path] to the same path inside the module. */
    private fun copyIn(path: String) {
        val source = path.toPath()
        if (!appFileSystem.exists(source)) {
            Log.w(TAG, "Nothing stored at $path to give the machine.")
            return
        }
        val bytes = appFileSystem.read(source) { readByteArray() }
        jsWriteFile(core, path, bytes.toInt8Array())
    }

    /** Starts the machine; see [jsRun] for why this returns immediately. */
    override fun run() {
        jsSetRunning(core, 1)
        startAudio(core)
        jsRun(core)
    }

    override fun stop() {
        jsSetRunning(core, 0)
        stopAudio(core)
    }

    override fun reset() = jsReset(core)

    override fun saveState(path: String) = jsStatePath(core, path, save = true)

    override fun loadState(path: String) {
        copyIn(path)
        jsStatePath(core, path, save = false)
    }

    override fun setSoundMuted(muted: Boolean) = jsSetSoundMuted(core, if (muted) 1 else 0)

    override fun paste(text: String) = jsPaste(core, text)

    override fun rewindCassette() = jsRewindCassette(core)

    override fun cassettePosition(): Float = jsCassettePosition(core).toFloat()

    override fun keyDown(sym: Int, key: Int) = jsKeyEvent(core, KEY_DOWN, sym, key)

    override fun keyUp(sym: Int, key: Int) = jsKeyEvent(core, KEY_UP, sym, key)

    /**
     * Not yet: a blank disk is written to a path, and the path the app would
     * ask for is in its own file system rather than the module's. Making one
     * means writing it inside the module and copying it back out, and nothing
     * in the browser has asked for a disk yet.
     */
    override fun createBlankDisk(path: String, spec: DiskImageSpec): Boolean {
        Log.w(TAG, "Making a ${spec.format} disk is not implemented in the browser yet.")
        return false
    }
}

/** The key event kinds trs80_core.h defines, which are SDL 1.2's. */
private const val KEY_DOWN = 2
private const val KEY_UP = 3
