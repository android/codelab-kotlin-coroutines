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
import com.example.android.kotlincoroutines.fakes.MainNetworkFake
import com.example.android.kotlincoroutines.fakes.TitleDaoFake
import com.example.android.kotlincoroutines.fakes.makeSuccessCall
import com.example.android.kotlincoroutines.test.util.captureValues
import com.example.android.kotlincoroutines.test.util.getValueForTest
import com.example.android.kotlincoroutines.util.FakeNetworkCall
import com.example.android.kotlincoroutines.util.FakeNetworkException
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MainViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun loadsTitleByDefault() {
        val subject = MainViewModel(
                TitleRepository(
                        MainNetworkFake(makeSuccessCall("OK")),
                        TitleDaoFake("title")
                )
        )

        assertThat(subject.title.getValueForTest()).isEqualTo("title")
    }

    @Test
    fun whenSuccessfulTitleLoad_itShowsAndHidesSpinner() {
        val call = FakeNetworkCall<String>()

        val subject = MainViewModel(
                TitleRepository(
                        MainNetworkFake(call),
                        TitleDaoFake("title")
                )
        )

        subject.spinner.captureValues {
            subject.onMainViewClicked()
            runBlocking {
                assertSendsValues(2_000, true)
                call.onSuccess("data")
                assertSendsValues(2_000, true, false)
            }
        }
    }

    @Test
    fun whenErrorTitleReload_itShowsErrorAndHidesSpinner() {
        val call = FakeNetworkCall<String>()
        val subject = MainViewModel(
                TitleRepository(
                        MainNetworkFake(call),
                        TitleDaoFake("title")
                )
        )

        subject.spinner.captureValues {
            val spinnerCaptor = this
            subject.onMainViewClicked()
            runBlocking {
                assertSendsValues(2_000, true)
                call.onError(FakeNetworkException("An error"))
                assertSendsValues(2_000, true, false)
            }
        }
    }

    @Test
    fun whenErrorTitleReload_itShowsErrorText() {
        val call = FakeNetworkCall<String>()
        val subject = MainViewModel(
                TitleRepository(
                        MainNetworkFake(call),
                        TitleDaoFake("title")
                )
        )

        subject.snackbar.captureValues {
            val spinnerCaptor = this
            subject.onMainViewClicked()
            runBlocking {
                call.onError(FakeNetworkException("An error"))
                assertSendsValues(2_000, "An error")
                subject.onSnackbarShown()
                assertSendsValues(2_000, "An error", null)
            }
        }
    }

    @Test
    fun whenMainViewClicked_titleIsRefreshed() {
        val titleDao = TitleDaoFake("title")
        val subject = MainViewModel(
                TitleRepository(
                        MainNetworkFake(makeSuccessCall("OK")),
                        titleDao
                )
        )
        subject.onMainViewClicked()
        titleDao.assertNextInsert("OK")
    }
}
