package com.example.android.kotlincoroutines.main

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.android.kotlincoroutines.fakes.MainNetworkCompletableFake
import com.example.android.kotlincoroutines.fakes.MainNetworkFake
import com.example.android.kotlincoroutines.fakes.TitleDaoFake
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test

class TitleRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun whenRefreshTitleSuccess_insertsRows() = runBlockingTest {
        val titleDao = TitleDaoFake("title")
        val subject = TitleRepository(
                MainNetworkFake("OK"),
                titleDao
        )

        subject.refreshTitle()
        assertThat(titleDao.nextInsertedOrNull()).isEqualTo("OK")
    }

    @Test(expected = TitleRefreshError::class)
    fun whenRefreshTitleTimeout_throws() = runBlockingTest {
        val network = MainNetworkCompletableFake()
        val subject = TitleRepository(
                network,
                TitleDaoFake("title")
        )

        val refresh = async {
            subject.refreshTitle()
        }

        advanceTimeBy(5_000)

        refresh.await() // throw the expected exception
    }
}