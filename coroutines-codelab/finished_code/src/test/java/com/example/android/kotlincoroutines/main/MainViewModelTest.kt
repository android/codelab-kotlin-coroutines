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

package com.example.android.kotlincoroutines.main

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.android.kotlincoroutines.fakes.MainNetworkCompletableFake
import com.example.android.kotlincoroutines.fakes.MainNetworkFake
import com.example.android.kotlincoroutines.fakes.TitleDaoFake
import com.example.android.kotlincoroutines.main.utils.MainCoroutineRule
import com.example.android.kotlincoroutines.main.utils.captureValues
import com.example.android.kotlincoroutines.main.utils.getValueForTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class MainViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule(UnconfinedTestDispatcher())

    lateinit var subject: MainViewModel

    @Before
    fun setup() {
        subject = MainViewModel(
            TitleRepository(
                MainNetworkFake("OK"),
                TitleDaoFake("initial")
            ))
    }

    @Test
    fun whenMainClicked_updatesTaps() = runTest {
        subject.onMainViewClicked()
        assertThat(subject.taps.getValueForTest()).isEqualTo("0 taps")
        advanceTimeBy(1001)
        assertThat(subject.taps.getValueForTest()).isEqualTo("1 taps")
    }

    @Test
    fun loadsTitleByDefault() {
        assertThat(subject.title.getValueForTest()).isEqualTo("initial")
    }

    @Test
    fun whenSuccessfulTitleLoad_itShowsAndHidesSpinner() = runTest {
        val network = MainNetworkCompletableFake()

        subject = MainViewModel(
            TitleRepository(
                network,
                TitleDaoFake("title")
            )
        )

        subject.spinner.captureValues {
            subject.onMainViewClicked()
            assertThat(values).isEqualTo(listOf(false, true))
            network.sendCompletionToAllCurrentRequests("OK")
            assertThat(values).isEqualTo(listOf(false, true, false))
        }
    }

    @Test
    fun whenErrorTitleReload_itShowsErrorAndHidesSpinner() = runTest {
        val network = MainNetworkCompletableFake()
        subject = MainViewModel(
            TitleRepository(
                network,
                TitleDaoFake("title")
            )
        )

        subject.spinner.captureValues {
            assertThat(values).isEqualTo(listOf(false))
            subject.onMainViewClicked()
            assertThat(values).isEqualTo(listOf(false, true))
            network.sendErrorToCurrentRequests(makeErrorResult("An error"))
            assertThat(values).isEqualTo(listOf(false, true, false))
        }
    }

    @Test
    fun whenErrorTitleReload_itShowsErrorText() = runTest {
        val network = MainNetworkCompletableFake()
        subject = MainViewModel(
            TitleRepository(
                network,
                TitleDaoFake("title")
            )
        )

        subject.onMainViewClicked()
        network.sendErrorToCurrentRequests(makeErrorResult("An error"))
        assertThat(subject.snackbar.getValueForTest()).isEqualTo("Unable to refresh title")
        subject.onSnackbarShown()
        assertThat(subject.snackbar.getValueForTest()).isEqualTo(null)
    }

    @Test
    fun whenMainViewClicked_titleIsRefreshed() = runTest {
        val titleDao = TitleDaoFake("title")
        subject = MainViewModel(
            TitleRepository(
                MainNetworkFake("OK"),
                titleDao
            )
        )
        subject.onMainViewClicked()
        assertThat(titleDao.nextInsertedOrNull()).isEqualTo("OK")
    }

    private fun makeErrorResult(result: String): HttpException {
        return HttpException(Response.error<String>(
            500,
            ResponseBody.create(
                MediaType.get("application/json"),
                "\"$result\"")
        ))
    }
}