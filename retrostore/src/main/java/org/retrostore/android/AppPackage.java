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

package org.retrostore.android;

import com.google.common.collect.ImmutableList;

import org.retrostore.client.common.proto.App;
import org.retrostore.client.common.proto.MediaImage;

import java.util.List;

/**
 * Contains app metadata and disk images.
 */
public class AppPackage {
    public final App appData;
    public final List<MediaImage> mediaImages;

    public AppPackage(App appData, List<MediaImage> mediaImages) {
        this.appData = appData;
        this.mediaImages = ImmutableList.copyOf(mediaImages);
    }
}
