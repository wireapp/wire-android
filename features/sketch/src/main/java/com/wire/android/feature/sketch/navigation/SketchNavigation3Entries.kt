/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.feature.sketch.navigation

import androidx.core.net.toUri
import com.wire.android.di.metro.wireMetroViewModel
import com.wire.android.feature.sketch.DrawingCanvasRouteScreen
import com.wire.android.feature.sketch.DrawingCanvasViewModel
import com.wire.android.navigation.navigation3.WireEntryPresentation
import com.wire.android.navigation.navigation3.WireEntryProviderInstaller
import com.wire.android.navigation.navigation3.WireNavigation3ResultType
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.navigation3.wireEntry
import com.wire.navigation.WireNavResult

val DrawingCanvasNavigation3ResultType: WireNavigation3ResultType<DrawingCanvasResult> =
    WireNavigation3ResultType(
        DrawingCanvasResultContract,
        DrawingCanvasResult.serializer(),
    )

object SketchNavigation3Contribution {
    val resultTypes: List<WireNavigation3ResultType<*>> =
        listOf(DrawingCanvasNavigation3ResultType)

    fun entryProviderInstallers(
        runtime: WireNavigation3Runtime,
    ): List<WireEntryProviderInstaller> = listOf(sketchNavigation3Entries(runtime))
}

internal fun sketchNavigation3Entries(
    runtime: WireNavigation3Runtime,
): WireEntryProviderInstaller = {
    wireEntry<DrawingCanvasRoute>(presentation = WireEntryPresentation.PopUp) { route ->
        DrawingCanvasRouteScreen(
            conversationName = route.conversationName,
            tempWritableUri = route.tempWritableUri?.toUri(),
            viewModel = wireMetroViewModel<DrawingCanvasViewModel>(),
            onDismiss = {
                if (!runtime.completeCurrentAndPop(
                        DrawingCanvasNavigation3ResultType,
                        WireNavResult.Canceled,
                    )
                ) {
                    runtime.navigator.goBack()
                }
            },
            onSketchSaved = { uri ->
                if (!runtime.completeCurrentAndPop(
                        DrawingCanvasNavigation3ResultType,
                        WireNavResult.Value(DrawingCanvasResult(uri.toString())),
                    )
                ) {
                    runtime.navigator.goBack()
                }
            },
        )
    }
}
