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

package org.puder.trs80.shared.store

import org.puder.trs80.shared.MODEL1
import org.puder.trs80.shared.MODEL3
import org.puder.trs80.shared.MODEL4
import org.puder.trs80.shared.MODEL4P
import org.puder.trs80.shared.MODEL_NONE
import org.puder.trs80.shared.configuration.Configuration
import org.puder.trs80.shared.configuration.ConfigurationManager
import org.puder.trs80.shared.configuration.ConfigurationManager.ConfigMedia
import org.retrostore.RetrostoreClient
import org.retrostore.client.common.proto.App
import org.retrostore.client.common.proto.MediaImage
import org.retrostore.client.common.proto.MediaType
import org.retrostore.client.common.proto.Trs80Model

/**
 * Installs an app from the store by making a configuration out of it.
 *
 * A port of the Android `AppInstaller`, talking to [RetrostoreClient] directly
 * rather than through the Android module's wrapper — which is all that class
 * was, once the client became shared.
 */
class AppInstaller(
    private val configurations: ConfigurationManager,
    private val store: RetrostoreClient = retroStore,
) {

    /**
     * Fetches [app]'s media and creates a configuration for it.
     *
     * @return the new configuration, or null if the store had nothing to install
     * or the configuration could not be written.
     */
    suspend fun install(app: App): Configuration? {
        val media = store.fetchMediaImages(app.id)
        if (media.isEmpty()) {
            return null
        }
        // The disks go in the drives, in the order the store lists them; a
        // cassette, if there is one, goes in the deck.
        val disks = media.filter { it.type == MediaType.DISK }.map(::toConfigMedia)
        val cassette = media.firstOrNull { it.type == MediaType.CASSETTE }?.let(::toConfigMedia)

        return configurations.addNewConfiguration(
            model = modelOf(app.ext_trs80?.model),
            configName = app.name,
            disks = disks,
            cassette = cassette,
            // What ties the machine back to the entry it came from, for as long
            // as it stays unedited. Renaming it does not break the link.
            storeId = app.id,
        )
    }
}

private fun toConfigMedia(image: MediaImage): ConfigMedia =
    ConfigMedia(image.filename, image.data_.toByteArray())

/** @return the `MODEL*` constant matching the store's [model]. */
/** The store's model, as this app numbers them. */
fun modelOf(model: Trs80Model?): Int = when (model) {
    Trs80Model.MODEL_I -> MODEL1
    Trs80Model.MODEL_III -> MODEL3
    Trs80Model.MODEL_4 -> MODEL4
    Trs80Model.MODEL_4P -> MODEL4P
    else -> MODEL_NONE
}
