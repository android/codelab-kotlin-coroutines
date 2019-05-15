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

package com.example.android.kotlincoroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.concurrent.Executors

/**
 * Sets the main coroutines dispatcher for unit testing.
 *
 * Uses a TestCoroutineDispatcher if provided. Otherwise it uses a new single thread
 * executor.
 *
 * See https://medium.com/androiddevelopers/easy-coroutines-in-android-viewmodelscope-25bffb605471
 * and https://github.com/Kotlin/kotlinx.coroutines/tree/master/kotlinx-coroutines-test
 */
@ExperimentalCoroutinesApi
class CoroutinesMainDispatcherRule(
        private val testDispatcher: TestCoroutineDispatcher? = null
) : TestWatcher() {

    // In order to check if singleThreadExecutor got initialized (without creating the object)
    // we need to extract the delegate out. Check the `finished` method,
    // a (singleThreadExecutor != null) will create the object even if it's not created
    private val singleThreadExecutorDelegate = lazy { Executors.newSingleThreadExecutor() }
    private val singleThreadExecutor by singleThreadExecutorDelegate

    override fun starting(description: Description?) {
        super.starting(description)
        if (testDispatcher != null) {
            Dispatchers.setMain(testDispatcher)
        } else {
            Dispatchers.setMain(singleThreadExecutor.asCoroutineDispatcher())
        }
    }

    override fun finished(description: Description?) {
        super.finished(description)
        if (singleThreadExecutorDelegate.isInitialized()) {
            singleThreadExecutor.shutdownNow()
        }
        testDispatcher?.let {
            testDispatcher.cleanupTestCoroutines()
        }
        Dispatchers.resetMain()
    }
}
