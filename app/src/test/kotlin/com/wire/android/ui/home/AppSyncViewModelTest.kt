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
package com.wire.android.ui.home

import com.wire.android.config.CoroutineTestExtension
import com.wire.kalium.logic.feature.client.MLSClientManager
import com.wire.kalium.logic.feature.conversation.keyingmaterials.KeyingMaterialsManager
import com.wire.kalium.logic.feature.e2ei.SyncCertificateRevocationListUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.ObserveCertificateRevocationForSelfClientUseCase
import com.wire.kalium.logic.feature.featureConfig.FeatureFlagsSyncWorker
import com.wire.kalium.logic.feature.mlsmigration.MLSMigrationManager
import com.wire.kalium.logic.feature.server.UpdateApiVersionsUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class AppSyncViewModelTest {
    @Test
    fun `when startSyncingAppConfig is called then it should call the use case`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange {
            withObserveCertificateRevocationForSelfClient()
            withFeatureFlagsSyncWorker()
            withSyncCertificateRevocationListUseCase()
            withUpdateApiVersions()
            withMlsClientManager()
            withMlsMigrationManager()
            withKeyingMaterialsManager()
        }

        viewModel.startSyncingAppConfig()
        advanceUntilIdle()

        coVerify { arrangement.observeCertificateRevocationForSelfClient.invoke() }
        coVerify { arrangement.syncCertificateRevocationListUseCase.invoke() }
        coVerify { arrangement.featureFlagsSyncWorker.execute() }
        coVerify { arrangement.updateApiVersions() }
        coVerify { arrangement.mlsClientManager() }
        coVerify { arrangement.mlsMigrationManager() }
        coVerify { arrangement.keyingMaterialsManager() }
    }

    @Test
    fun `when startSyncingAppConfig is called multiple times then it should call the use case with delay`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange {
            withObserveCertificateRevocationForSelfClient(1000)
            withFeatureFlagsSyncWorker(1000)
            withSyncCertificateRevocationListUseCase(1000)
            withUpdateApiVersions(1000)
            withMlsClientManager(1000)
            withMlsMigrationManager(1000)
            withKeyingMaterialsManager(1000)
        }

        viewModel.startSyncingAppConfig()
        viewModel.startSyncingAppConfig()
        viewModel.startSyncingAppConfig()
        advanceUntilIdle()

        coVerify(exactly = 1) { arrangement.observeCertificateRevocationForSelfClient.invoke() }
        coVerify(exactly = 1) { arrangement.syncCertificateRevocationListUseCase.invoke() }
        coVerify(exactly = 1) { arrangement.featureFlagsSyncWorker.execute() }
        coVerify(exactly = 1) { arrangement.updateApiVersions() }
        coVerify(exactly = 1) { arrangement.mlsClientManager() }
        coVerify(exactly = 1) { arrangement.mlsMigrationManager() }
        coVerify(exactly = 1) { arrangement.keyingMaterialsManager() }
    }

    private class Arrangement {

        @MockK
        lateinit var syncCertificateRevocationListUseCase: SyncCertificateRevocationListUseCase

        @MockK
        lateinit var observeCertificateRevocationForSelfClient: ObserveCertificateRevocationForSelfClientUseCase

        @MockK
        lateinit var featureFlagsSyncWorker: FeatureFlagsSyncWorker

        @MockK
        lateinit var updateApiVersions: UpdateApiVersionsUseCase

        @MockK
        lateinit var mlsClientManager: MLSClientManager

        @MockK
        lateinit var mlsMigrationManager: MLSMigrationManager

        @MockK
        lateinit var keyingMaterialsManager: KeyingMaterialsManager

        init {
            MockKAnnotations.init(this)
        }

        private val viewModel = AppSyncViewModel(
            syncCertificateRevocationListUseCase,
            observeCertificateRevocationForSelfClient,
            featureFlagsSyncWorker,
            updateApiVersions,
            mLSClientManager = mlsClientManager,
            mLSMigrationManager = mlsMigrationManager,
            keyingMaterialsManager = keyingMaterialsManager
        )

        fun withObserveCertificateRevocationForSelfClient(delayMs: Long = 0) {
            coEvery { observeCertificateRevocationForSelfClient.invoke() } coAnswers {
                delay(delayMs)
            }
        }

        fun withSyncCertificateRevocationListUseCase(delayMs: Long = 0) {
            coEvery { syncCertificateRevocationListUseCase.invoke() } coAnswers {
                delay(delayMs)
            }
        }

        fun withFeatureFlagsSyncWorker(delayMs: Long = 0) {
            coEvery { featureFlagsSyncWorker.execute() } coAnswers {
                delay(delayMs)
            }
        }

        fun withUpdateApiVersions(delayMs: Long = 0) {
            coEvery { updateApiVersions() } coAnswers {
                delay(delayMs)
            }
        }

        fun withMlsClientManager(delayMs: Long = 0) {
            coEvery { mlsClientManager() } coAnswers {
                delay(delayMs)
            }
        }

        fun withMlsMigrationManager(delayMs: Long = 0) {
            coEvery { mlsMigrationManager() } coAnswers {
                delay(delayMs)
            }
        }

        fun withKeyingMaterialsManager(delayMs: Long = 0) {
            coEvery { keyingMaterialsManager() } coAnswers {
                delay(delayMs)
            }
        }

        fun arrange(block: Arrangement.() -> Unit) = apply(block).let {
            this to viewModel
        }
    }
}
