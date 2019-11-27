package com.example.android.kotlincoroutines.main

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.android.kotlincoroutines.fakes.MainNetworkCompletableFake
import com.example.android.kotlincoroutines.fakes.MainNetworkFake
import com.example.android.kotlincoroutines.fakes.TitleDaoFake
import com.example.android.kotlincoroutines.main.utils.MainCoroutineRule
import com.example.android.kotlincoroutines.main.utils.captureValues
import com.example.android.kotlincoroutines.main.utils.getValueForTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runBlockingTest
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import retrofit2.HttpException
import retrofit2.Response

@RunWith(JUnit4::class)
class MainViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @Test
    fun testTaps() {
        val subject = MainViewModel(
                TitleRepository(
                        MainNetworkFake("OK"),
                        TitleDaoFake("title")
                )
        )

        assertThat(subject.taps.getValueForTest()).isEqualTo("0 taps")
        subject.onMainViewClicked()
        assertThat(subject.taps.getValueForTest()).isEqualTo("0 taps")

        mainCoroutineRule.advanceTimeBy(200)
        assertThat(subject.taps.getValueForTest()).isEqualTo("1 taps")
    }

    @Test
    fun loadsTitleByDefault() {
        val subject = MainViewModel(
                TitleRepository(
                    MainNetworkFake("OK"),
                    TitleDaoFake("title")
                )
        )

        assertThat(subject.title.getValueForTest()).isEqualTo("title")
    }

    @Test
    fun whenSuccessfulTitleLoad_itShowsAndHidesSpinner() {
        val network = MainNetworkCompletableFake()

        val subject = MainViewModel(
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
    fun whenErrorTitleReload_itShowsErrorAndHidesSpinner() {
        val network = MainNetworkCompletableFake()
        val subject = MainViewModel(
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
    fun whenErrorTitleReload_itShowsErrorText() {
        val network = MainNetworkCompletableFake()
        val subject = MainViewModel(
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
    fun whenMainViewClicked_titleIsRefreshed() {
        val titleDao = TitleDaoFake("title")
        val subject = MainViewModel(
                TitleRepository(
                        MainNetworkFake("OK"),
                        titleDao
                )
        )
        subject.onMainViewClicked()
        titleDao.assertNextInsert("OK")
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