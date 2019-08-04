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

package org.retrostore.android.view;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.retrostore.android.R;
import org.retrostore.android.RetrostoreActivity.InternalAppInstallListener;
import org.retrostore.client.common.proto.App;

/**
 * View holder for the items in the main apps list recycler view.
 */
class ViewHolder extends RecyclerView.ViewHolder {
    private final ImageLoader mImageLoader;
    private final ViewGroup mAppEntry;
    private final TextView mAppNameView;
    private final TextView mAppDescriptionView;
    private final TextView mAuthorView;
    private final TextView mVersionView;
    private final ImageView mThumbnailView;

    ViewHolder(ImageLoader imageLoader, ViewGroup v) {
        super(v);
        mImageLoader = imageLoader;
        mAppEntry = v;
        mAppNameView = (TextView) v.findViewById(R.id.appName);
        mAppDescriptionView = (TextView) v.findViewById(R.id.appDescription);
        mAuthorView = (TextView) v.findViewById(R.id.appAuthor);
        mVersionView = (TextView) v.findViewById(R.id.appVersion);
        mThumbnailView = (ImageView) v.findViewById(R.id.appThumbnail);
    }

    void setData(final App app, final InternalAppInstallListener installListener) {
        mAppNameView.setText(app.getName());
        mAppDescriptionView.setText(app.getDescription());
        mAuthorView.setText(app.getAuthor());
        mVersionView.setText(getFormattedString(R.string.app_version, app.getVersion()));
        if (app.getScreenshotUrlCount() > 0) {
            mImageLoader.loadUrlIntoView(app.getScreenshotUrl(0), mThumbnailView);
            // TODO: Add an icon to indicate that loading failed.
        } else {
            // TODO: Display something indicating that there are no screenshots.
        }
        this.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                installListener.onInstallApp(app);
            }
        });
    }

    private String getFormattedString(int id, Object... args) {
        return mAppEntry.getResources().getString(id, args);
    }
}
