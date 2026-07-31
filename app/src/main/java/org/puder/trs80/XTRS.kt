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

package org.puder.trs80

import android.util.Log
import org.puder.trs80.shared.configuration.Configuration
import org.puder.trs80.shared.configuration.EmulatorState
import org.puder.trs80.shared.ScreenBuffer
import java.nio.ByteBuffer

private const val TAG = "XTRS"

/**
 * Gateway to the native layer. The `external` functions declared here are implemented in
 * `src/main/c/native.c`. XTRS also handles upcalls, i.e. the cases where the emulator needs
 * to call back into the Java layer; see [notImplemented].
 *
 * ## The binding is by name, and the compiler cannot check it
 *
 * The native library exports one `Java_org_puder_trs80_XTRS_<name>` symbol per `external`
 * function below, and resolves the upcall by looking up a *static* method literally named
 * `notImplemented`. Consequently:
 *
 *  * the class has to stay `org.puder.trs80.XTRS`,
 *  * every function has to keep its name, and
 *  * every function has to remain **static** as seen from the JVM.
 *
 * `@JvmStatic` on a member of an `object` is what buys the last point: it emits a single
 * `public static native` method on `XTRS` (no instance method, no bridge), so the native
 * side keeps receiving a `jclass` as its second argument. `initNative` depends on that
 * directly &mdash; it uses the `jclass` for `GetStaticMethodID(..., "notImplemented", ...)`.
 * Dropping a `@JvmStatic`, renaming anything, or moving the declarations out of this object
 * still compiles, and then fails at runtime with an `UnsatisfiedLinkError` or worse.
 */
object XTRS {

    init {
        Log.d(TAG, "Loading native library ...")
        System.loadLibrary("xtrs")
        Log.d(TAG, "Native library successfully loaded.")
    }

    /**
     * The activity that receives the emulator's upcalls, or `null` while no emulator is
     * on screen.
     */
    @JvmStatic
    var emulatorActivity: EmulatorActivity? = null

    /**
     * Boots the native emulator for the given configuration.
     *
     * @return 0 on success, a negative error code otherwise.
     */
    @JvmStatic
    fun init(configuration: Configuration, emulatorState: EmulatorState): Int {
        val model = configuration.model
        val romFile = when (model) {
            Hardware.MODEL1 -> SettingsActivity.getSetting(SettingsActivity.CONF_ROM_MODEL1)
            Hardware.MODEL3 -> SettingsActivity.getSetting(SettingsActivity.CONF_ROM_MODEL3)
            Hardware.MODEL4 -> SettingsActivity.getSetting(SettingsActivity.CONF_ROM_MODEL4)
            Hardware.MODEL4P -> SettingsActivity.getSetting(SettingsActivity.CONF_ROM_MODEL4P)
            // TODO return -1?
            else -> null
        }

        return initNative(
            model,
            romFile,
            0, // entryAddr; a .cmd image supplies its own.
            configuration.cassettePath ?: emulatorState.defaultCassettePath,
            configuration.getDiskPath(0),
            configuration.getDiskPath(1),
            configuration.getDiskPath(2),
            configuration.getDiskPath(3)
        )
    }

    /**
     * The character buffer shared with the native emulator, one byte per screen cell. The
     * buffer is owned by the native side and written to as the emulated machine updates
     * its video RAM.
     */
    @JvmStatic
    val screenBuffer: ScreenBuffer
        get() = DirectScreenBuffer(getScreenBufferNative())

    /**
     * Upcall from the emulator, reporting that it hit an unsupported code path.
     *
     * Resolved by name from `native.c`, so both the name and the static-ness are part of
     * the ABI.
     */
    @JvmStatic
    fun notImplemented(msg: String) {
        // A null activity means the emulator was torn down while the CPU thread was still
        // running. Dropping the message beats throwing back into native code, which would
        // leave a pending JNI exception that native.c does not check for.
        emulatorActivity?.notImplemented(msg)
    }

    @JvmStatic
    external fun setRunning(run: Boolean)

    @JvmStatic
    private external fun initNative(
        model: Int, romFile: String?, entryAddr: Int, cassette: String?,
        disk0: String?, disk1: String?, disk2: String?, disk3: String?
    ): Int

    @JvmStatic
    private external fun getScreenBufferNative(): ByteBuffer

    /**
     * Sets the size one character cell is drawn at, so the core rasterizes
     * straight to it and the host never scales the result. See `trs80_render()`.
     */
    @JvmStatic
    external fun setCellSize(width: Int, height: Int)

    /** The rasterized screen's size, which follows [setCellSize]. */
    @JvmStatic
    external fun pixelWidth(): Int

    @JvmStatic
    external fun pixelHeight(): Int

    /**
     * The rasterized screen: one coverage byte per pixel, which the host tints
     * and scales. See `trs80_render()`.
     */
    @JvmStatic
    val pixelBuffer: ByteBuffer
        get() = getPixelBufferNative()

    @JvmStatic
    private external fun getPixelBufferNative(): ByteBuffer

    /**
     * Rasterizes video RAM into [pixelBuffer], redrawing only what changed.
     *
     * Must be called from the thread that reads [pixelBuffer], and never from the
     * one running [run]: the core snapshots video RAM first so that a write
     * landing mid-rasterize cannot produce a half-drawn character, and doing this
     * work on the CPU thread would both lose that property and steal time from
     * the emulated machine.
     *
     * @return whether anything changed. When it has not, there is nothing to
     * upload and the frame can be skipped entirely.
     */
    @JvmStatic
    external fun render(): Boolean

    /** Makes the next [render] redraw the whole screen. */
    @JvmStatic
    external fun invalidateRender()

    @JvmStatic
    external fun saveState(fileName: String)

    @JvmStatic
    external fun loadState(fileName: String)

    @JvmStatic
    external fun isExpandedMode(): Boolean

    @JvmStatic
    external fun reset()

    @JvmStatic
    external fun rewindCassette()

    @JvmStatic
    external fun addKeyEvent(event: Int, mod: Int, key: Int)

    @JvmStatic
    external fun paste(clipboard: String)

    @JvmStatic
    external fun setSoundMuted(isMuted: Boolean)

    @JvmStatic
    external fun run()

    @JvmStatic
    external fun getCassettePosition(): Float

    @JvmStatic
    external fun createBlankJV1(filename: String): Boolean

    @JvmStatic
    external fun createBlankJV3(filename: String): Boolean

    @JvmStatic
    external fun createBlankDMK(
        filename: String, sides: Int, density: Int, eight: Int, ignden: Int
    ): Boolean
}

/**
 * Reads the emulator's screen memory straight out of the direct buffer JNI handed back, which
 * addresses the native buffer itself rather than a copy of it.
 */
private class DirectScreenBuffer(private val buffer: ByteBuffer) : ScreenBuffer {
    override fun get(index: Int): Byte = buffer.get(index)
}
