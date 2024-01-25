/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.initializer

import android.content.Context
import androidx.startup.Initializer
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.wire.android.BuildConfig
import com.wire.android.util.extension.isGoogleServicesAvailable

class FirebaseInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        if (context.isGoogleServicesAvailable()) {
            val firebaseOptions = FirebaseOptions.Builder()
                .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                .setGcmSenderId(BuildConfig.FIREBASE_PUSH_SENDER_ID)
                .setApiKey(BuildConfig.GOOGLE_API_KEY)
                .setProjectId(BuildConfig.FCM_PROJECT_ID)
                .build()
            FirebaseApp.initializeApp(context, firebaseOptions)
        }
    }
    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList() // no dependencies on other libraries
}
