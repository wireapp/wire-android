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

package com.wire.android.model

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.UserAssetId

@Stable
sealed class ImageAsset(private val imageLoader: WireSessionImageLoader) {
    /**
     * Value that uniquely identifies this Asset,
     * can be used for caching purposes, for example.
     */
    abstract val uniqueKey: String

    @Stable
    data class UserAvatarAsset(
        private val imageLoader: WireSessionImageLoader,
        val userAssetId: UserAssetId
    ) : ImageAsset(imageLoader) {
        override val uniqueKey: String
            get() = userAssetId.toString()
    }

    /**
     * Represents an image asset that is stored locally on the device, and it isn't necessarily bounded to any specific conversation or
     * message, i.e. some preview images that the user selected from local device gallery.
     */
    @Stable
    data class LocalImageAsset(
        private val imageLoader: WireSessionImageLoader,
        val dataUri: Uri,
        val idKey: String
    ) : ImageAsset(imageLoader) {

        override val uniqueKey: String
            get() = idKey
    }

    @Stable
    data class PrivateAsset(
        private val imageLoader: WireSessionImageLoader,
        val conversationId: ConversationId,
        val messageId: String,
        val isSelfAsset: Boolean,
        val isEphemeral: Boolean = false
    ) : ImageAsset(imageLoader) {
        override fun toString(): String = "$conversationId:$messageId:$isSelfAsset:$isEphemeral"
        override val uniqueKey: String
            get() = toString()
    }

    @Composable
    fun paint(
        fallbackData: Any? = null,
        withCrossfadeAnimation: Boolean = false
    ) = imageLoader.paint(asset = this, fallbackData = fallbackData, withCrossfadeAnimation = withCrossfadeAnimation)
}

fun String.parseIntoPrivateImageAsset(
    imageLoader: WireSessionImageLoader,
    qualifiedIdMapper: QualifiedIdMapper,
): ImageAsset.PrivateAsset {
    val (conversationIdString, messageId, isSelfAsset, isEphemeral) = split(":")
    val conversationIdParam = qualifiedIdMapper.fromStringToQualifiedID(conversationIdString)

    return ImageAsset.PrivateAsset(imageLoader, conversationIdParam, messageId, isSelfAsset.toBoolean(), isEphemeral.toBoolean())
}
