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

package org.retrostore.android.net;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import org.retrostore.ApiException;
import org.retrostore.RetrostoreClient;
import org.retrostore.client.common.proto.App;
import org.retrostore.client.common.proto.MediaImage;
import org.retrostore.client.common.proto.SystemState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * Fetches data from the RetroStore.
 */
public class DataFetcher {
    private final RetrostoreClient mClient;
    private final Executor mRequestExecutor;
    private final Map<String, App> mAppCache;

    private static DataFetcher sInstance;

    public static Optional<DataFetcher> get() {
        // Might be null if the app got cleaned up but the details activity was resumed.
        return Optional.ofNullable(sInstance);
    }

    public static DataFetcher initialize(RetrostoreClient client, Executor executor) {
        if (sInstance == null) {
            sInstance = new DataFetcher(client, executor);
        }
        return sInstance;
    }

    private DataFetcher(RetrostoreClient client, Executor executor) {
        mClient = client;
        mRequestExecutor = executor;
        mAppCache = new HashMap<>();
    }

    /**
     * Turn the synchronous API requests into an asynchronous one and return a listenable future
     * with the result.
     */
    public ListenableFuture<List<App>> getAppsAsync() {
        final SettableFuture<List<App>> future = SettableFuture.create();
        mRequestExecutor.execute(() -> {
            try {
                List<App> apps = mClient.fetchApps(0, 100);
                updateCache(apps);
                future.set(apps);
            } catch (ApiException e) {
                future.setException(e);
            }
        });
        return future;
    }

    /**
     * Asynchronously fetch and return all media images associated with the app with the given ID.
     */
    public ListenableFuture<List<MediaImage>> fetchMediaImages(final String appId) {
        final SettableFuture<List<MediaImage>> future = SettableFuture.create();
        mRequestExecutor.execute(() -> {
            try {
                future.set(mClient.fetchMediaImages(appId));
            } catch (ApiException e) {
                future.setException(e);
            }
        });
        return future;
    }

    public Optional<App> getFromCache(String id) {
        return Optional.ofNullable(mAppCache.get(id));
    }

    public ListenableFuture<SystemState> getSystemState(final long token) {
        final SettableFuture<SystemState> future = SettableFuture.create();
        mRequestExecutor.execute(() -> {
            try {
                future.set(mClient.downloadState(token));
            } catch (ApiException e) {
                future.setException(e);
            }
        });
        return future;
    }

    private void updateCache(List<App> apps) {
        for (App app : apps) {
            mAppCache.put(app.getId(), app);
        }
    }
}
