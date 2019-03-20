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
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.android.kotlincoroutines.util.DefaultErrorDecisionStrategy
import com.example.android.kotlincoroutines.util.ErrorDecisionStrategy
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.TimeUnit

@RunWith(JUnit4::class)
class RefreshMainDataWorkTest {
    private lateinit var targetContext: Context
    private lateinit var configuration: Configuration
    private lateinit var workManager: WorkManager

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        configuration = Configuration.Builder()
                // Set log level to Log.DEBUG to make it easier to debug
                .setMinimumLoggingLevel(Log.DEBUG)
                // Use a SynchronousExecutor here to make it easier to write tests
                .setExecutor(SynchronousExecutor())
                .build()

        // Initialize WorkManager for instrumentation tests.
        WorkManagerTestInitHelper.initializeTestWorkManager(targetContext, configuration)
        workManager = WorkManager.getInstance()

        DefaultErrorDecisionStrategy.delegate =
                object: ErrorDecisionStrategy {
                            override fun shouldError() = false
                        }
    }

    @Test
    @Throws(Exception::class)
    fun testRefreshMainDataWork() {

        // Create request
        val request = OneTimeWorkRequestBuilder<RefreshMainDataWorkTestable>()
                .build()

        // Enqueue and wait for result. This also runs the Worker synchronously
        // because we are using a SynchronousExecutor.
        workManager.enqueue(request).result.get()
        // Get WorkInfo
        val workInfo = workManager.getWorkInfoById(request.id).get()

        // Assert
        assertThat(workInfo.state, `is`(WorkInfo.State.SUCCEEDED))
    }

    @Test
    @Throws(Exception::class)
    fun testWithConstraints() {
        val constraints = Constraints.Builder()
                .setRequiresCharging(true)
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build()

        // Create request
        val request = OneTimeWorkRequestBuilder<RefreshMainDataWorkTestable>()
                .setConstraints(constraints)
                .build()

        val workManager = WorkManager.getInstance()
        val testDriver = WorkManagerTestInitHelper.getTestDriver()
        // Enqueue and wait for result.
        workManager.enqueue(request).result.get()
        testDriver.setAllConstraintsMet(request.id)
        // Get WorkInfo
        val workInfo = workManager.getWorkInfoById(request.id).get()
        // Assert
        assertThat(workInfo.state, `is`(WorkInfo.State.SUCCEEDED))
    }

    @Test
    @Throws(Exception::class)
    fun testPeriodicWork() {
        // Create request
        val request = PeriodicWorkRequestBuilder<RefreshMainDataWorkTestable>(1, TimeUnit.DAYS)
                .build()

        val workManager = WorkManager.getInstance()
        val testDriver = WorkManagerTestInitHelper.getTestDriver()
        // Enqueue and wait for result.
        workManager.enqueue(request).result.get()
        // Tells the testing framework the period delay is met
        testDriver.setPeriodDelayMet(request.id)
        // Get WorkInfo
        val workInfo = workManager.getWorkInfoById(request.id).get()
        // Assert
        assertThat(workInfo.state, `is`(WorkInfo.State.ENQUEUED))
    }
}

class RefreshMainDataWorkTestable(context: Context, params: WorkerParameters) :
        RefreshMainDataWork(context, params) {

    override val coroutineContext = SynchronousExecutor().asCoroutineDispatcher()

    override suspend fun doWork(): Result = runBlocking {
        super.doWork()
    }
}
