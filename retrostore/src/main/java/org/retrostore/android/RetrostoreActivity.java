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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import org.retrostore.RetrostoreClientImpl;
import org.retrostore.android.net.DataFetcher;
import org.retrostore.android.view.ImageLoader;
import org.retrostore.android.view.ViewAdapter;
import org.retrostore.client.common.proto.App;

import java.util.List;
import java.util.concurrent.Executors;

import static java.util.Locale.US;

public class RetrostoreActivity extends AppCompatActivity {
    private DataFetcher mFetcher;
    private ImageLoader mImageLoader;

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private SwipeRefreshLayout mRefreshLayout;
    private AppInstallListener mAppInstallListener;

    private static AppInstallListener mExternalListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_retrostore);
        mAppInstallListener = new AppInstallListener() {
            @Override
            public void onInstallApp(App app) {
                showDetailsPage(app.getId());
            }
        };
        mFetcher = DataFetcher.initialize(RetrostoreClientImpl.get(
                "n/a", "https://test-dot-trs-80.appspot.com/api/%s", false),
                Executors.newSingleThreadExecutor());
        mImageLoader = ImageLoader.get(this.getApplicationContext());
        mRecyclerView = (RecyclerView) findViewById(R.id.appList);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh_layout);
        mRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        refreshApps();
                    }
                });
        refreshApps();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_app_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            refreshApps();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Protected so only the RetroStore API can access this. Not to be used directly by clients.
     */
    static void setAppInstallListener(AppInstallListener listener) {
        mExternalListener = listener;
    }

    private void showDetailsPage(long appId) {
        AppDetailsPageActivity.setAppInstallListener(mExternalListener);
        Intent intent = new Intent(this, AppDetailsPageActivity.class);
        intent.putExtra(AppDetailsPageActivity.EXTRA_APP_ID, appId);
        startActivity(intent);
    }

    private void refreshApps() {
        setRefreshingStatus(true);
        // FIXME: I don't see variable names for the API.
        Futures.addCallback(mFetcher.getAppsAsync(), new FutureCallback<List<App>>() {
            @Override
            public void onSuccess(List<App> result) {
                onAppsReceived(result);
                setRefreshingStatus(false);
            }

            @Override
            public void onFailure(Throwable t) {
                showToast("Something went wrong during the request: " + t.getMessage());
                setRefreshingStatus(false);
            }
        });
    }

    /**
     * Apps list received. Display in list.
     */
    private void onAppsReceived(final List<App> apps) {
        showToast(String.format(US, "Got %d apps, yay.", apps.size()));
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter = new ViewAdapter(mImageLoader, apps, mAppInstallListener);
                mRecyclerView.setAdapter(mAdapter);
            }
        });
    }

    private void setRefreshingStatus(final boolean refreshing) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRefreshLayout.setRefreshing(refreshing);
            }
        });
    }

    /**
     * Show a toast with the given message on the main thread.
     */
    private void showToast(final String message) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(RetrostoreActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
