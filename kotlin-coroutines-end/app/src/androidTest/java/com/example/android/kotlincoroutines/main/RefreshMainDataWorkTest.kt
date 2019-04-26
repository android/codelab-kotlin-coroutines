/*
 * Copyright 2019 Google LLC
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
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.work.Constraints
import androidx.work.ListenableWorker.Result
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.android.kotlincoroutines.util.DefaultErrorDecisionStrategy
import com.example.android.kotlincoroutines.util.ErrorDecisionStrategy
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

@RunWith(JUnit4::class)
class RefreshMainDataWorkTest {
    private lateinit var context: Context
    private lateinit var workManager: WorkManager

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        workManager = WorkManager.getInstance(context)

        DefaultErrorDecisionStrategy.delegate =
                object: ErrorDecisionStrategy {
                            override fun shouldError() = false
                        }
    }

    @Test
    @Throws(Exception::class)
    fun testRefreshMainDataWork() {
        // Create request
        val request = OneTimeWorkRequestBuilder<RefreshMainDataWork>()
                .build()

        // Get the ListenableWorker
        val worker = TestListenableWorkerBuilder.from(context, request).build()

        // Start the work synchronously
        val result = worker.startWork().get()

        assertThat(result, `is` (Result.success()))
    }

    @Test
    @Throws(Exception::class)
    fun testWithConstraints() {
        val constraints = Constraints.Builder()
                .setRequiresCharging(true)
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build()

        // Create request
        val request = OneTimeWorkRequestBuilder<RefreshMainDataWork>()
                .setConstraints(constraints)
                .build()

        // Get the ListenableWorker
        val worker = TestListenableWorkerBuilder.from(context, request).build()

        // Start the work synchronously
        val result = worker.startWork().get()

        assertThat(result, `is` (Result.success()))
    }

    @Test
    @Throws(Exception::class)
    fun testPeriodicWork() {
        // Create request
        val request = PeriodicWorkRequestBuilder<RefreshMainDataWork>(1, TimeUnit.DAYS)
                .build()

        // Get the ListenableWorker
        val worker = TestListenableWorkerBuilder.from(context, request).build()

        // Start the work synchronously
        val result = worker.startWork().get()

        assertThat(result, `is` (Result.success()))
    }
}
