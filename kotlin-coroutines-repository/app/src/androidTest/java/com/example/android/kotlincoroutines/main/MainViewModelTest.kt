package com.example.android.kotlincoroutines.main

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.android.kotlincoroutines.main.fakes.MainNetworkFake
import com.example.android.kotlincoroutines.main.fakes.TitleDaoFake
import com.example.android.kotlincoroutines.main.fakes.makeSuccessCall
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
import java.util.concurrent.TimeUnit.SECONDS

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
