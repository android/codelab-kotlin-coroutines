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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class MainViewModelTest {

    /**
     * In this test, LiveData will immediately post values without switching threads.
     */
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    /**
     * This dispatcher will let us progress time in the test.
     */
    var testDispatcher = TestCoroutineDispatcher()

    lateinit var subject: MainViewModel

    /**
     * Before the test runs initialize subject
     */
    @Before
    fun setup() {
        // set Dispatchers.Main, this allows our test to run
        // off-device
        Dispatchers.setMain(testDispatcher)
        subject = MainViewModel()
    }

    @After
    fun teardown() {
        // reset main after the test is done
        Dispatchers.resetMain()
        // call this to ensure TestCoroutineDispatcher doesn't
        // accidentally carry state to the next test
        testDispatcher.cleanupTestCoroutines()
    }

    // note the use of runBlockingTest instead of runBlocking
    // this gives the test the ability to control time.
    @Test
    fun whenMainViewModelClicked_showSnackbar() = testDispatcher.runBlockingTest {
        subject.snackbar.observeForTesting {
            subject.onMainViewClicked()
            // progress time by one second
            advanceTimeBy(1_000)
            // value is available immediately without making the test wait
            Truth.assertThat(subject.snackbar.value)
                    .isEqualTo("Hello, from coroutines!")
        }
    }

    // helper method to allow us to get the value from a LiveData
    // LiveData won't publish a result until there is at least one observer
    private fun <T> LiveData<T>.observeForTesting(
            block: () -> Unit) {
        val observer = Observer<T> { Unit }
        try {
            observeForever(observer)
            block()
        } finally {
            removeObserver(observer)
        }
    }
}

