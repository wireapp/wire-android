/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
package com.wire.android.di

import androidx.compose.runtime.Composable
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.sebaslogen.resaca.hilt.hiltViewModelScoped
import dev.ahmedmourad.bundlizer.Bundlizer
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

inline fun <reified R : ScopedArgs> SavedStateHandle.scopedArgs(): R =
    scopedArgs(R::class, this)

@OptIn(InternalSerializationApi::class)
fun <R : ScopedArgs> scopedArgs(argsClass: KClass<R>, argsContainer: SavedStateHandle): R =
    Bundlizer.unbundle(argsClass.serializer(), argsContainer.toBundle())

@OptIn(InternalSerializationApi::class)
@Composable
inline fun <reified T : ViewModel, reified R : ScopedArgs> hiltViewModelScoped(arguments: R): T =
    hiltViewModelScoped(key = arguments.key, defaultArguments = Bundlizer.bundle(R::class.serializer(), arguments))

@Suppress("SpreadOperator")
fun SavedStateHandle.toBundle() = bundleOf(*(keys().map { it to get<Any>(it) }.toTypedArray()))

interface ScopedArgs {
    val key: Any?
}
