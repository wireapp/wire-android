/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wire.android.core.ui.event

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

/**
 * Used as a wrapper for data that is exposed via a LiveData that represents an event.
 */
class OneTimeEvent<out T>(private val content: T) {

    private var hasBeenHandled = false

    /**
     * Returns the content and prevents its use again.
     */
    fun getContentIfNotHandled(): T? =
        if (hasBeenHandled) null
        else {
            hasBeenHandled = true
            content
        }

    /**
     * Returns the content, even if it's already been handled.
     */
    fun peekContent(): T = content
}

/**
 * An [Observer] for [OneTimeEvent]s, simplifying the pattern of checking if the [OneTimeEvent]'s content has
 * already been handled.
 *
 * [onEventUnhandledContent] is *only* called if the [OneTimeEvent]'s contents has not been handled.
 */
private class EventObserver<T>(private val onEventUnhandledContent: (T) -> Unit) : Observer<OneTimeEvent<T>> {
    override fun onChanged(oneTimeEvent: OneTimeEvent<T>?) {
        oneTimeEvent?.getContentIfNotHandled()?.let {
            onEventUnhandledContent(it)
        }
    }
}

fun <T, L : LiveData<OneTimeEvent<T>>> L.onEvent(lifecycleOwner: LifecycleOwner, onChanged: (T) -> Unit) =
    observe(lifecycleOwner, EventObserver(onChanged))