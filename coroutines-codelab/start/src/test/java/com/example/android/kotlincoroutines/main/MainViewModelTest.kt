package com.example.android.kotlincoroutines.main

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.android.kotlincoroutines.fakes.MainNetworkFake
import com.example.android.kotlincoroutines.fakes.TitleDaoFake
import com.example.android.kotlincoroutines.main.utils.MainCoroutineRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MainViewModelTest {
    @get:Rule
    val mainCoroutineRule =  MainCoroutineRule()
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

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
    fun whenMainClicked_updatesTaps() {
        // TODO: Write this
    }
}