/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.kotlincoroutines.main

import android.content.Context
import android.support.annotation.WorkerThread
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * Worker job to refresh refresh titles from the network while the app is in the background.
 *
 * WorkManager is a library used to enqueue work that is guaranteed to execute after its constraints
 * are met. It can run work even when the app is in the background, or not running.
 */
class RefreshMainDataWork(context: Context, params: WorkerParameters) : Worker(context, params) {

    /**
     * Do our actual processing for the worker.
     *
     * WorkManager will call this method from a background thread. It may be called even
     * after our app has been terminated by the operating system, in which case [WorkManager] will
     * start just enough to run this [Worker].
     */
    override fun doWork(): Result {
        return refreshTitle()
    }

    /**
     * Refresh the title from the network using [TitleRepository]
     */
    // TODO: Implement refreshTitle using coroutines and runBlocking
    @WorkerThread
    private fun refreshTitle(): Result = Result.SUCCESS
}
