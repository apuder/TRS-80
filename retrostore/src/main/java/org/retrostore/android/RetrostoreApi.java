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

import com.google.common.base.Preconditions;

import org.retrostore.client.common.proto.App;

/**
 * Main API interface for an Android app to interface with the RetroStore Android components.
 */
public class RetrostoreApi {
    private static final RetrostoreApi SINGLETON = new RetrostoreApi();

    private AppInstallListener mInstallListener;

    public static RetrostoreApi get() {
        return SINGLETON;
    }

    private RetrostoreApi() {
        RetrostoreActivity.setAppInstallListener(new AppInstallListener() {
            @Override
            public void onInstallApp(App app) {
                RetrostoreApi.this.onInstallApp(app);
            }
        });
    }

    public void registerAppInstallListener(AppInstallListener listener) {
        mInstallListener = Preconditions.checkNotNull(listener);
    }

    private void onInstallApp(App app) {
        if (mInstallListener != null) {
            mInstallListener.onInstallApp(app);
        }
    }
}
