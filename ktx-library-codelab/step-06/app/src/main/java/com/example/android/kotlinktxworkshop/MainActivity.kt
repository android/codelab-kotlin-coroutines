/*
 * Copyright 2019 Google LLC
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

package com.example.android.kotlinktxworkshop

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.android.myktxlibrary.awaitLastLocation
import com.example.android.myktxlibrary.findAndSetText
import com.example.android.myktxlibrary.hasPermission
import com.example.android.myktxlibrary.locationFlow
import com.example.android.myktxlibrary.showLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        lifecycleScope.launchWhenStarted { // only get results when the Activity is in the started state
            try {
                getLastKnownLocation()
                startUpdatingLocation()
            } catch (e: Exception) {
                findAndSetText(R.id.textView, "Unable to get location.")
                Log.d(TAG, "Unable to get location", e)
            }
        }
    }

    override fun onStart() {
        super.onStart()

        if (!hasPermission(ACCESS_FINE_LOCATION)) {
            requestPermissions(arrayOf(ACCESS_FINE_LOCATION), 0)
        }
    }

    private suspend fun getLastKnownLocation() {
        val lastLocation = fusedLocationClient.awaitLastLocation()
        showLocation(R.id.textView, lastLocation)
    }

    private suspend fun startUpdatingLocation() {
        fusedLocationClient.locationFlow()
            .conflate()
            // In a real app collecting a Flow from your data layer using a pausing Dispatcher
            // such as lifecycleScope.launchWhenStarted is not recommended,
            // so we switch to Dispatchers.Main here
            // Normally use something like Flow.asLiveData and Observe the LiveData from UI
            .flowOn(Dispatchers.Main)
            .collect { location ->
                showLocation(R.id.textView, location)
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            recreate()
        }
    }
}

const val TAG = "KTXCODELAB"
