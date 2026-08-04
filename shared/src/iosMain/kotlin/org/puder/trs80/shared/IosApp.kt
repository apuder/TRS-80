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

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import org.puder.trs80.shared.configuration.ConfigurationManager
import org.puder.trs80.shared.io.FileManager
import org.puder.trs80.shared.io.TRS80_DIRECTORY
import org.puder.trs80.shared.io.appDataDirectory
import org.puder.trs80.shared.io.appFileSystem
import org.puder.trs80.shared.localstore.RomManager
import org.puder.trs80.shared.storage.appSettings
import platform.UIKit.UIViewController

private const val TAG = "IosApp"

/**
 * How much smaller a point is drawn than iOS would draw it, so that the two
 * apps come out the same size.
 *
 * They were never drawn differently: the wordmark measures 178x38 pixels on a
 * Pixel 9 Pro and 176x37 on an iPhone 16 Pro, which is the same drawing at the
 * same 3x. What differs is how much room each platform says those phones have.
 * They are both 6.3 inches, and Android calls it 427dp across while iOS calls
 * it 402pt — so the identical drawing covers 6% more of the iPhone, and reads
 * as larger type and fewer rows on screen.
 *
 * A constant rather than a target width: scaling every iPhone to some reference
 * number of points would leave a small one drawing everything at three quarters
 * size. This is a deliberate step away from what other iOS apps do, taken
 * because one app on two platforms is the point.
 */
private const val ANDROID_DP = 402f / 426.67f

/**
 * The iOS host: storage, a Compose view controller, and the hardware keyboard.
 *
 * Everything on screen is [Trs80AppUi], which is shared. What is left here is
 * the three things only this platform can answer — where the app's data lives,
 * what draws a Compose tree, and how a real keyboard reaches the machine.
 *
 * @param diskPath a disk image in the app bundle, seeded into the app's own
 * storage on first run. The ROMs are no longer among them: the app downloads
 * those itself, as the Android app always has.
 */
@OptIn(ExperimentalForeignApi::class)
fun Trs80ViewController(diskPath: String?): UIViewController {
    installIfNeeded(diskPath)

    val capture = KeyCapture()
    val compose = ComposeUIViewController {
        val platform = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(
                density = platform.density * ANDROID_DP,
                fontScale = platform.fontScale,
            ),
        ) {
            Trs80AppUi(core = IosEmulatorCore, hardwareKeys = capture)
        }
    }

    // A real keyboard has to be taken in UIKit, not in Compose; see
    // KeyForwardingController.
    return KeyForwardingController(
        content = compose,
        capture = capture,
        onKeyDown = { IosEmulatorCore.keyDown(it.sym, it.key) },
        onKeyUp = { IosEmulatorCore.keyUp(it.sym, it.key) },
    )
}

/**
 * Opens the app's storage, and seeds a bundled disk into it on first run.
 *
 * Only if one ships. No disk ships today, and a first run then leaves the
 * library empty, which is the right answer and the one Android has always
 * given: the machines a person wants are in the catalog below, a tap away, and
 * the empty section says so. It used to make a machine regardless and call it
 * "Bundled sample" -- a Model III with no disk in it, named after the mechanism
 * that made it, sitting at the top of a new install as though the user had put
 * it there.
 *
 * Idempotent: on later runs the store already has the configuration and the
 * files are already there, so this finds them rather than copying again.
 */
private fun installIfNeeded(diskPath: String?) {
    val settings = appSettings()
    val creator = FileManager.Creator(appDataDirectory() / TRS80_DIRECTORY)
    val manager = ConfigurationManager.init(creator, settings)
    RomManager.init(creator, settings)

    if (manager.configCount > 0) {
        Log.i(TAG, "${manager.configCount} configuration(s) already installed.")
        return
    }
    val disk = diskPath?.toPath() ?: return

    // Named for the disk, because that is what it is. A machine in the library
    // is read as something the user made, and its name is how they will find it
    // again.
    val disks = listOf(
        ConfigurationManager.ConfigMedia(
            filename = disk.name,
            data = appFileSystem.read(disk) { readByteArray() },
        )
    )
    manager.addNewConfiguration(MODEL3, disk.name.substringBeforeLast('.'), disks, cassette = null)
        ?: Log.e(TAG, "Could not install the bundled disk ${disk.name}.")
}
