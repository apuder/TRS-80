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

import android.util.Log;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import org.retrostore.ApiException;
import org.retrostore.RetrostoreClient;
import org.retrostore.RetrostoreClientImpl;
import org.retrostore.client.common.proto.App;
import org.retrostore.client.common.proto.MediaImage;
import org.retrostore.client.common.proto.SystemState;

import java.util.List;

/**
 * Main API interface for an Android app to interface with the RetroStore Android components.
 */
public class RetrostoreApi {
    private static final String TAG = "RetrostoreApi";
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
            public void onInstallApp(App app) {
                RetrostoreApi.this.onInstallApp(app);
            }

            @Override
            public void onInstallSystemState(SystemState state) {
                RetrostoreApi.this.onInstallSystemState(state);
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
            return downloadImages(mRetrostoreClient.getApp(appId));
        } catch (ApiException e) {
            Log.e(TAG, "Cannot download media images.", e);
        }
        return Optional.absent();
    }

    /** Downloads the media images for the given app and returns the whole package. */
    public Optional<AppPackage> downloadImages(App app) {
        if (app == null) {
            return Optional.absent();
        }
        try {
            List<MediaImage> mediaImages = mRetrostoreClient.fetchMediaImages(app.getId());
            return Optional.of(new AppPackage(app, mediaImages));

        } catch (ApiException e) {
            Log.e(TAG, "Cannot download media images.", e);
        }
        return Optional.absent();
    }

    /** Uploads a system state ephemerally to the RetroStore. */
    public Optional<Long> uploadSystemState(SystemState state) {
        try {
            return Optional.of(mRetrostoreClient.uploadState(state));
        } catch (ApiException e) {
            Log.e(TAG, "Cannot upload system state.", e);
            return Optional.absent();
        }
    }

    /** Downloads an ephemeral system state from the RetroStore. */
    public Optional<SystemState> downloadSystemState(long token) {
        try {
            return Optional.of(mRetrostoreClient.downloadState(token));
        } catch (ApiException e) {
            Log.e(TAG, "Cannot download system state.", e);
            return Optional.absent();
        }
    }

    private void onInstallApp(App app) {
        if (mInstallListener != null) {
            mInstallListener.onInstallApp(app);
        }
    }

    private void onInstallSystemState(SystemState state) {
        if (mInstallListener != null) {
            mInstallListener.onInstallSystemState(state);
        }
    }
}
