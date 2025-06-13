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
package com.wire.android.ui.home.conversations.media

import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.ui.sharing.ImportedMediaAsset
import com.wire.kalium.logic.data.asset.AttachmentType
import okio.Path.Companion.toPath
import org.junit.jupiter.api.Test

class CheckAssetRestrictionsViewModelTest {

    @Test
    fun `given valid asset, when checking restrictions, then should return success state`() {
        // given
        val asset = ImportedMediaAsset(Arrangement.testAssetBundle, null)
        val (_, viewModel) = Arrangement().arrange()
        // when
        viewModel.checkRestrictions(listOf(asset))
        // then
        assert(viewModel.state is RestrictionCheckState.Success)
    }

    @Test
    fun `given exceeded asset, when checking restrictions, then should return asset too large dialog state`() {
        // given
        val asset = ImportedMediaAsset(Arrangement.testAssetBundle, 1)
        val (_, viewModel) = Arrangement().arrange()
        // when
        viewModel.checkRestrictions(listOf(asset))
        // then
        assert(viewModel.state is RestrictionCheckState.AssetTooLarge)
    }

    class Arrangement {
        fun arrange() = this to CheckAssetRestrictionsViewModel()

        companion object {
            val testAssetBundle = AssetBundle(
                "key",
                "image/jpeg",
                "some-data-path".toPath(),
                1000L,
                "mocked_image.jpeg",
                AttachmentType.IMAGE
            )
        }
    }
}
