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

package com.wire.android.ui

import android.content.Intent
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.di.ObserveIfE2EIRequiredDuringLoginUseCaseProvider
import com.wire.android.di.ObserveScreenshotCensoringConfigUseCaseProvider
import com.wire.android.di.ObserveSyncStateUseCaseProvider
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.SwitchAccountActions
import com.wire.android.feature.SwitchAccountParam
import com.wire.android.feature.SwitchAccountResult
import com.wire.android.services.ServicesManager
import com.wire.android.ui.authentication.devices.model.displayName
import com.wire.android.ui.common.ActionsViewModel
import com.wire.android.ui.common.dialogs.CustomServerDetailsDialogState
import com.wire.android.ui.common.dialogs.CustomServerDialogState
import com.wire.android.ui.common.dialogs.CustomServerNoNetworkDialogState
import com.wire.android.ui.joinConversation.JoinConversationViaCodeState
import com.wire.android.ui.theme.ThemeOption
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.deeplink.DeepLinkProcessor
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.deeplink.LoginType
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.UIText
import com.wire.android.workmanager.worker.cancelPeriodicPersistentWebsocketCheckWorker
import com.wire.android.workmanager.worker.enqueuePeriodicPersistentWebsocketCheckWorker
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.client.Client
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.appVersioning.ObserveIfAppUpdateRequiredUseCase
import com.wire.kalium.logic.feature.client.ClearNewClientsForUserUseCase
import com.wire.kalium.logic.feature.client.NewClientResult
import com.wire.kalium.logic.feature.client.ObserveNewClientsUseCase
import com.wire.kalium.logic.feature.conversation.CheckConversationInviteCodeUseCase
import com.wire.kalium.logic.feature.debug.SynchronizeExternalDataResult
import com.wire.kalium.logic.feature.server.GetServerConfigResult
import com.wire.kalium.logic.feature.server.GetServerConfigUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.DoesValidSessionExistResult
import com.wire.kalium.logic.feature.session.DoesValidSessionExistUseCase
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.ObserveSessionsUseCase
import com.wire.kalium.logic.feature.user.screenshotCensoring.ObserveScreenshotCensoringConfigResult
import com.wire.kalium.logic.feature.user.webSocketStatus.ObservePersistentWebSocketConnectionStatusUseCase
import com.wire.kalium.util.DateTimeUtil.toIsoDateTimeString
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.InputStreamReader
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WireActivityViewModel @Inject constructor(
    @KaliumCoreLogic private val coreLogic: Lazy<CoreLogic>,
    private val dispatchers: DispatcherProvider,
    currentSessionFlow: Lazy<CurrentSessionFlowUseCase>,
    private val doesValidSessionExist: Lazy<DoesValidSessionExistUseCase>,
    private val getServerConfigUseCase: Lazy<GetServerConfigUseCase>,
    private val deepLinkProcessor: Lazy<DeepLinkProcessor>,
    private val observeSessions: Lazy<ObserveSessionsUseCase>,
    private val accountSwitch: Lazy<AccountSwitchUseCase>,
    private val servicesManager: Lazy<ServicesManager>,
    private val observeSyncStateUseCaseProviderFactory: ObserveSyncStateUseCaseProvider.Factory,
    private val observeIfAppUpdateRequired: Lazy<ObserveIfAppUpdateRequiredUseCase>,
    private val observeNewClients: Lazy<ObserveNewClientsUseCase>,
    private val clearNewClientsForUser: Lazy<ClearNewClientsForUserUseCase>,
    private val currentScreenManager: Lazy<CurrentScreenManager>,
    private val observeScreenshotCensoringConfigUseCaseProviderFactory: ObserveScreenshotCensoringConfigUseCaseProvider.Factory,
    private val globalDataStore: Lazy<GlobalDataStore>,
    private val observeIfE2EIRequiredDuringLoginUseCaseProviderFactory: ObserveIfE2EIRequiredDuringLoginUseCaseProvider.Factory,
    private val workManager: Lazy<WorkManager>
) : ActionsViewModel<WireActivityViewAction>() {

    var globalAppState: GlobalAppState by mutableStateOf(GlobalAppState())
        private set

    private val _observeSyncFlowState: MutableStateFlow<SyncState?> = MutableStateFlow(null)
    val observeSyncFlowState: StateFlow<SyncState?> = _observeSyncFlowState

    private val observeCurrentAccountInfo: SharedFlow<AccountInfo?> = currentSessionFlow.get().invoke()
        .map { (it as? CurrentSessionResult.Success)?.accountInfo }
        .distinctUntilChanged()
        .flowOn(dispatchers.io())
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    private val observeCurrentValidUserId: SharedFlow<UserId?> = observeCurrentAccountInfo
        .map {
            if (it?.isValid() == true) it.userId else null
        }
        .distinctUntilChanged()
        .flowOn(dispatchers.io())
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    private lateinit var validSessions: StateFlow<List<AccountInfo>>

    init {
        observeSyncState()
        observeUpdateAppState()
        observeNewClientState()
        observeScreenshotCensoringConfigState()
        observeAppThemeState()
        observeLogoutState()
        resetNewRegistrationAnalyticsState()
    }

    private suspend fun shouldEnrollToE2ei(): Boolean = observeCurrentValidUserId.first()?.let {
        observeIfE2EIRequiredDuringLoginUseCaseProviderFactory.create(it)
            .observeIfE2EIIsRequiredDuringLogin().first() ?: false
    } ?: false

    private fun observeAppThemeState() {
        viewModelScope.launch(dispatchers.io()) {
            globalDataStore.get().selectedThemeOptionFlow()
                .distinctUntilChanged()
                .collect {
                    globalAppState = globalAppState.copy(themeOption = it)
                }
        }
    }

    private fun observeSyncState() {
        viewModelScope.launch(dispatchers.io()) {
            observeCurrentValidUserId
                .flatMapLatest { userId ->
                    userId?.let {
                        observeSyncStateUseCaseProviderFactory.create(userId).observeSyncState()
                    } ?: flowOf(null)
                }
                .distinctUntilChanged()
                .flowOn(dispatchers.io())
                .collect {
                    _observeSyncFlowState.emit(it)
                }
        }
    }

    private fun observeLogoutState() {
        viewModelScope.launch(dispatchers.io()) {
            observeCurrentAccountInfo
                .collect {
                    if (it is AccountInfo.Invalid) {
                        handleInvalidSession(it.logoutReason)
                    }
                }
        }
    }

    private fun observeUpdateAppState() {
        viewModelScope.launch(dispatchers.io()) {
            observeIfAppUpdateRequired.get().invoke(BuildConfig.VERSION_CODE)
                .distinctUntilChanged()
                .collect {
                    globalAppState = globalAppState.copy(updateAppDialog = it)
                }
        }
    }

    private fun observeNewClientState() {
        viewModelScope.launch(dispatchers.io()) {
            currentScreenManager.get().observeCurrentScreen(this)
                .flatMapLatest {
                    if (it.isGlobalDialogAllowed()) {
                        observeNewClients.get().invoke()
                    } else {
                        flowOf(NewClientResult.Empty)
                    }
                }
                .collect {
                    val newClientDialog = NewClientsData.fromUseCaseResul(it)
                    globalAppState = globalAppState.copy(newClientDialog = newClientDialog)
                }
        }
    }

    private fun observeScreenshotCensoringConfigState() {
        viewModelScope.launch(dispatchers.io()) {
            observeCurrentValidUserId
                .flatMapLatest { currentValidUserId ->
                    currentValidUserId?.let {
                        observeScreenshotCensoringConfigUseCaseProviderFactory.create(it)
                            .observeScreenshotCensoringConfig()
                            .map { result ->
                                result is ObserveScreenshotCensoringConfigResult.Enabled
                            }
                    } ?: flowOf(false)
                }
                .collect {
                    globalAppState = globalAppState.copy(screenshotCensoringEnabled = it)
                }
        }
    }

    private suspend fun validSessionsFlow() = observeSessions.get().invoke()
        .map { (it as? GetAllSessionsResult.Success)?.sessions ?: emptyList() }

    @VisibleForTesting
    internal suspend fun initValidSessionsFlowIfNeeded() {
        if (::validSessions.isInitialized.not()) { // initialise valid sessions flow if not already initialised
            validSessions = validSessionsFlow().stateIn(viewModelScope, SharingStarted.Eagerly, validSessionsFlow().first())
        }
    }

    suspend fun initialAppState(): InitialAppState = withContext(dispatchers.io()) {
        initValidSessionsFlowIfNeeded()
        when {
            shouldLogIn() -> InitialAppState.NOT_LOGGED_IN
            shouldEnrollToE2ei() -> InitialAppState.ENROLL_E2EI
            else -> InitialAppState.LOGGED_IN
        }
    }

    private suspend fun handleInvalidSession(logoutReason: LogoutReason) {
        withContext(dispatchers.main()) {
            when (logoutReason) {
                LogoutReason.SELF_SOFT_LOGOUT, LogoutReason.SELF_HARD_LOGOUT -> {
                    // Self logout is handled from the Self user profile screen directly
                }

                LogoutReason.MIGRATION_TO_CC_FAILED, LogoutReason.REMOVED_CLIENT ->
                    globalAppState =
                        globalAppState.copy(blockUserUI = CurrentSessionErrorState.RemovedClient)

                LogoutReason.DELETED_ACCOUNT ->
                    globalAppState =
                        globalAppState.copy(blockUserUI = CurrentSessionErrorState.DeletedAccount)

                LogoutReason.SESSION_EXPIRED ->
                    globalAppState =
                        globalAppState.copy(blockUserUI = CurrentSessionErrorState.SessionExpired)
            }
        }
    }

    fun handleSynchronizeExternalData(
        data: InputStream
    ) {
        viewModelScope.launch(dispatchers.io()) {
            when (val currentSession = coreLogic.get().getGlobalScope().session.currentSession()) {
                is CurrentSessionResult.Failure.Generic -> null
                CurrentSessionResult.Failure.SessionNotFound -> null
                is CurrentSessionResult.Success -> {
                    coreLogic.get().sessionScope(currentSession.accountInfo.userId) {
                        when (val result = debug.synchronizeExternalData(InputStreamReader(data).readText())) {
                            is SynchronizeExternalDataResult.Success -> {
                                appLogger.d("Synchronized external data")
                            }

                            is SynchronizeExternalDataResult.Failure -> {
                                appLogger.d("Failed to Synchronize external data: ${result.coreFailure}")
                            }
                        }
                    }
                }
            }
        }
    }

    @Suppress("ComplexMethod")
    fun handleDeepLink(intent: Intent?) {
        viewModelScope.launch(dispatchers.io()) {
            when (val result = deepLinkProcessor.get().invoke(intent?.data, intent?.action)) {
                DeepLinkResult.AuthorizationNeeded -> sendAction(OnAuthorizationNeeded)
                is DeepLinkResult.SSOLogin -> sendAction(OnSSOLogin(result))
                is DeepLinkResult.CustomServerConfig -> onCustomServerConfig(result.url, result.loginType)
                is DeepLinkResult.SwitchAccountFailure.OngoingCall -> sendAction(ShowToast(R.string.cant_switch_account_in_call))
                is DeepLinkResult.SwitchAccountFailure.Unknown -> appLogger.e("unknown deeplink failure")
                is DeepLinkResult.JoinConversation -> onConversationInviteDeepLink(
                    result.code,
                    result.key,
                    result.domain
                ) { conversationId ->
                    sendAction(OpenConversation(DeepLinkResult.OpenConversation(conversationId, result.switchedAccount)))
                }

                is DeepLinkResult.MigrationLogin -> sendAction(OnMigrationLogin(result))
                is DeepLinkResult.OpenConversation -> sendAction(OpenConversation(result))
                is DeepLinkResult.OpenOtherUserProfile -> sendAction(OnOpenUserProfile(result))

                DeepLinkResult.SharingIntent -> sendAction(OnShowImportMediaScreen)
                DeepLinkResult.Unknown -> {
                    sendAction(OnUnknownDeepLink)
                    appLogger.e("unknown deeplink result $result")
                }
            }
        }
    }

    fun dismissCustomBackendDialog() {
        globalAppState = globalAppState.copy(customBackendDialog = null)
    }

    fun customBackendDialogProceedButtonClicked(onProceed: (ServerConfig.Links) -> Unit) {
        val backendDialogState = globalAppState.customBackendDialog
        if (backendDialogState is CustomServerDetailsDialogState) {
            dismissCustomBackendDialog()
            if (checkNumberOfSessions()) {
                onProceed(backendDialogState.serverLinks)
            }
        }
    }

    // TODO: needs to be covered with test once hard logout is validated to be used
    fun doHardLogout(
        clearUserData: (userId: UserId) -> Unit,
        switchAccountActions: SwitchAccountActions
    ) {
        viewModelScope.launch {
            coreLogic.get().getGlobalScope().session.currentSession().takeIf {
                it is CurrentSessionResult.Success
            }?.let {
                val currentUserId = (it as CurrentSessionResult.Success).accountInfo.userId
                coreLogic.get().getSessionScope(currentUserId).logout(LogoutReason.SELF_HARD_LOGOUT)
                clearUserData(currentUserId)
            }
            accountSwitch.get().invoke(SwitchAccountParam.TryToSwitchToNextAccount).also {
                if (it == SwitchAccountResult.NoOtherAccountToSwitch) {
                    globalDataStore.get().clearAppLockPasscode()
                }
            }.callAction(switchAccountActions)
        }
    }

    fun dismissNewClientsDialog(userId: UserId) {
        globalAppState = globalAppState.copy(newClientDialog = null)
        viewModelScope.launch {
            doesValidSessionExist.get().invoke(userId).let {
                if (it is DoesValidSessionExistResult.Success && it.doesValidSessionExist) {
                    clearNewClientsForUser.get().invoke(userId)
                }
            }
        }
    }

    fun switchAccount(userId: UserId, actions: SwitchAccountActions, onComplete: () -> Unit) {
        viewModelScope.launch {
            accountSwitch.get().invoke(SwitchAccountParam.SwitchToAccount(userId))
                .callAction(actions)
            onComplete()
        }
    }

    fun tryToSwitchAccount(actions: SwitchAccountActions) {
        viewModelScope.launch {
            globalAppState = globalAppState.copy(blockUserUI = null)
            accountSwitch.get().invoke(SwitchAccountParam.TryToSwitchToNextAccount)
                .callAction(actions)
        }
    }

    /*
     * This function is used to check if the number of accounts is less than the maximum allowed accounts.
     * If the number of accounts is greater than or equal to the maximum allowed accounts, then the max account dialog is shown.
     * @return true if the number of accounts is less than the maximum allowed accounts, false otherwise.
     */
    fun checkNumberOfSessions(): Boolean {
        val reachedMax = validSessions.value.size >= BuildConfig.MAX_ACCOUNTS
        if (reachedMax) {
            globalAppState = globalAppState.copy(maxAccountDialog = true)
        }
        return !reachedMax
    }

    private suspend fun loadServerConfig(url: String): ServerConfig.Links? =
        when (val result = getServerConfigUseCase.get().invoke(url)) {
            is GetServerConfigResult.Success -> result.serverConfigLinks
            is GetServerConfigResult.Failure.Generic -> {
                appLogger.e("something went wrong during handling the custom server deep link: ${result.genericFailure}")
                null
            }
        }

    fun onCustomServerConfig(customServerUrl: String, loginType: LoginType) {
        viewModelScope.launch(dispatchers.io()) {
            val customBackendDialogData = loadServerConfig(customServerUrl)
                ?.let { serverLinks -> CustomServerDetailsDialogState(serverLinks = serverLinks, loginType = loginType) }
                ?: CustomServerNoNetworkDialogState(customServerUrl = customServerUrl, loginType = loginType)

            globalAppState = globalAppState.copy(
                customBackendDialog = customBackendDialogData
            )
        }
    }

    private suspend fun onConversationInviteDeepLink(
        code: String,
        key: String,
        domain: String?,
        onSuccess: (ConversationId) -> Unit
    ) = when (val currentSession = coreLogic.get().getGlobalScope().session.currentSession()) {
        is CurrentSessionResult.Failure.Generic -> null
        CurrentSessionResult.Failure.SessionNotFound -> null
        is CurrentSessionResult.Success -> {
            coreLogic.get().sessionScope(currentSession.accountInfo.userId) {
                when (val result = conversations.checkIConversationInviteCode(code, key, domain)) {
                    is CheckConversationInviteCodeUseCase.Result.Success -> {
                        if (result.isSelfMember) {
                            // TODO; display messsage that user is already a member and ask if they want to navigate to the conversation
                            appLogger.d("user is already a member of the conversation")
                            onSuccess(result.conversationId)
                        } else {
                            globalAppState =
                                globalAppState.copy(
                                    conversationJoinedDialog = JoinConversationViaCodeState.Show(
                                        result.name,
                                        code,
                                        key,
                                        domain,
                                        result.isPasswordProtected
                                    )
                                )
                        }
                    }

                    is CheckConversationInviteCodeUseCase.Result.Failure -> globalAppState =
                        globalAppState.copy(
                            conversationJoinedDialog = JoinConversationViaCodeState.Error(result)
                        )
                }
            }
        }
    }

    fun onJoinConversationFlowCompleted() {
        globalAppState = globalAppState.copy(conversationJoinedDialog = null)
    }

    private suspend fun shouldLogIn(): Boolean = observeCurrentValidUserId.first() == null

    fun dismissMaxAccountDialog() {
        globalAppState = globalAppState.copy(maxAccountDialog = false)
    }

    fun observePersistentConnectionStatus() {
        viewModelScope.launch {
            coreLogic.get().getGlobalScope().observePersistentWebSocketConnectionStatus()
                .let { result ->
                    when (result) {
                        is ObservePersistentWebSocketConnectionStatusUseCase.Result.Failure -> {
                            appLogger.e("Failure while fetching persistent web socket status flow from wire activity")
                        }

                        is ObservePersistentWebSocketConnectionStatusUseCase.Result.Success -> {
                            result.persistentWebSocketStatusListFlow.collect { statuses ->

                                if (statuses.any { it.isPersistentWebSocketEnabled }) {
                                    if (!servicesManager.get()
                                            .isPersistentWebSocketServiceRunning()
                                    ) {
                                        servicesManager.get().startPersistentWebSocketService()
                                        workManager.get()
                                            .enqueuePeriodicPersistentWebsocketCheckWorker()
                                    }
                                } else {
                                    servicesManager.get().stopPersistentWebSocketService()
                                    workManager.get().cancelPeriodicPersistentWebsocketCheckWorker()
                                }
                            }
                        }
                    }
                }
        }
    }

    /**
     * Reset any unfinished registration process analytics where the user aborted and enabled the registration analytics.
     */
    private fun resetNewRegistrationAnalyticsState() = viewModelScope.launch {
        globalDataStore.get().setAnonymousRegistrationEnabled(false)
    }

    private fun CurrentScreen.isGlobalDialogAllowed(): Boolean = when (this) {
        is CurrentScreen.ImportMedia,
        is CurrentScreen.DeviceManager -> false

        is CurrentScreen.InBackground,
        is CurrentScreen.Conversation,
        is CurrentScreen.Home,
        is CurrentScreen.OtherUserProfile,
        is CurrentScreen.AuthRelated,
        is CurrentScreen.SomeOther -> true
    }
}

