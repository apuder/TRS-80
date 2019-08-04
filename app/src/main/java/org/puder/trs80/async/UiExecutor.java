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

package org.puder.trs80.async;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;

/**
 * Executor that runs tasks on the Android main-thread.
 */
public class UiExecutor implements Executor {
    private final Handler handler;

    /** Creates a new UiExecutor. */
    public static Executor create() {
        return new UiExecutor(new Handler(Looper.getMainLooper()));
    }

    private UiExecutor(Handler handler) {
        this.handler = handler;
    }


    @Override
    public void execute(@NonNull Runnable runnable) {
        handler.post(runnable);
    }
}
