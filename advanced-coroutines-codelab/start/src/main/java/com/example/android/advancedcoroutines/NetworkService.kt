package com.example.android.advancedcoroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

class NetworkService {

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://raw.githubusercontent.com/")
        .client(OkHttpClient())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val retrofitGist = Retrofit.Builder()
        .baseUrl("https://gist.githubusercontent.com/")
        .client(OkHttpClient())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val sunflowerService = retrofit.create(SunflowerService::class.java)
    private val sunflowerServiceGist = retrofitGist.create(SunflowerService::class.java)

    suspend fun allPlants(): List<Plant> = withContext(Dispatchers.Default) {
        val result = sunflowerService.getAllPlants()
        result.shuffled()
    }

    suspend fun plantsByGrowZone(growZone: GrowZone) = withContext(Dispatchers.Default) {
        val result = sunflowerService.getAllPlants()
        result.filter { it.growZoneNumber == growZone.number }.shuffled()
    }

    suspend fun customPlantSortOrder(): List<String> = withContext(Dispatchers.Default) {
        val result = sunflowerServiceGist.getCustomPlantSortOrder()
        result.map { plant -> plant.plantId }
    }
}

interface SunflowerService {
    @GET("android/sunflower/master/app/src/main/assets/plants.json")
    suspend fun getAllPlants() : List<Plant>

    @GET("tiembo/0829013ac578e60c87d7c7f2fb89d6a5/raw/fc98374a3d1a5c6f5df40e9fb3e1a03e6f540d86/custom_plant_sort_order.json")
    suspend fun getCustomPlantSortOrder() : List<Plant>
}