package com.example.android.kotlincoroutines.main

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.android.kotlincoroutines.main.fakes.MainNetworkFake
import com.example.android.kotlincoroutines.main.fakes.TitleDaoFake
import com.example.android.kotlincoroutines.main.fakes.makeFailureCall
import com.example.android.kotlincoroutines.main.fakes.makeSuccessCall
import com.example.android.kotlincoroutines.test.util.assertSendsEventWith
import com.example.android.kotlincoroutines.test.util.captureValues
import com.example.android.kotlincoroutines.test.util.getValueForTest
import com.example.android.kotlincoroutines.test.util.verify
import com.example.android.kotlincoroutines.util.FakeNetworkException
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.experimental.runBlocking
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
        val subject = MainViewModel(TitleRepository(
                MainNetworkFake(makeSuccessCall("OK")),
                TitleDaoFake("title")))

        assertThat(subject.title.getValueForTest()).isEqualTo("title")
    }

    @Test
    fun whenSuccessfulTitleLoad_itShowsAndHidesSpinner() {
        val subject = MainViewModel(TitleRepository(
                MainNetworkFake(makeSuccessCall("OK")),
                TitleDaoFake("title")))

        subject.spinner.captureValues {
            // TODO: Implement test for onMainViewClicked with coroutines here
        }.verify(true, false)
    }

    @Test
    fun whenError_itShowsErrorAndHidesSpinner() {
        val subject = MainViewModel(TitleRepository(
                MainNetworkFake(makeFailureCall(FakeNetworkException("An error"))),
                TitleDaoFake("title")))

        subject.spinner.captureValues {
            // TODO: Implement test for onMainViewClicked with coroutines here
        }.verify(true, false)

        subject.snackbar.assertSendsEventWith("An error")
    }

}
