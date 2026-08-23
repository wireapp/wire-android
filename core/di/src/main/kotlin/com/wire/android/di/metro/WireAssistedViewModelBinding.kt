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
package com.wire.android.di.metro

import kotlin.reflect.KClass

/** Declares one generated Metro manual-assisted factory group. */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class WireAssistedViewModelFactoryGroup(
    val factoryName: String = "",
)

/**
 * Adds an assisted ViewModel to a generated factory group.
 *
 * The annotated ViewModel must declare exactly one nested `@AssistedFactory` with one `create` method.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class WireAssistedViewModelBinding(
    val group: KClass<*>,
    val factoryMethod: String = "",
)
