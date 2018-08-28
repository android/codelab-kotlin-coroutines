package com.example.android.kotlincoroutines.main.fakes

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.example.android.kotlincoroutines.main.MainNetwork
import com.example.android.kotlincoroutines.main.Title
import com.example.android.kotlincoroutines.main.TitleDao
import com.example.android.kotlincoroutines.util.FakeNetworkCall
import com.example.android.kotlincoroutines.util.FakeNetworkException

/**
 * Fake [TitleDao] for use in tests
 *
 * @param titleToReturn an updatable value that will be returned by [loadTitle] via a [LiveData]
 */
class TitleDaoFake(var titleToReturn: String): TitleDao {
    val inserted = mutableListOf<Title>()

    override fun insertTitle(title: Title) {
        inserted += title
    }

    override fun loadTitle(): LiveData<Title> {
        return MutableLiveData<Title>().apply {
            value = Title(titleToReturn)
        }
    }
}

/**
 * Testing Fake implementation of MainNetwork
 *
 * @param call an updatable result for [fetchNewWelcome]
 */
class MainNetworkFake(var call: FakeNetworkCall<String> = makeSuccessCall("title")):
        MainNetwork {
    override fun fetchNewWelcome(): FakeNetworkCall<String> {
        return call
    }
}

/**
 * Make a fake successful network result
 *
 * @param title result to return
 */
fun makeSuccessCall(title: String) = FakeNetworkCall<String>().apply {
    onSuccess(title)
}

/**
 * Make a fake failed network call
 *
 * @param throwable error to wrap
 */
fun makeFailureCall(throwable: FakeNetworkException) = FakeNetworkCall<String>().apply {
    onError(throwable)
}
