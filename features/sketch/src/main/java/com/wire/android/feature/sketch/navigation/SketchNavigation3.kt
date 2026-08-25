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

import com.wire.navigation.SessionRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireNavResultContract
import com.wire.navigation.WireNavResultContractId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.Serializable

@Serializable
data class DrawingCanvasRoute(
    override val sessionId: WireSessionId,
    val conversationName: String,
    val tempWritableUri: String?,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String
        get() = ROUTE_ID

    companion object {
        const val ROUTE_ID = "sketch/drawing_canvas_screen"
    }
}

@Serializable
data class DrawingCanvasResult(
    val uri: String,
)

internal val DrawingCanvasResultContract = WireNavResultContract<DrawingCanvasResult>(
    WireNavResultContractId("sketch.drawing-canvas")
)
