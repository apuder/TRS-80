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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.common.base.Optional;

import org.retrostore.android.net.DataFetcher;
import org.retrostore.android.view.ImageLoader;
import org.retrostore.client.common.proto.App;

import java.util.Locale;

/**
 * Shows details about an app, and lets the user install it.
 */
public class AppDetailsPageActivity extends AppCompatActivity {
    public static final String EXTRA_APP_ID = "app_id";
    private static final String TAG = "DetailsActivity";
    private static AppInstallListener mExternalListener;

    private DataFetcher mFetcher;
    private ImageLoader mImageLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        mImageLoader = ImageLoader.get(getApplicationContext());

        Optional<App> appOpt = getAppFromIntent();
        if (!appOpt.isPresent()) {
            Log.w(TAG, "Finishing activity.");
            finish();
            return;
        }
        fillViews(appOpt.get());
    }

    private void fillViews(final App app) {
        ((TextView) findViewById(R.id.appName)).setText(app.getName());
        ((TextView) findViewById(R.id.appDescription)).setText(app.getDescription());
        ((TextView) findViewById(R.id.appDescription)).setMovementMethod(new
                ScrollingMovementMethod());
        ((TextView) findViewById(R.id.appAuthor)).setText(app.getAuthor());
        ((TextView) findViewById(R.id.appVersion)).setText(getFormattedString(R.string
                .app_version, app.getVersion()));
        if (app.getScreenshotUrlCount() > 0) {
            mImageLoader.loadUrlIntoView(app.getScreenshotUrl(0),
                    (ImageView) findViewById(R.id.appThumbnail));
        }
        findViewById(R.id.installButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onAskForInstallation(app);
            }
        });
    }

    private String getFormattedString(int id, Object... args) {
        return getResources().getString(id, args);
    }

    private Optional<App> getAppFromIntent() {
        Intent intent = getIntent();
        if (intent == null) {
            Log.i(TAG, "No intent.");
            return Optional.absent();
        }
        Bundle extras = intent.getExtras();
        if (extras == null) {
            Log.i(TAG, "No extras.");
            return Optional.absent();
        }
        String appId = extras.getString(EXTRA_APP_ID, null);
        if (appId == null) {
            Log.w(TAG, "No 'appId' given.");
            return Optional.absent();
        }

        // TODO: Make this work so that we instantiate a new fetcher if it got clean-up.
        mFetcher = DataFetcher.get().orNull();
        if (mFetcher == null) {
            Log.w(TAG, "No data fetcher available.");
            return Optional.absent();
        }

        Optional<App> appOpt = mFetcher.getFromCache(appId);
        if (!appOpt.isPresent()) {
            Log.w(TAG, "App not in cache.");
        }
        return appOpt;
    }

    static void setAppInstallListener(AppInstallListener listener) {
        mExternalListener = listener;
    }

    private void onAskForInstallation(final App app) {
        DialogInterface.OnClickListener dialogClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                mExternalListener.onInstallApp(app);
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                // Nothing to do.
                                break;
                        }
                    }
                };

        String dialogText = String.format(Locale.getDefault(),
                getString(R.string.dialog_install), app.getName());
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(dialogText)
                .setPositiveButton(R.string.dialog_yes_install, dialogClickListener)
                .setNegativeButton(R.string.dialog_no, dialogClickListener)
                .show();
    }

    private void showMessage(final int messageId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(AppDetailsPageActivity.this);
                builder.setMessage(messageId);
                builder.show();
            }
        });
    }

}