sealed class CurrentSessionErrorState {
    data object RemovedClient : CurrentSessionErrorState()
    data object DeletedAccount : CurrentSessionErrorState()
    data object SessionExpired : CurrentSessionErrorState()
}

sealed class NewClientsData(open val clientsInfo: List<NewClientInfo>, open val userId: UserId) {
    data class CurrentUser(
        override val clientsInfo: List<NewClientInfo>,
        override val userId: UserId
    ) : NewClientsData(clientsInfo, userId)

    data class OtherUser(
        override val clientsInfo: List<NewClientInfo>,
        override val userId: UserId,
        val userName: String?,
        val userHandle: String?
    ) : NewClientsData(clientsInfo, userId)

    companion object {
        fun fromUseCaseResul(result: NewClientResult): NewClientsData? = when (result) {
            is NewClientResult.InCurrentAccount -> {
                CurrentUser(
                    result.newClients.map(NewClientInfo::fromClient),
                    result.userId
                )
            }

            is NewClientResult.InOtherAccount -> {
                OtherUser(
                    result.newClients.map(NewClientInfo::fromClient),
                    result.userId,
                    result.userName,
                    result.userHandle
                )
            }

            else -> null
        }
    }
}

data class NewClientInfo(val date: String, val deviceInfo: UIText) {
    companion object {
        fun fromClient(client: Client): NewClientInfo =
            NewClientInfo(
                client.registrationTime?.toIsoDateTimeString() ?: "",
                client.displayName()
            )
    }
}

