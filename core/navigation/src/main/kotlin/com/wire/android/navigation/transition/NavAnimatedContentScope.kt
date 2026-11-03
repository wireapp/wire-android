/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.navigation.transition

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.navigation3.ui.LocalNavAnimatedContentScope

/**
 * Animation scope of the Navigation 3 entry currently being rendered.
 *
 * Shared elements must use this scope so they are matched across entries while the entry
 * transition runs.
 */
val currentNavAnimatedContentScope: AnimatedContentScope
    @Composable
    @ReadOnlyComposable
    get() = LocalNavAnimatedContentScope.current
