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

package org.puder.trs80.async

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor

/**
 * Executor that runs tasks on the Android main-thread.
 */
class UiExecutor private constructor(private val handler: Handler) : Executor {

    override fun execute(runnable: Runnable) {
        handler.post(runnable)
    }

    companion object {
        /** Creates a new UiExecutor. */
        @JvmStatic
        fun create(): Executor = UiExecutor(Handler(Looper.getMainLooper()))
    }
}