data class GlobalAppState(
    val customBackendDialog: CustomServerDialogState? = null,
    val maxAccountDialog: Boolean = false,
    val blockUserUI: CurrentSessionErrorState? = null,
    val updateAppDialog: Boolean = false,
    val conversationJoinedDialog: JoinConversationViaCodeState? = null,
    val newClientDialog: NewClientsData? = null,
    val screenshotCensoringEnabled: Boolean = true,
    val themeOption: ThemeOption = ThemeOption.SYSTEM
)

enum class InitialAppState {
    NOT_LOGGED_IN, LOGGED_IN, ENROLL_E2EI
}

sealed interface WireActivityViewAction
internal data class OpenConversation(val result: DeepLinkResult.OpenConversation) : WireActivityViewAction
internal data object OnShowImportMediaScreen : WireActivityViewAction
internal data object OnAuthorizationNeeded : WireActivityViewAction
internal data object OnUnknownDeepLink : WireActivityViewAction
internal data class OnMigrationLogin(val result: DeepLinkResult.MigrationLogin) : WireActivityViewAction
internal data class OnOpenUserProfile(val result: DeepLinkResult.OpenOtherUserProfile) : WireActivityViewAction
internal data class OnSSOLogin(val result: DeepLinkResult.SSOLogin) : WireActivityViewAction
internal data class ShowToast(val messageResId: Int) : WireActivityViewAction
