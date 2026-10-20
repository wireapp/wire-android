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
package com.wire.android.di.metro

import androidx.paging.PagingData
import com.wire.android.di.CoreLogicModule
import com.wire.android.di.CurrentAccount
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.di.UseCaseModule
import com.wire.android.di.accountScoped.AppsModule
import com.wire.android.di.accountScoped.AuthenticationModule
import com.wire.android.di.accountScoped.BackupModule
import com.wire.android.di.accountScoped.CallsModule
import com.wire.android.di.accountScoped.CellsModule
import com.wire.android.di.accountScoped.ConnectionModule
import com.wire.android.di.accountScoped.ServicesModule
import com.wire.android.di.accountScoped.TeamModule
import com.wire.kalium.cells.CellsScope
import com.wire.kalium.cells.KaliumCellsApi
import com.wire.kalium.cells.domain.model.CellConversation
import com.wire.kalium.cells.domain.usecase.GetCellConversationsPagedUseCase
import com.wire.kalium.cells.domain.usecase.RemoveAttachmentDraftsUseCase
import com.wire.kalium.cells.paginatedConversationsFlowUseCase
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.di.KaliumAppApi
import com.wire.kalium.logic.di.KaliumAuthenticationVerificationApi
import com.wire.kalium.logic.di.KaliumBackupApi
import com.wire.kalium.logic.di.KaliumCallsApi
import com.wire.kalium.logic.di.KaliumConnectionApi
import com.wire.kalium.logic.di.KaliumGlobalValidationApi
import com.wire.kalium.logic.di.KaliumServiceApi
import com.wire.kalium.logic.di.KaliumSessionSelectionApi
import com.wire.kalium.logic.di.KaliumTeamApi
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.app.GetAppByIdUseCase
import com.wire.kalium.logic.feature.auth.AuthenticationScope
import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import com.wire.kalium.logic.feature.auth.verification.RequestSecondFactorVerificationCodeUseCase
import com.wire.kalium.logic.feature.backup.VerifyBackupUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.connection.CancelConnectionRequestUseCase
import com.wire.kalium.logic.feature.service.ObserveAllServicesUseCase
import com.wire.kalium.logic.feature.user.IsSelfATeamMemberUseCase
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/** Generated production-module wiring. Actual SDK graph/capture identities are tested in SDK suites. */
class RemainingKaliumApiModuleTest {
    @Test
    @Suppress("LongMethod") // One generated graph verifies all factories and borrowed owner ports together.
    fun givenFreshFactoriesAndBorrowedOwners_whenResolving_thenFreshApisAreNotRetainedByTheParentGraph() {
        val account = UserId("remaining-apis", "wire.example")
        val core = mockk<CoreLogic>()
        val session = mockk<UserSessionScope>()
        every { core.getSessionScope(account) } returns session
        var appCreations = 0
        every { session.appApiFactory } returns KaliumAppApi.Factory {
            appCreations++
            mockk<KaliumAppApi>().also { api -> every { api.appGetAppById } returns mockk() }
        }
        every { session.callsApiFactory } returns KaliumCallsApi.Factory {
            mockk<KaliumCallsApi>().also { api -> every { api.callsEstablishedCall } returns mockk() }
        }
        every { session.backupApiFactory } returns KaliumBackupApi.Factory {
            mockk<KaliumBackupApi>().also { api -> every { api.backupVerify } returns mockk() }
        }
        every { session.teamApiFactory } returns KaliumTeamApi.Factory {
            mockk<KaliumTeamApi>().also { api -> every { api.teamIsSelfATeamMember } returns mockk() }
        }
        every { session.serviceApiFactory } returns KaliumServiceApi.Factory {
            mockk<KaliumServiceApi>().also { api -> every { api.serviceObserveAllServices } returns mockk() }
        }
        every { session.connectionApiFactory } returns KaliumConnectionApi.Factory {
            mockk<KaliumConnectionApi>().also { api -> every { api.connectionCancelConnectionRequest } returns mockk() }
        }
        val cellsOwner = mockk<CellsScope>()
        val cellsApi = mockk<KaliumCellsApi>()
        val removeDrafts = mockk<RemoveAttachmentDraftsUseCase>()
        every { session.cells } returns cellsOwner
        every { cellsOwner.individualApi } returns cellsApi
        every { cellsApi.cellsRemoveAttachments } returns removeDrafts
        val authOwner = mockk<AuthenticationScope>()
        val verificationApi = mockk<KaliumAuthenticationVerificationApi>()
        val requestCode = mockk<RequestSecondFactorVerificationCodeUseCase>()
        every { session.authenticationScope } returns authOwner
        every { authOwner.individualVerificationApi } returns verificationApi
        every { verificationApi.requestSecondFactorVerificationCode } returns requestCode
        val validation = mockk<KaliumGlobalValidationApi>()
        val validateEmail = mockk<ValidateEmailUseCase>()
        every { core.globalValidationApi } returns validation
        every { validation.globalValidateEmailUseCase } returns validateEmail
        val graph = createGraphFactory<RemainingApiModuleTestGraph.Factory>().create(core, account)
        assertEquals(0, appCreations)
        assertNotSame(graph.getApp, graph.getApp)
        assertEquals(2, appCreations)
        assertNotSame(graph.calls, graph.calls)
        assertNotSame(graph.verifyBackup, graph.verifyBackup)
        assertNotSame(graph.teamMember, graph.teamMember)
        assertNotSame(graph.services, graph.services)
        assertNotSame(graph.cancelConnection, graph.cancelConnection)
        assertSame(removeDrafts, graph.removeDrafts)
        assertSame(removeDrafts, graph.removeDrafts)
        assertSame(requestCode, graph.requestCode)
        assertSame(validateEmail, graph.validateEmail)
        // Old fresh facade reads and borrowed owner member reads are strict/unconfigured and would throw.
        verify(exactly = 0) { session.apps }
        verify(exactly = 0) { session.calls }
        verify(exactly = 0) { session.backup }
        verify(exactly = 0) { cellsOwner.removeAttachments }
        verify(exactly = 0) { authOwner.requestSecondFactorVerificationCode }
        verify(exactly = 0) { core.getGlobalScope() }
    }

