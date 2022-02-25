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
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TitleRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun whenRefreshTitleSuccess_insertsRows() = runTest {
        val titleDao = TitleDaoFake("title")
        val subject = TitleRepository(
            MainNetworkFake("OK"),
            titleDao
        )

        subject.refreshTitle()
        assertThat(titleDao.nextInsertedOrNull()).isEqualTo("OK")
    }

    @Test(expected = TitleRefreshError::class)
    fun whenRefreshTitleTimeout_throws() = runTest {
        val network = MainNetworkCompletableFake()
        val subject = TitleRepository(
            network,
            TitleDaoFake("title")
        )

        launch {
            subject.refreshTitle()
        }

        advanceTimeBy(5_000)
    }
}