package com.wire.android.util.extension

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

fun intervalFlow(periodMs: Long, initialDelayMs: Long = 0L, stopWhen: () -> Boolean = { false }) =
    flow {
        delay(initialDelayMs)
        while (!stopWhen()) {
            emit(Unit)
            delay(periodMs)
        }
    }
