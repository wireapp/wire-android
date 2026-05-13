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
import com.wire.android.framework.FakeKaliumFileSystem
import com.wire.android.ui.userprofile.avatarpicker.AvatarImageGateway
import com.wire.android.ui.userprofile.avatarpicker.AvatarPickerViewModel
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
import okio.Path
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
                        uploadUserAvatarUseCase(any(), 5L)
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
                    uploadUserAvatarUseCase(any(), 5L)
                }
                assertEquals(1, avatarImageGateway.writableAvatarUriCalls)
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
        assertEquals(listOf(arrangement.mockOriginalUri), arrangement.avatarImageGateway.sanitizedAvatarUris)
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
        assertEquals(listOf(arrangement.mockOriginalUri), arrangement.avatarImageGateway.sanitizedAvatarUris)
        assertInstanceOf(AvatarPickerViewModel.PictureState.Picked::class.java, avatarPickerViewModel.pictureState)
        avatarPickerViewModel.loadInitialAvatarState()
        assertInstanceOf(AvatarPickerViewModel.PictureState.Empty::class.java, avatarPickerViewModel.pictureState)
    }

    private class Arrangement {

        val userDataStore = mockk<UserDataStore>()

        val getAvatarAsset = mockk<GetAvatarAssetUseCase>()

        val uploadUserAvatarUseCase = mockk<UploadUserAvatarUseCase>()

        val avatarImageGateway = FakeAvatarImageGateway()

        @MockK
        private lateinit var qualifiedIdMapper: QualifiedIdMapper

        val viewModel by lazy {
            AvatarPickerViewModel(
                userDataStore,
                getAvatarAsset,
                uploadUserAvatarUseCase,
                avatarImageGateway,
                fakeKaliumFileSystem,
                qualifiedIdMapper
            )
        }

        val mockTargetUri = "file://target-avatar"
        val mockOriginalUri = "content://original-avatar"

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun withSuccessfulInitialAvatarLoad(): Arrangement {
            val avatarAssetId = "avatar-value@avatar-domain"
            val fakeAvatarData = "some-dummy-avatar".toByteArray()
            val avatarPath = fakeKaliumFileSystem.selfUserAvatarPath()
            fakeKaliumFileSystem.sink(avatarPath).buffer().use {
                it.write(fakeAvatarData)
            }
            coEvery { getAvatarAsset(any()) } returns PublicAssetResult.Success(avatarPath)
            avatarImageGateway.imageSize = 5L
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

    private class FakeAvatarImageGateway : AvatarImageGateway {
        var imageSize: Long = 0L
        var writableAvatarUriCalls: Int = 0
            private set
        val sanitizedAvatarUris = mutableListOf<String>()

        override fun getWritableAvatarUri(avatarPath: Path): String {
            writableAvatarUriCalls++
            return TARGET_AVATAR_URI
        }

        override fun getShareableTempAvatarUri(avatarPath: Path): String = TARGET_AVATAR_URI

        override suspend fun sanitizeAvatarImage(originalAvatarUri: String, avatarPath: Path) {
            sanitizedAvatarUris += originalAvatarUri
        }

        override suspend fun getAvatarImageSize(avatarUri: String): Long = imageSize
    }

    companion object {
        val fakeKaliumFileSystem: FakeKaliumFileSystem = FakeKaliumFileSystem()
        const val TARGET_AVATAR_URI: String = "file://target-avatar"
    }
}
