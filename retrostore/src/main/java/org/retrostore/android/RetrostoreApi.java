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

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import org.retrostore.ApiException;
import org.retrostore.RetrostoreClient;
import org.retrostore.RetrostoreClientImpl;
import org.retrostore.client.common.proto.App;
import org.retrostore.client.common.proto.MediaImage;

import java.util.List;

/**
 * Main API interface for an Android app to interface with the RetroStore Android components.
 */
public class RetrostoreApi {
    private static final RetrostoreApi SINGLETON = new RetrostoreApi();

    private final RetrostoreClient mRetrostoreClient;
    private AppInstallListener mInstallListener;

    public static RetrostoreApi get() {
        return SINGLETON;
    }

    private RetrostoreApi() {
        mRetrostoreClient = RetrostoreClientImpl.getDefault("n/a");
        RetrostoreActivity.setAppInstallListener(new AppInstallListener() {
            @Override
            public void onInstallApp(AppPackage appPackage) {
                RetrostoreApi.this.onInstallApp(appPackage);
            }
        });
    }

    public void registerAppInstallListener(AppInstallListener listener) {
        mInstallListener = Preconditions.checkNotNull(listener);
    }

    /**
     * Will download all aspects of a app package. Do not call this on the main thread.
     */
    public Optional<AppPackage> downloadApp(String appId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(appId));
        try {
            App app = mRetrostoreClient.getApp(appId);
            List<MediaImage> mediaImages = mRetrostoreClient.fetchMediaImages(appId);
            return Optional.of(new AppPackage(app, mediaImages));
        } catch (ApiException e) {
            e.printStackTrace();
        }
        return Optional.absent();
    }

    private void onInstallApp(AppPackage appPackage) {
        if (mInstallListener != null) {
            mInstallListener.onInstallApp(appPackage);
        }
    }
}
