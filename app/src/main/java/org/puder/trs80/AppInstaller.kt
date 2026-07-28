/*
 * Copyright 2017, Sascha Haeberling
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

import org.puder.trs80.configuration.ConfigurationManager
import org.puder.trs80.configuration.ConfigurationManager.ConfigMedia
import org.retrostore.android.AppPackage
import org.retrostore.android.RetrostoreApi
import org.retrostore.client.common.proto.App
import org.retrostore.client.common.proto.MediaImage
import org.retrostore.client.common.proto.MediaType
import org.retrostore.client.common.proto.Trs80Model

/**
 * Bundles functionality to install an app by creating the right configuration.
 */
class AppInstaller(
    private val configManager: ConfigurationManager,
    private val retroStoreApi: RetrostoreApi
) {

    /**
     * Downloads the app with the given ID and creates a configuration for it. Do not call this on
     * the main thread.
     *
     * @return Whether the download and configuration creation was successful.
     */
    fun downloadAndInstallApp(appId: String): Boolean {
        require(appId.isNotEmpty()) { "appId must not be empty." }
        return retroStoreApi.downloadApp(appId).orNull()?.let(::installApp) ?: false
    }

    /**
     * Downloads the disk images of [app] and installs it by creating the configuration. Do not
     * call this on the main thread.
     *
     * @return Whether the download and configuration creation was successful.
     */
    fun downloadAndInstallApp(app: App): Boolean =
        retroStoreApi.downloadImages(app).orNull()?.let(::installApp) ?: false

    /**
     * Creates a configuration for the given app package.
     *
     * @return Whether the configuration was added successfully.
     */
    fun installApp(appPackage: AppPackage): Boolean {
        // The first four items will be the disk images. The fifth is the cassette.
        val disks = appPackage.mediaImages
            .filter { it.type == MediaType.DISK }
            .map(::toConfigMedia)
        val cassette = appPackage.mediaImages
            .firstOrNull { it.type == MediaType.CASSETTE }
            ?.let(::toConfigMedia)

        val app = appPackage.appData
        return configManager.addNewConfiguration(
            getHardwareModelId(app.extTrs80.model), app.name, disks, cassette
        ).isPresent
    }
}

private fun toConfigMedia(image: MediaImage): ConfigMedia =
    ConfigMedia(image.filename, image.data.toByteArray())

/** @return The `Hardware.MODEL*` constant matching the RetroStore [model]. */
private fun getHardwareModelId(model: Trs80Model): Int = when (model) {
    Trs80Model.MODEL_I -> Hardware.MODEL1
    Trs80Model.MODEL_III -> Hardware.MODEL3
    Trs80Model.MODEL_4 -> Hardware.MODEL4
    Trs80Model.MODEL_4P -> Hardware.MODEL4P
    else -> Hardware.MODEL_NONE
}
