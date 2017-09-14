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

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


/**
 * Loads images from a URL into an image view.
 */
public class ImageLoader {
    private static final String TAG = "ImageLoader";
    private final WeakReference<Context> mCtx;
    private final Executor mExecutor;

    public static ImageLoader get(Context ctx) {
        return new ImageLoader(ctx, Executors.newCachedThreadPool());
    }

    public ImageLoader(Context ctx, Executor executor) {
        mCtx = new WeakReference<>(ctx);
        mExecutor = executor;
    }

    public void loadUrlIntoView(String url, ImageView view) {
        Context context = mCtx.get();
        if (context != null) {
            Glide.with(context).load(url).centerCrop().into(view);
        }
    }

    public ListenableFuture<Bitmap> loadAsBitmapAsync(
            final String url, final int width, final int height) {
        final SettableFuture<Bitmap> future = SettableFuture.create();
        final Context context = mCtx.get();
        if (context == null) {
            future.setException(new RuntimeException("Context invalid."));
            return future;
        }

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    future.set(Glide.with(context).load(url).asBitmap().into(width, height).get());
                } catch (InterruptedException | ExecutionException e) {
                    Log.e(TAG, "Could not load image as bitmap.", e);
                    future.setException(e);
                }
            }
        });
        return future;
    }
}
