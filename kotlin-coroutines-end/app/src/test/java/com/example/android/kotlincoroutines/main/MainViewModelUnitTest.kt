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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.android.kotlincoroutines.CoroutinesMainDispatcherRule
import com.example.android.kotlincoroutines.LiveDataTestUtil
import com.example.android.kotlincoroutines.fakes.MainNetworkFake
import com.example.android.kotlincoroutines.fakes.TitleDaoFake
import com.example.android.kotlincoroutines.util.FakeNetworkCall
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class MainViewModelUnitTest {

    // Subject under test
    private lateinit var mainViewModel: MainViewModel

    // A CoroutineDispatcher that can be controlled from tests
    @ExperimentalCoroutinesApi
    private val testDispatcher = TestCoroutineDispatcher()

    // Set the main coroutines dispatcher for unit testing.
    // We are setting the above-defined testDispatcher as the Main thread dispatcher.
    // Code running on Main dispatcher will run on the test thread.
    @ExperimentalCoroutinesApi
    @get:Rule
    var coroutinesMainDispatcherRule = CoroutinesMainDispatcherRule(testDispatcher)

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun refreshTitle() {
        mainViewModel = MainViewModel(
                TitleRepository(
                        MainNetworkFake(FakeNetworkCall()),
                        TitleDaoFake("title")
                )
        )
        mainViewModel.refreshTitle()

        assertThat(LiveDataTestUtil.getValue(mainViewModel.title)).isEqualTo("title")
    }

    /**
     *  Test the refreshTitle with Mockito verifications.
     *
     *  IMPORTANT! The preferred way to test this functionality is with the test above:
     *  For testing refreshTitle, a good test checks that the element is present not that
     *  interactions with an object happen.
     *
     *  Since refreshTitle launches a coroutine on the Main dispatcher, we can use
     *  testDispatcher.runBlockingTest to execute that coroutine on the test thread and wait
     *  for it to finish.
     */
    @ExperimentalCoroutinesApi
    @Test
    fun refreshTitle_MockitoVersion() {
        testDispatcher.runBlockingTest {
            val mockRepository = mock(TitleRepository::class.java)
            mainViewModel = MainViewModel(mockRepository).also {
                it.refreshTitle()
            }

            verify(mockRepository).refreshTitle()
        }
    }
}