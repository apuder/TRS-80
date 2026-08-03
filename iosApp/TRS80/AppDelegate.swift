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

import UIKit
import Shared

/// The iOS app, which is a window and one view controller.
///
/// Everything on screen comes from the shared module — the same code the Android
/// app draws — so there is deliberately nothing here to change when a screen is
/// added. This is the counterpart of `Trs80Activity` on Android, and it is
/// smaller only because iOS asks for less.
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        // A disk image to seed the first machine with, if one is shipped. Nil is
        // a supported answer: the app then starts with a bare Model III and the
        // catalog, which is where the programs are anyway.
        let disk = Bundle.main.path(forResource: "disk_0", ofType: "dsk")

        let window = UIWindow(frame: UIScreen.main.bounds)
        window.rootViewController = IosAppKt.Trs80ViewController(diskPath: disk)
        window.makeKeyAndVisible()
        self.window = window
        return true
    }
}
