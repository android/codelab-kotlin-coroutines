package com.example.android.kotlincoroutines.main

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Rule
import org.junit.Test

class TitleRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun whenRefreshTitleSuccess_insertsRows() {
        // TODO: Write this test
    }

    @Test(expected = TitleRefreshError::class)
    fun whenRefreshTitleTimeout_throws() {
        // TODO: Write this test
        throw TitleRefreshError("Remove this – made test pass in starter code", null)
    }
}