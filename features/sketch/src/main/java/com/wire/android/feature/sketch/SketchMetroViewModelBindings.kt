/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.feature.sketch

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@BindingContainer
object SketchMetroViewModelBindings {

    @Provides
    @IntoMap
    @ViewModelKey(DrawingCanvasViewModel::class)
    fun drawingCanvasViewModel(): ViewModel = DrawingCanvasViewModel()
}
