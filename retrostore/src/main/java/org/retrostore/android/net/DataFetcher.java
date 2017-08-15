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

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Fetches data from the RetroStore.
 */
public class DataFetcher {
    private final RetrostoreClient mClient;
    private final Executor mRequestExecutor;

    public DataFetcher(RetrostoreClient client, Executor mExecutor) {
        mClient = client;
        mRequestExecutor = mExecutor;
    }

    /**
     * Turn the synchronous API requests into an asynchronous one and return a listenable future
     * with the result.
     */
    public ListenableFuture<List<App>> getAppsAsync() {
        final SettableFuture<List<App>> future = SettableFuture.create();
        mRequestExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    future.set(mClient.fetchApps(0, 10));
                } catch (ApiException e) {
                    future.setException(e);
                }
            }
        });
        return future;
    }
}
