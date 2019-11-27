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

package com.example.android.advancedcoroutines

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * The [ViewModel] for fetching a list of [Plant]s.
 */
class PlantListViewModel internal constructor(
    private val plantRepository: PlantRepository
) : ViewModel() {

    private var currentNetworkRequest: Job? = null

    private val growZone = MutableLiveData<GrowZone>(NoGrowZone)

    val plants: LiveData<List<Plant>> = growZone.switchMap { growZone ->
        if (growZone == NoGrowZone) {
            plantRepository.plants
        } else {
            plantRepository.getPlantsWithGrowZone(growZone)
        }
    }

    init {
        clearGrowZoneNumber()
    }

    fun setGrowZoneNumber(num: Int) {
        currentNetworkRequest?.cancel()
        currentNetworkRequest = viewModelScope.launch {
            plantRepository.fetchPlantsForGrowZone(GrowZone(num))
        }
        growZone.value = GrowZone(num)
    }

    fun clearGrowZoneNumber() {
        currentNetworkRequest?.cancel()
        currentNetworkRequest = viewModelScope.launch {
            plantRepository.fetchPlants()
        }
        growZone.value = NoGrowZone
    }

    fun isFiltered() = growZone.value != NoGrowZone
}
