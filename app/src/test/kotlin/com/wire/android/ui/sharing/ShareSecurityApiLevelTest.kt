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

package com.wire.android.ui.sharing

import android.app.Application
import android.app.ComponentCaller
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.paging.PagingData
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.ui.captureIntentRequest
import com.wire.android.ui.home.conversations.usecase.GetConversationsFromSearchUseCase
import com.wire.android.ui.home.conversations.usecase.HandleUriAssetUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ShareSecurityApiLevelTest {

    private val dispatchers = TestDispatcherProvider()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatchers.main())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun givenAndroid14_whenWireProviderUriIsSharedThenItIsRejected() = runTest(dispatchers.main()) {
        val activity = sharingActivity()
        val (viewModel, handleUriAsset) = viewModel()

        val trustedCaller = activity.captureIntentRequest(activity.intent).hasTrustedWireShareCaller
        viewModel.handleReceivedDataFromSharingIntent(activity)

        assertFalse(trustedCaller)
        assertTrue(listOf(WIRE_URI).shouldRejectSharingIntent(PROVIDER_AUTHORITY, trustedCaller))
        coVerify(exactly = 0) { handleUriAsset.invoke(any(), any()) }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
    fun givenAndroid15_whenCallerCanReadWireProviderUriThenItIsAccepted() = runTest(dispatchers.main()) {
        val activity = sharingActivity(PackageManager.PERMISSION_GRANTED)
        val (viewModel, handleUriAsset) = viewModel()

        val trustedCaller = activity.captureIntentRequest(activity.intent).hasTrustedWireShareCaller
        viewModel.handleReceivedDataFromSharingIntent(activity)

        assertTrue(trustedCaller)
        assertFalse(listOf(WIRE_URI).shouldRejectSharingIntent(PROVIDER_AUTHORITY, trustedCaller))
        coVerify(exactly = 1) { handleUriAsset.invoke(WIRE_URI, false) }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
    fun givenAndroid15_whenCallerCannotReadWireProviderUriThenItIsRejected() = runTest(dispatchers.main()) {
        val activity = sharingActivity(PackageManager.PERMISSION_DENIED)
        val (viewModel, handleUriAsset) = viewModel()

        val trustedCaller = activity.captureIntentRequest(activity.intent).hasTrustedWireShareCaller
        viewModel.handleReceivedDataFromSharingIntent(activity)

        assertFalse(trustedCaller)
        assertTrue(listOf(WIRE_URI).shouldRejectSharingIntent(PROVIDER_AUTHORITY, trustedCaller))
        coVerify(exactly = 0) { handleUriAsset.invoke(any(), any()) }
    }

    private fun sharingActivity(permission: Int? = null): AppCompatActivity {
        val sharingIntent = Intent(Intent.ACTION_SEND).apply {
            type = MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, WIRE_URI)
        }
        val caller = permission?.let {
            mockk<ComponentCaller> {
                every {
                    checkContentUriPermission(WIRE_URI, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } returns permission
            }
        }
        return mockk(relaxed = true) {
            every { intent } returns sharingIntent
            every { packageName } returns PACKAGE_NAME
            caller?.let { every { this@mockk.caller } returns it }
        }
    }

    private fun viewModel(): Pair<ImportMediaAuthenticatedViewModel, HandleUriAssetUseCase> {
        val context = mockk<Context> {
            every { packageName } returns PACKAGE_NAME
        }
        val getSelf = mockk<ObserveSelfUserUseCase> {
            coEvery { this@mockk.invoke() } returns emptyFlow()
        }
        val getConversations = mockk<GetConversationsFromSearchUseCase> {
            coEvery {
                this@mockk.invoke(any(), any(), any(), any(), useStrictMlsFilter = any())
            } returns flowOf(PagingData.empty())
        }
        val handleUriAsset = mockk<HandleUriAssetUseCase> {
            coEvery { this@mockk.invoke(any(), any()) } returns HandleUriAssetUseCase.Result.Failure.Unknown
        }
        val viewModel = ImportMediaAuthenticatedViewModel(
            context = context,
            getSelf = getSelf,
            getConversationsPaginated = getConversations,
            handleUriAsset = handleUriAsset,
            persistNewSelfDeletionTimerUseCase = mockk(relaxed = true),
            observeSelfDeletionSettingsForConversation = mockk(relaxed = true),
            dispatchers = dispatchers,
        )
        return viewModel to handleUriAsset
    }

    private companion object {
        const val PACKAGE_NAME = "com.wire.android"
        const val PROVIDER_AUTHORITY = "$PACKAGE_NAME.provider"
        const val MIME_TYPE = "application/zip"
        val WIRE_URI: Uri = Uri.parse("content://$PROVIDER_AUTHORITY/shared_files/image.jpg")
    }
}
