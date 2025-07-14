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

package com.wire.android.ui.userprofile.image

import android.content.Context
import android.net.Uri
import app.cash.turbine.test
import com.wire.android.assertIs
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.datastore.UserDataStore
import com.wire.android.framework.FakeKaliumFileSystem
import com.wire.android.ui.userprofile.avatarpicker.AvatarPickerViewModel
import com.wire.android.util.AvatarImageManager
import com.wire.android.util.resampleImageAndCopyToTempPath
import com.wire.android.util.toByteArray
import com.wire.kalium.common.error.CoreFailure.Unknown
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.AssetId
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.feature.asset.GetAvatarAssetUseCase
import com.wire.kalium.logic.feature.asset.PublicAssetResult
import com.wire.kalium.logic.feature.user.UploadAvatarResult
import com.wire.kalium.logic.feature.user.UploadUserAvatarUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import okio.buffer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class AvatarPickerViewModelTest {

    @Test
    fun `given a valid image, when uploading the asset succeeds, then the useCase should be called and navigate back on success`() =
        runTest {
            // Given
            val uploadedAssetId = AssetId("some-dummy-value", "some-dummy-domain")

            val (arrangement, avatarPickerViewModel) = Arrangement()
                .withSuccessfulInitialAvatarLoad()
                .withSuccessfulAvatarUpload(uploadedAssetId)
                .arrange()

            avatarPickerViewModel.infoMessage.test {
                // When
                avatarPickerViewModel.uploadNewPickedAvatar()

                // Then
                with(arrangement) {
                    coVerify {
                        uploadUserAvatarUseCase(any(), any())
                        userDataStore.updateUserAvatarAssetId(uploadedAssetId.toString())
                    }
                    assertIs<AvatarPickerViewModel.PictureState.Completed>(avatarPickerViewModel.pictureState)
                }

                expectNoEvents()
            }
        }

    @Test
    fun `given a valid image, when uploading the asset fails, then should emit an error`() = runTest {
        // Given
        val (arrangement, avatarPickerViewModel) = Arrangement()
            .withSuccessfulInitialAvatarLoad()
            .withErrorUploadResponse()
            .arrange()

        avatarPickerViewModel.infoMessage.test {
            // When
            avatarPickerViewModel.uploadNewPickedAvatar()

            // Then
            with(arrangement) {
                coVerify {
                    uploadUserAvatarUseCase(any(), any())
                }
                coVerify(exactly = 1) {
                    avatarImageManager.getWritableAvatarUri(any())
                }
                assertIs<AvatarPickerViewModel.PictureState.Initial>(avatarPickerViewModel.pictureState) // not PictureState.Completed
            }

            assertEquals(AvatarPickerViewModel.InfoMessageType.UploadAvatarError.uiText, awaitItem())
        }
    }

    @Test
    fun `given current avatar download failed, when uploading the asset fails, then set state as Empty`() = runTest {
        // Given
        val (arrangement, avatarPickerViewModel) = Arrangement()
            .withFailedInitialAvatarLoad()
            .withErrorUploadResponse()
            .arrange()
        // When
        avatarPickerViewModel.uploadNewPickedAvatar()
        // Then
        assertInstanceOf(AvatarPickerViewModel.PictureState.Empty::class.java, avatarPickerViewModel.pictureState)
    }

    @Test
    fun `given current avatar download succeeded, when uploading the asset fails, then set state as Initial`() = runTest {
        // Given
        val (arrangement, avatarPickerViewModel) = Arrangement()
            .withSuccessfulInitialAvatarLoad()
            .withErrorUploadResponse()
            .arrange()
        // When
        avatarPickerViewModel.uploadNewPickedAvatar()
        // Then
        assertInstanceOf(AvatarPickerViewModel.PictureState.Initial::class.java, avatarPickerViewModel.pictureState)
    }

    @Test
    fun `given current avatar present, when new avatar is picked and cancel button pressed, then set state to Initial`() = runTest {
        val (arrangement, avatarPickerViewModel) = Arrangement()
            .withSuccessfulInitialAvatarLoad()
            .arrange()

        avatarPickerViewModel.updatePickedAvatarUri(arrangement.mockOriginalUri, arrangement.mockTargetUri)
        assertInstanceOf(AvatarPickerViewModel.PictureState.Picked::class.java, avatarPickerViewModel.pictureState)
        avatarPickerViewModel.loadInitialAvatarState()
        assertInstanceOf(AvatarPickerViewModel.PictureState.Initial::class.java, avatarPickerViewModel.pictureState)
    }

    @Test
    fun `given no avatar is present, when new avatar is picked and cancel button pressed, then set state to Empty`() = runTest {
        val (arrangement, avatarPickerViewModel) = Arrangement()
            .withNoInitialAvatar()
            .arrange()

        avatarPickerViewModel.updatePickedAvatarUri(arrangement.mockOriginalUri, arrangement.mockTargetUri)
        assertInstanceOf(AvatarPickerViewModel.PictureState.Picked::class.java, avatarPickerViewModel.pictureState)
        avatarPickerViewModel.loadInitialAvatarState()
        assertInstanceOf(AvatarPickerViewModel.PictureState.Empty::class.java, avatarPickerViewModel.pictureState)
    }

    private class Arrangement {

        val userDataStore = mockk<UserDataStore>()

        val getAvatarAsset = mockk<GetAvatarAssetUseCase>()

        val uploadUserAvatarUseCase = mockk<UploadUserAvatarUseCase>()

        val avatarImageManager = mockk<AvatarImageManager>()

        val context = mockk<Context>()

        @MockK
        private lateinit var qualifiedIdMapper: QualifiedIdMapper

        val dispatcherProvider = TestDispatcherProvider()

        val viewModel by lazy {
            AvatarPickerViewModel(
                userDataStore,
                getAvatarAsset,
                uploadUserAvatarUseCase,
                avatarImageManager,
                dispatcherProvider,
                fakeKaliumFileSystem,
                qualifiedIdMapper,
                context
            )
        }

        val mockTargetUri = mockk<Uri>()
        val mockOriginalUri = mockk<Uri>()

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun withSuccessfulInitialAvatarLoad(): Arrangement {
            val avatarAssetId = "avatar-value@avatar-domain"
            mockkStatic(Uri::class)
            mockkStatic(Uri::resampleImageAndCopyToTempPath)
            mockkStatic(Uri::toByteArray)
            every { Uri.parse(any()) } returns mockTargetUri
            val fakeAvatarData = "some-dummy-avatar".toByteArray()
            val avatarPath = fakeKaliumFileSystem.selfUserAvatarPath()
            fakeKaliumFileSystem.sink(avatarPath).buffer().use {
                it.write(fakeAvatarData)
            }
            coEvery { getAvatarAsset(any()) } returns PublicAssetResult.Success(avatarPath)
            coEvery { avatarImageManager.getWritableAvatarUri(any()) } returns mockTargetUri
            coEvery { avatarImageManager.getShareableTempAvatarUri(any()) } returns mockTargetUri
            coEvery { any<Uri>().resampleImageAndCopyToTempPath(any(), any(), any(), eq(true), any()) } returns 1L
            coEvery { any<Uri>().toByteArray(any(), any()) } returns ByteArray(5)
            every { userDataStore.avatarAssetId } returns flow { emit(avatarAssetId) }
            every { qualifiedIdMapper.fromStringToQualifiedID(any()) } returns QualifiedID("avatar-value", "avatar-domain")

            return this
        }

        fun withFailedInitialAvatarLoad(): Arrangement {
            val avatarAssetId = "avatar-value@avatar-domain"
            coEvery { getAvatarAsset(any()) } returns PublicAssetResult.Failure(Unknown(RuntimeException("some error")), false)
            coEvery { avatarImageManager.getShareableTempAvatarUri(any()) } returns mockTargetUri
            every { userDataStore.avatarAssetId } returns flow { emit(avatarAssetId) }
            every { qualifiedIdMapper.fromStringToQualifiedID(any()) } returns QualifiedID("avatar-value", "avatar-domain")

            return this
        }

        fun withNoInitialAvatar(): Arrangement {
            coEvery { avatarImageManager.getShareableTempAvatarUri(any()) } returns mockTargetUri
            every { userDataStore.avatarAssetId } returns flow { emit(null) }

            return this
        }

        fun withSuccessfulAvatarUpload(expectedUserAssetId: UserAssetId): Arrangement {
            coEvery { userDataStore.updateUserAvatarAssetId(any()) } returns Unit
            coEvery { uploadUserAvatarUseCase(any(), any()) } returns UploadAvatarResult.Success(expectedUserAssetId)

            return this
        }

        fun withErrorUploadResponse(): Arrangement {
            coEvery { uploadUserAvatarUseCase(any(), any()) } returns UploadAvatarResult.Failure(Unknown(RuntimeException("some error")))

            return this
        }

        fun arrange() =
            this to viewModel
    }

    companion object {
        val fakeKaliumFileSystem: FakeKaliumFileSystem = FakeKaliumFileSystem()
    }
}
