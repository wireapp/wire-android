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
package com.wire.android.ui.home.conversations

import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.sebaslogen.resaca.KeyInScopeResolver
import com.wire.android.di.ScopedArgs
import com.wire.android.di.metro.MetroViewModelGraph
import com.wire.android.di.wireMetroViewModelScoped
import com.wire.android.media.audiomessage.AudioMessageArgs
import com.wire.android.media.audiomessage.AudioMessageViewModel
import com.wire.android.media.audiomessage.AudioMessageViewModelImpl
import com.wire.android.ui.home.conversations.edit.MessageOptionsMenuArgs
import com.wire.android.ui.home.conversations.edit.MessageOptionsMenuViewModel
import com.wire.android.ui.home.conversations.edit.MessageOptionsMenuViewModelImpl
import com.wire.android.ui.home.conversations.messages.item.AssetLocalPathArgs
import com.wire.android.ui.home.conversations.messages.item.AssetLocalPathViewModel
import com.wire.android.ui.home.conversations.messages.item.AssetLocalPathViewModelImpl
import com.wire.android.ui.home.conversations.model.CompositeMessageArgs
import com.wire.android.ui.home.conversations.typing.TypingIndicatorArgs
import com.wire.android.ui.home.conversations.typing.TypingIndicatorViewModel
import com.wire.android.ui.home.conversations.typing.TypingIndicatorViewModelImpl
import com.wire.android.ui.home.messagecomposer.actions.SelfDeletingMessageActionArgs
import com.wire.android.ui.home.messagecomposer.actions.SelfDeletingMessageActionViewModel
import com.wire.android.ui.home.messagecomposer.actions.SelfDeletingMessageActionViewModelImpl
import com.wire.android.ui.home.messagecomposer.attachments.IsFileSharingEnabledViewModel
import com.wire.android.ui.home.messagecomposer.attachments.IsFileSharingEnabledViewModelImpl
import com.wire.android.ui.home.messagecomposer.recordaudio.RecordAudioViewModel
import kotlin.time.Duration

interface ScopedMessageViewModelGraph : MetroViewModelGraph {
    val scopedMessageViewModelFactory: ScopedMessageViewModelFactory
}

@Composable
private inline fun <reified VM, reified S, reified R : ScopedArgs> scopedMessageViewModel(
    arguments: R,
    clearDelay: Duration? = null,
    noinline create: ScopedMessageViewModelFactory.(SavedStateHandle, R) -> VM,
): S where VM : ViewModel =
    wireMetroViewModelScoped<ScopedMessageViewModelGraph, VM, S, R>(
        arguments = arguments,
        clearDelay = clearDelay,
    ) { savedStateHandle, scopedArgs ->
        scopedMessageViewModelFactory.create(savedStateHandle, scopedArgs)
    }

@Composable
private inline fun <reified VM, reified S, reified R : ScopedArgs> scopedMessageViewModel(
    arguments: R,
    noinline keyInScopeResolver: KeyInScopeResolver<String>,
    clearDelay: Duration? = null,
    noinline create: ScopedMessageViewModelFactory.(SavedStateHandle, R) -> VM,
): S where VM : ViewModel =
    wireMetroViewModelScoped<ScopedMessageViewModelGraph, VM, S, R>(
        arguments = arguments,
        keyInScopeResolver = keyInScopeResolver,
        clearDelay = clearDelay,
    ) { savedStateHandle, scopedArgs ->
        scopedMessageViewModelFactory.create(savedStateHandle, scopedArgs)
    }

@Composable
private inline fun <reified VM, reified S> scopedMessageViewModel(
    clearDelay: Duration? = null,
    noinline create: ScopedMessageViewModelFactory.(SavedStateHandle) -> VM,
): S where VM : ViewModel =
    wireMetroViewModelScoped<ScopedMessageViewModelGraph, VM, S>(
        clearDelay = clearDelay,
    ) { savedStateHandle ->
        scopedMessageViewModelFactory.create(savedStateHandle)
    }

@Composable
fun compositeMessageViewModel(args: CompositeMessageArgs): CompositeMessageViewModel =
    scopedMessageViewModel<CompositeMessageViewModelImpl, CompositeMessageViewModel, CompositeMessageArgs>(
        arguments = args,
    ) { savedStateHandle, scopedArgs ->
        compositeMessageViewModel(savedStateHandle, scopedArgs)
    }

@Composable
fun messageOptionsMenuViewModel(args: MessageOptionsMenuArgs): MessageOptionsMenuViewModel =
    scopedMessageViewModel<MessageOptionsMenuViewModelImpl, MessageOptionsMenuViewModel, MessageOptionsMenuArgs>(args) { _, scopedArgs ->
        messageOptionsMenuViewModel(scopedArgs)
    }

@Composable
fun typingIndicatorViewModel(args: TypingIndicatorArgs): TypingIndicatorViewModel =
    scopedMessageViewModel<TypingIndicatorViewModelImpl, TypingIndicatorViewModel, TypingIndicatorArgs>(args) { _, scopedArgs ->
        typingIndicatorViewModel(scopedArgs)
    }

@Composable
fun assetLocalPathViewModel(
    args: AssetLocalPathArgs,
    keyInScopeResolver: KeyInScopeResolver<String>? = null,
): AssetLocalPathViewModel =
    if (keyInScopeResolver != null) {
        scopedMessageViewModel<AssetLocalPathViewModelImpl, AssetLocalPathViewModel, AssetLocalPathArgs>(
            arguments = args,
            keyInScopeResolver = keyInScopeResolver,
        ) { _, scopedArgs ->
            assetLocalPathViewModel(scopedArgs)
        }
    } else {
        scopedMessageViewModel<AssetLocalPathViewModelImpl, AssetLocalPathViewModel, AssetLocalPathArgs>(args) { _, scopedArgs ->
            assetLocalPathViewModel(scopedArgs)
        }
    }

@Composable
fun selfDeletingMessageActionViewModel(args: SelfDeletingMessageActionArgs): SelfDeletingMessageActionViewModel =
    scopedMessageViewModel<
            SelfDeletingMessageActionViewModelImpl,
            SelfDeletingMessageActionViewModel,
            SelfDeletingMessageActionArgs,
            >(args) { _, scopedArgs ->
        selfDeletingMessageActionViewModel(scopedArgs)
    }

@Composable
fun isFileSharingEnabledViewModel(): IsFileSharingEnabledViewModel =
    scopedMessageViewModel<IsFileSharingEnabledViewModelImpl, IsFileSharingEnabledViewModel> { _ ->
        isFileSharingEnabledViewModel()
    }

@Composable
fun recordAudioViewModel(): RecordAudioViewModel =
    wireMetroViewModelScoped<ScopedMessageViewModelGraph, RecordAudioViewModel> {
        scopedMessageViewModelFactory.recordAudioViewModel()
    }

@Composable
fun audioMessageViewModel(
    args: AudioMessageArgs,
    keyInScopeResolver: KeyInScopeResolver<String>? = null,
): AudioMessageViewModel =
    if (keyInScopeResolver != null) {
        scopedMessageViewModel<AudioMessageViewModelImpl, AudioMessageViewModel, AudioMessageArgs>(
            arguments = args,
            keyInScopeResolver = keyInScopeResolver,
        ) { _, scopedArgs ->
            audioMessageViewModel(scopedArgs)
        }
    } else {
        scopedMessageViewModel<AudioMessageViewModelImpl, AudioMessageViewModel, AudioMessageArgs>(args) { _, scopedArgs ->
            audioMessageViewModel(scopedArgs)
        }
    }