    @Test
    fun givenSessionSelectionFactory_whenResolvingGlobalModuleProviders_thenEachRequestCreatesAnApi() {
        val core = mockk<CoreLogic>()
        var creations = 0
        every { core.sessionSelectionApiFactory } returns KaliumSessionSelectionApi.Factory {
            creations++
            mockk<KaliumSessionSelectionApi>().also { api ->
                every { api.currentSession } returns mockk()
                every { api.updateCurrentSession } returns mockk()
                every { api.allSessions } returns mockk()
                every { api.allSessionsFlow } returns mockk()
                every { api.currentSessionFlow } returns mockk()
            }
        }
        val module = CoreLogicModule()
        assertNotSame(module.provideCurrentSessionUseCase(core), module.provideCurrentSessionUseCase(core))
        module.provideUpdateCurrentSessionUseCase(core)
        module.provideGetAllSessionsUseCase(core)
        module.provideObserveAllSessionsUseCase(core)
        UseCaseModule().provideCurrentSessionFlowUseCase(core)
        assertEquals(6, creations)
        verify(exactly = 0) { core.getGlobalScope() }
    }

    @Test
    fun givenBorrowedCellsApis_whenCreatingAndroidPagingAdapters_thenWrappersUseTheirOwnersExistingPagingUseCase() {
        val first = mockk<KaliumCellsApi>()
        val second = mockk<KaliumCellsApi>()
        val firstPaging = mockk<GetCellConversationsPagedUseCase>()
        val secondPaging = mockk<GetCellConversationsPagedUseCase>()
        val firstFlow = flowOf(PagingData.empty<CellConversation>())
        val secondFlow = flowOf(PagingData.empty<CellConversation>())
        every { first.cellsGetCellConversationsPagedUseCase } returns firstPaging
        every { second.cellsGetCellConversationsPagedUseCase } returns secondPaging
        every { firstPaging("query") } returns firstFlow
        every { secondPaging("query") } returns secondFlow
        val firstWrapper = first.paginatedConversationsFlowUseCase
        assertNotSame(firstWrapper, first.paginatedConversationsFlowUseCase)
        assertSame(firstFlow, firstWrapper("query"))
        assertSame(secondFlow, second.paginatedConversationsFlowUseCase("query"))
        verify(exactly = 1) { firstPaging("query") }
        verify(exactly = 1) { secondPaging("query") }
        // This checks adapter ownership/forwarding; deferred PagingSource loads are not consumer-guarded.
    }
}

@DependencyGraph(
    bindingContainers = [
        AppsModule::class,
        AuthenticationModule::class,
        BackupModule::class,
        CallsModule::class,
        CellsModule::class,
        ConnectionModule::class,
        ServicesModule::class,
        TeamModule::class,
        UseCaseModule::class,
    ],
)
internal interface RemainingApiModuleTestGraph {
    val getApp: GetAppByIdUseCase
    val calls: ObserveEstablishedCallsUseCase
    val verifyBackup: VerifyBackupUseCase
    val teamMember: IsSelfATeamMemberUseCase
    val services: ObserveAllServicesUseCase
    val cancelConnection: CancelConnectionRequestUseCase
    val removeDrafts: RemoveAttachmentDraftsUseCase
    val requestCode: RequestSecondFactorVerificationCodeUseCase
    val validateEmail: ValidateEmailUseCase

    @DependencyGraph.Factory
    interface Factory {
        fun create(
            @Provides @KaliumCoreLogic core: CoreLogic,
            @Provides @CurrentAccount account: UserId,
        ): RemainingApiModuleTestGraph
    }
}
