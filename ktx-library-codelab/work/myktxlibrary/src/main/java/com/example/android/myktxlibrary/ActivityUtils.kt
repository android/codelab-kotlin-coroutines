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

package com.example.android.myktxlibrary

import android.app.Activity
import android.location.Location
import android.widget.TextView
import androidx.annotation.IdRes

fun Activity.findAndSetText(@IdRes id: Int, text: String) {
    findViewById<TextView>(id).text = text
}

fun Activity.showLocation(@IdRes id: Int, location: Location?) {
    if (location != null) {
        findAndSetText(id, location.toString())
    } else {
        findAndSetText(id, "Location unknown")
    }
}

