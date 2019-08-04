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

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import org.retrostore.android.R;
import org.retrostore.android.RetrostoreActivity.InternalAppInstallListener;
import org.retrostore.client.common.proto.App;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Adapter for main app list view.
 */
public class ViewAdapter extends RecyclerView.Adapter<ViewHolder> {
    private final ImageLoader mImageLoader;
    private final List<App> mAppList;
    private final InternalAppInstallListener mInstallListener;

    public ViewAdapter(ImageLoader imageLoader, List<App> appList,
                       InternalAppInstallListener installListener) {
        mImageLoader = checkNotNull(imageLoader);
        mAppList = checkNotNull(appList);
        mInstallListener = checkNotNull(installListener);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewGroup appEntry = (ViewGroup) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.app_item, parent, false);
        ViewHolder holder = new ViewHolder(mImageLoader, appEntry);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.setData(mAppList.get(position), mInstallListener);
    }

    @Override
    public int getItemCount() {
        return mAppList.size();
    }
}
