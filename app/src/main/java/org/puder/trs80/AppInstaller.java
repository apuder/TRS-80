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

package org.puder.trs80;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.puder.trs80.async.UiExecutor;
import org.puder.trs80.configuration.Configuration;
import org.puder.trs80.configuration.ConfigurationManager;
import org.retrostore.android.AppPackage;
import org.retrostore.android.view.ImageLoader;
import org.retrostore.client.common.proto.App;
import org.retrostore.client.common.proto.MediaImage;
import org.retrostore.client.common.proto.Trs80Extension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Bundles functionality to install an app by creating the right configuration.
 */
public class AppInstaller {
    private static final String TAG = "AppInstaller";
    private final ConfigurationManager configManager;
    private final ImageLoader imageLoader;

    public AppInstaller(ConfigurationManager configManager, ImageLoader imageLoader) {
        this.configManager = configManager;
        this.imageLoader = imageLoader;
    }

    public boolean installApp(AppPackage appPackage) {
        List<ConfigurationManager.ConfigMedia> configMedia = new ArrayList<>();
        // The first four items will be the media images. The fifth is the cassette.
        for (int i = 0; i < 4; ++i) {
            MediaImage mediaImage = appPackage.mediaImages.get(i);
            if (mediaImage != null) {
                configMedia.add(new ConfigurationManager.ConfigMedia(mediaImage.getFilename(),
                        mediaImage.getData().toByteArray()));
            }
        }
        ConfigurationManager.ConfigMedia cassetteMedia = null;
        MediaImage cassetteImage = appPackage.mediaImages.get(4);
        if (cassetteImage != null) {
            cassetteMedia = new ConfigurationManager.ConfigMedia(
                    cassetteImage.getFilename(), cassetteImage.getData().toByteArray());
        }
        App app = appPackage.appData;
        Optional<Configuration> newConfiguration = configManager.addNewConfiguration(
                getHardwareModelId(app.getExtTrs80().getModel()), app.getName(),
                configMedia, cassetteMedia);
        if (!newConfiguration.isPresent()) {
            return false;
        }

        // So that it doesn't show up blank, set the screenshot to the logo of the just downloaded
        // app. It will later be replaces by actual screenshots.
        final int configId = newConfiguration.get().getId();
        String screenshotUrl = app.getScreenshotUrl(0);
        if (!Strings.isNullOrEmpty(screenshotUrl)) {
            ListenableFuture<Bitmap> screenshotBitmap =
                    imageLoader.loadAsBitmapAsync(screenshotUrl, 800, 600);
            Futures.addCallback(screenshotBitmap, new FutureCallback<Bitmap>() {
                @Override
                public void onSuccess(Bitmap result) {
                    try {
                        configManager.getEmulatorState(configId).saveScreenshot(result);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Cannot emulator state", e);
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    Log.e(TAG, "Cannot load initial screenshot.", t);
                }
            }, UiExecutor.create());
        }
        return true;
    }

    private static int getHardwareModelId(Trs80Extension.Trs80Model model) {
        switch (model) {
            case MODEL_I:
                return 1;
            case MODEL_III:
                return 3;
            case MODEL_4:
                return 4;
            case MODEL_4P:
                return 5;
            default:
            case UNKNOWN_MODEL:
            case UNRECOGNIZED:
                return 0;
        }
    }
}
