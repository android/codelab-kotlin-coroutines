/*
 * Copyright (C) 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.kotlincoroutines.main.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * MainCoroutineRule installs a TestDispatcher for Dispatchers.Main.
 *
 * When using MainCoroutineRule, you should use runTest to contain your tests:
 *
 * ```
 * @Test
 * fun usingRunTest() = runTest {
 *     aTestCoroutine()
 * }
 * ```
 *
 * @param dispatcher if provided, this [TestDispatcher] will be used.
 */
@ExperimentalCoroutinesApi
class MainCoroutineRule(val dispatcher: TestDispatcher = StandardTestDispatcher()) :
    TestWatcher() {

    override fun starting(description: Description?) {
        super.starting(description)
        // All injected dispatchers in a test should be TestDispatchers that share the same
        // TestScheduler (available as dispatcher.scheduler). All TestDispatchers created after
        // the Main dispatcher has been replaced will automatically share its scheduler.
        Dispatchers.setMain(dispatcher)

        // If you need to create and inject test dispatchers before this happens, create a
        // TestScheduler yourself, and pass it to all test dispatchers explicitly.
    }

    override fun finished(description: Description?) {
        super.finished(description)
        Dispatchers.resetMain()
    }
}
