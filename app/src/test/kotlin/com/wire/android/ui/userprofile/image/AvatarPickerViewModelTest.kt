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

import app.cash.turbine.test
import com.wire.android.assertIs
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.datastore.UserDataStore
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.framework.FakeKaliumFileSystem
import com.wire.android.ui.userprofile.avatarpicker.AvatarPickerViewModel
import com.wire.android.framework.TestUser
import com.wire.content.external.ExternalContentReference
import com.wire.content.external.PlatformResult
import com.wire.content.media.ContentImageSource
import com.wire.content.media.ImageProcessor
import com.wire.content.media.ImageTargetProvider
import com.wire.content.media.ProcessedImage
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

        avatarPickerViewModel.updatePickedAvatar(arrangement.originalReference, arrangement.targetSource)
        assertInstanceOf(AvatarPickerViewModel.PictureState.Picked::class.java, avatarPickerViewModel.pictureState)
        avatarPickerViewModel.loadInitialAvatarState()
        assertInstanceOf(AvatarPickerViewModel.PictureState.Initial::class.java, avatarPickerViewModel.pictureState)
    }

    @Test
    fun `given no avatar is present, when new avatar is picked and cancel button pressed, then set state to Empty`() = runTest {
        val (arrangement, avatarPickerViewModel) = Arrangement()
            .withNoInitialAvatar()
            .arrange()

        avatarPickerViewModel.updatePickedAvatar(arrangement.originalReference, arrangement.targetSource)
        assertInstanceOf(AvatarPickerViewModel.PictureState.Picked::class.java, avatarPickerViewModel.pictureState)
        avatarPickerViewModel.loadInitialAvatarState()
        assertInstanceOf(AvatarPickerViewModel.PictureState.Empty::class.java, avatarPickerViewModel.pictureState)
    }

    @Test
    fun `given image processing fails, when a new avatar is picked, then keep the current state and emit an error`() = runTest {
        val (arrangement, avatarPickerViewModel) = Arrangement()
            .withNoInitialAvatar()
            .withImageProcessingFailure()
            .arrange()

        avatarPickerViewModel.infoMessage.test {
            avatarPickerViewModel.updatePickedAvatar(arrangement.originalReference, arrangement.targetSource)

            assertInstanceOf(AvatarPickerViewModel.PictureState.Empty::class.java, avatarPickerViewModel.pictureState)
            assertEquals(AvatarPickerViewModel.InfoMessageType.ImageProcessError.uiText, awaitItem())
        }
    }

    private class Arrangement {

        val userDataStore = mockk<UserDataStore>()

        val userDataStoreProvider = mockk<UserDataStoreProvider>()

        val getAvatarAsset = mockk<GetAvatarAssetUseCase>()

        val uploadUserAvatarUseCase = mockk<UploadUserAvatarUseCase>()

        val imageProcessor = mockk<ImageProcessor>()

        val imageTargetProvider = mockk<ImageTargetProvider>()

        @MockK
        private lateinit var qualifiedIdMapper: QualifiedIdMapper

        val viewModel by lazy {
            AvatarPickerViewModel(
                userDataStoreProvider,
                TestUser.SELF_USER.id,
                getAvatarAsset,
                uploadUserAvatarUseCase,
                imageProcessor,
                imageTargetProvider,
                fakeKaliumFileSystem,
                qualifiedIdMapper,
            )
        }

        val originalReference = ExternalContentReference("content://original")
        val targetReference = ExternalContentReference("content://target")
        val targetSource = ContentImageSource.Local(fakeKaliumFileSystem.selfUserAvatarPath())

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { userDataStoreProvider.getOrCreate(TestUser.SELF_USER.id) } returns userDataStore
            every { imageTargetProvider.createTarget(any()) } returns PlatformResult.Success(targetReference)
            coEvery { imageProcessor.process(any()) } returns
                PlatformResult.Success(ProcessedImage(fakeKaliumFileSystem.selfUserAvatarPath(), 5L))
        }

        fun withSuccessfulInitialAvatarLoad(): Arrangement {
            val avatarAssetId = "avatar-value@avatar-domain"
            val fakeAvatarData = "some-dummy-avatar".toByteArray()
            val avatarPath = fakeKaliumFileSystem.selfUserAvatarPath()
            fakeKaliumFileSystem.sink(avatarPath).buffer().use {
                it.write(fakeAvatarData)
            }
            coEvery { getAvatarAsset(any()) } returns PublicAssetResult.Success(avatarPath)
            every { userDataStore.avatarAssetId } returns flow { emit(avatarAssetId) }
            every { qualifiedIdMapper.fromStringToQualifiedID(any()) } returns QualifiedID("avatar-value", "avatar-domain")

            return this
        }

        fun withFailedInitialAvatarLoad(): Arrangement {
            val avatarAssetId = "avatar-value@avatar-domain"
            coEvery { getAvatarAsset(any()) } returns PublicAssetResult.Failure(Unknown(RuntimeException("some error")), false)
            every { userDataStore.avatarAssetId } returns flow { emit(avatarAssetId) }
            every { qualifiedIdMapper.fromStringToQualifiedID(any()) } returns QualifiedID("avatar-value", "avatar-domain")

            return this
        }

        fun withNoInitialAvatar(): Arrangement {
            every { userDataStore.avatarAssetId } returns flow { emit(null) }

            return this
        }

        fun withImageProcessingFailure(): Arrangement {
            coEvery { imageProcessor.process(any()) } returns PlatformResult.Failure("processing_failed")

            return this
        }

        fun withSuccessfulAvatarUpload(expectedUserAssetId: UserAssetId): Arrangement {
            coEvery { userDataStore.updateUserAvatarAssetId(any()) } returns Unit
            coEvery { uploadUserAvatarUseCase(any(), any()) } returns UploadAvatarResult.Success(expectedUserAssetId)

            return this
        }

        fun withErrorUploadResponse(): Arrangement {
            coEvery {
                uploadUserAvatarUseCase(any(), any())
            } returns UploadAvatarResult.Failure(Unknown(RuntimeException("some error")))

            return this
        }

        fun arrange() =
            this to viewModel
    }

    companion object {
        val fakeKaliumFileSystem: FakeKaliumFileSystem = FakeKaliumFileSystem()
    }
}
