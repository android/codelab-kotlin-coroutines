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

import com.example.android.kotlincoroutines.util.FAKE_RESULTS
import com.example.android.kotlincoroutines.util.FakeNetworkCall
import com.example.android.kotlincoroutines.util.fakeNetworkLibrary

/**
 * Main network interface which will fetch a new welcome title for us
 */
interface MainNetwork {
    fun fetchNewWelcome(): FakeNetworkCall<String>
}

/**
 * Default implementation of MainNetwork.
 */
object MainNetworkImpl : MainNetwork {
    override fun fetchNewWelcome() = fakeNetworkLibrary(FAKE_RESULTS)
}
