/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.wire.android.ui.common.R
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.UserAssetId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import okio.Path
import okio.Path.Companion.toPath
import javax.inject.Inject

@Stable
@Serializable
sealed class ImageAsset {

    /**
     * Represents an image asset that is stored locally on the device, and it isn't necessarily bounded to any specific conversation or
     * message, i.e. some preview images that the user selected from local device gallery.
     */
    @Stable
    @Serializable
    data class Local(
        val dataPath: @Serializable(with = PathAsStringSerializer::class) Path,
        val idKey: String
    ) : ImageAsset()

    @Serializable
    sealed class Remote : ImageAsset() {

        /**
         * Value that uniquely identifies this Asset,
         * can be used for caching purposes, for example.
         */
        abstract val uniqueKey: String

        @Composable
        fun paint(
            fallbackData: Any? = null,
            withCrossfadeAnimation: Boolean = false
        ) = when {
            LocalInspectionMode.current -> painterResource(id = R.drawable.mock_image)
            else -> hiltViewModel<RemoteAssetImageViewModel>().imageLoader
                .paint(asset = this, fallbackData = fallbackData, withCrossfadeAnimation = withCrossfadeAnimation)
        }
    }

    @Stable
    @Serializable
    data class UserAvatarAsset(
        val userAssetId: UserAssetId
    ) : Remote() {
        override val uniqueKey: String
            get() = userAssetId.toString()
    }

    @Stable
    @Serializable
    data class PrivateAsset(
        val conversationId: ConversationId,
        val messageId: String,
        val isSelfAsset: Boolean,
        val isEphemeral: Boolean = false
    ) : Remote() {
        override fun toString(): String = "$conversationId:$messageId:$isSelfAsset:$isEphemeral"
        override val uniqueKey: String
            get() = toString()
    }
}

fun String.parseIntoPrivateImageAsset(qualifiedIdMapper: QualifiedIdMapper): ImageAsset.PrivateAsset {
    val (conversationIdString, messageId, isSelfAsset, isEphemeral) = split(":")
    val conversationIdParam = qualifiedIdMapper.fromStringToQualifiedID(conversationIdString)

    return ImageAsset.PrivateAsset(conversationIdParam, messageId, isSelfAsset.toBoolean(), isEphemeral.toBoolean())
}

object PathAsStringSerializer : KSerializer<Path> {
    override val descriptor = PrimitiveSerialDescriptor("Path", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Path) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): Path = decoder.decodeString().toPath(normalize = true)
}

@HiltViewModel
class RemoteAssetImageViewModel @Inject constructor(val imageLoader: WireSessionImageLoader) : ViewModel()
