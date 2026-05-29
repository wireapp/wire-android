@file:Suppress("TooManyFunctions")

package com.wire.wireone

import androidx.compose.runtime.Composable
import com.wire.kalium.common.logger.kaliumLogger
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.call.CallType
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.message.getType
import com.wire.kalium.logic.data.session.StoreSessionParam
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthenticationResult
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.client.RegisterClientParam
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.message.MessageOperationResult
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class ConversationListItemUiState(
    val id: ConversationId,
    val title: String,
    val subtitle: String,
    val protocolLabel: String,
    val hasOngoingCall: Boolean,
)

internal data class MessageItemUiState(
    val id: String,
    val senderLabel: String,
    val body: String,
    val dateLabel: String,
    val isOwnMessage: Boolean,
)

internal data class KaliumUiState(
    val runtimeLine: String,
    val sessionLine: String,
    val syncLine: String,
    val syncState: SyncState = SyncState.Waiting,
    val hasPassedSlowSync: Boolean = false,
    val isLoggingIn: Boolean = false,
    val errorLine: String? = null,
    val activeUserId: UserId? = null,
    val conversations: List<ConversationListItemUiState> = emptyList(),
    val selectedConversationId: ConversationId? = null,
    val selectedConversationTitle: String? = null,
    val messages: List<MessageItemUiState> = emptyList(),
    val incomingCallConversationIds: Set<ConversationId> = emptySet(),
    val ongoingCallConversationIds: Set<ConversationId> = emptySet(),
)

internal interface KaliumProvider {
    val state: StateFlow<KaliumUiState>

    suspend fun login(userIdentifier: String, password: String)
    fun selectConversation(conversationId: ConversationId)
    suspend fun sendMessage(text: String): Boolean
    suspend fun joinCall(): Boolean
    suspend fun answerCall(): Boolean
    suspend fun rejectCall(): Boolean
    suspend fun endCall(): Boolean
}

@Composable
internal expect fun rememberKaliumProvider(): KaliumProvider

internal open class CoreLogicKaliumProvider(
    private val coreLogicFactory: () -> CoreLogic,
    internal val readyLine: String,
    private val serverLinks: ServerConfig.Links = ServerConfig.PRODUCTION,
) : KaliumProvider {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var syncObservationJob: Job? = null
    private var syncForegroundJob: Job? = null
    private var conversationListJob: Job? = null
    private var messagesObservationJob: Job? = null
    private var incomingCallsObservationJob: Job? = null
    private var ongoingCallsObservationJob: Job? = null
    internal val coreLogic: CoreLogic by lazy(coreLogicFactory)

    internal val mutableState = MutableStateFlow(
        KaliumUiState(
            runtimeLine = "Kalium: bootstrapping",
            sessionLine = "Session: none",
            syncLine = "Sync: idle"
        )
    )
    override val state: StateFlow<KaliumUiState> = mutableState.asStateFlow()

    init {
        scope.launch {
            awaitRuntimeReady(coreLogic)
            mutableState.update { it.copy(runtimeLine = readyLine) }
            observeCurrentSession()
        }
    }

    override fun selectConversation(conversationId: ConversationId) {
        val activeUserId = mutableState.value.activeUserId ?: return
        kaliumLogger.i("WireOne selectConversation: user=${activeUserId.value} conversation=${conversationId.value}")
        mutableState.update { state ->
            state.copy(
                selectedConversationId = conversationId,
                selectedConversationTitle = state.conversations.firstOrNull { it.id == conversationId }?.title,
                messages = emptyList()
            )
        }
        observeMessages(activeUserId, conversationId)
    }

    override suspend fun sendMessage(text: String): Boolean {
        val messageText = text.trim()
        val state = mutableState.value
        val sendContext = createSendContext(messageText, state) ?: return false
        kaliumLogger.i(
            "WireOne sendMessage start: conversation=${sendContext.conversationId.value} draftLength=${messageText.length} " +
                "visibleMessages=${sendContext.visibleMessages}"
        )

        return when (
            val result = coreLogic.getSessionScope(sendContext.userId).messages.sendTextMessage(
                sendContext.conversationId,
                messageText
            )
        ) {
            MessageOperationResult.Success -> {
                kaliumLogger.i(
                    "WireOne sendMessage success: conversation=${sendContext.conversationId.value} draftLength=${messageText.length}"
                )
                mutableState.update {
                    it.copy(
                        runtimeLine = "Kalium: message sent",
                        errorLine = null
                    )
                }
                true
            }

            is MessageOperationResult.Failure -> {
                kaliumLogger.e("WireOne sendMessage failed for ${sendContext.conversationId.value}: ${result.error}")
                mutableState.update {
                    it.copy(errorLine = "Send failed: ${result.error}")
                }
                false
            }
        }
    }

    override suspend fun joinCall(): Boolean {
        val state = mutableState.value
        val activeUserId = state.activeUserId
        val conversationId = state.selectedConversationId
        return if (activeUserId == null || conversationId == null) {
            false
        } else if (!requestMicrophonePermissionIfNeeded()) {
            mutableState.update { it.copy(errorLine = "Microphone permission denied.") }
            false
        } else {
            runCatching {
                coreLogic.getSessionScope(activeUserId).calls.startCall(conversationId = conversationId, callType = CallType.AUDIO)
                mutableState.update { it.copy(runtimeLine = "Kalium: joined call", errorLine = null) }
                true
            }.getOrElse { error ->
                mutableState.update { it.copy(errorLine = "Join call failed: ${error.message ?: error::class.simpleName}") }
                false
            }
        }
    }

    override suspend fun answerCall(): Boolean {
        val state = mutableState.value
        val activeUserId = state.activeUserId
        val conversationId = state.selectedConversationId
        return if (activeUserId == null || conversationId == null) {
            false
        } else if (!requestMicrophonePermissionIfNeeded()) {
            mutableState.update { it.copy(errorLine = "Microphone permission denied.") }
            false
        } else {
            runCatching {
                coreLogic.getSessionScope(activeUserId).calls.answerCall(conversationId)
                mutableState.update { it.copy(runtimeLine = "Kalium: answered call", errorLine = null) }
                true
            }.getOrElse { error ->
                mutableState.update { it.copy(errorLine = "Answer call failed: ${error.message ?: error::class.simpleName}") }
                false
            }
        }
    }

    override suspend fun rejectCall(): Boolean {
        val state = mutableState.value
        val activeUserId = state.activeUserId
        val conversationId = state.selectedConversationId
        return if (activeUserId == null || conversationId == null) {
            false
        } else {
            runCatching {
                coreLogic.getSessionScope(activeUserId).calls.rejectCall(conversationId)
                mutableState.update { it.copy(runtimeLine = "Kalium: rejected call", errorLine = null) }
                true
            }.getOrElse { error ->
                mutableState.update { it.copy(errorLine = "Reject call failed: ${error.message ?: error::class.simpleName}") }
                false
            }
        }
    }

    override suspend fun endCall(): Boolean {
        val state = mutableState.value
        val activeUserId = state.activeUserId
        val conversationId = state.selectedConversationId
        return if (activeUserId == null || conversationId == null) {
            false
        } else {
            runCatching {
                coreLogic.getSessionScope(activeUserId).calls.endCall(conversationId)
                mutableState.update { it.copy(runtimeLine = "Kalium: ended call", errorLine = null) }
                true
            }.getOrElse { error ->
                mutableState.update { it.copy(errorLine = "End call failed: ${error.message ?: error::class.simpleName}") }
                false
            }
        }
    }

    override suspend fun login(userIdentifier: String, password: String) {
        awaitRuntimeReady(coreLogic)
        val credentials = sanitizeCredentials(userIdentifier, password)
        if (credentials == null) {
            mutableState.update { it.copy(errorLine = "Enter login and password.") }
            return
        }

        markLoginStarted()

        when (val authScopeResult = coreLogic.versionedAuthenticationScope(serverLinks).invoke(null)) {
            is AutoVersionAuthScopeUseCase.Result.Failure -> handleAuthScopeFailure(authScopeResult)
            is AutoVersionAuthScopeUseCase.Result.Success -> handleAuthScopeSuccess(authScopeResult, credentials)
        }
    }

    internal suspend fun storeSessionAndStartSync(
        loginResult: AuthenticationResult.Success,
        initialServerConfig: ServerConfig,
        userIdentifier: String,
        password: String,
    ) {
        val storeResult = addAuthenticatedAccount(loginResult)
        when (storeResult) {
            is AddAuthenticatedUserUseCase.Result.Failure -> handleStoredSessionFailure(storeResult)
            is AddAuthenticatedUserUseCase.Result.Success -> handleStoredSessionSuccess(
                userId = storeResult.userId,
                userIdentifier = userIdentifier,
                password = password,
                initialServerConfig = initialServerConfig,
            )
        }
    }

    private suspend fun observeCurrentSession() {
        coreLogic.getGlobalScope().session.currentSessionFlow().collectLatest { currentSessionResult ->
            when (currentSessionResult) {
                is CurrentSessionResult.Failure -> {
                    syncObservationJob?.cancel()
                    syncForegroundJob?.cancel()
                    conversationListJob?.cancel()
                    messagesObservationJob?.cancel()
                    incomingCallsObservationJob?.cancel()
                    ongoingCallsObservationJob?.cancel()
                    mutableState.update {
                        it.copy(
                            sessionLine = "Session: none",
                            syncLine = "Sync: idle",
                            syncState = SyncState.Waiting,
                            hasPassedSlowSync = false,
                            activeUserId = null,
                            conversations = emptyList(),
                            selectedConversationId = null,
                            selectedConversationTitle = null,
                            messages = emptyList(),
                            incomingCallConversationIds = emptySet(),
                            ongoingCallConversationIds = emptySet(),
                        )
                    }
                }

                is CurrentSessionResult.Success -> {
                    when (val accountInfo = currentSessionResult.accountInfo) {
                        is AccountInfo.Invalid -> {
                            syncObservationJob?.cancel()
                            syncForegroundJob?.cancel()
                            conversationListJob?.cancel()
                            messagesObservationJob?.cancel()
                            incomingCallsObservationJob?.cancel()
                            ongoingCallsObservationJob?.cancel()
                            mutableState.update {
                                it.copy(
                                    sessionLine = "Session: invalid (${accountInfo.userId.value})",
                                    syncLine = "Sync: idle",
                                    syncState = SyncState.Waiting,
                                    hasPassedSlowSync = false,
                                    activeUserId = null,
                                    conversations = emptyList(),
                                    selectedConversationId = null,
                                    selectedConversationTitle = null,
                                    messages = emptyList(),
                                    incomingCallConversationIds = emptySet(),
                                    ongoingCallConversationIds = emptySet(),
                                )
                            }
                        }

                        is AccountInfo.Valid -> {
                            observeSyncForUser(accountInfo.userId)
                        }
                    }
                }
            }
        }
    }

    private fun observeSyncForUser(userId: UserId) {
        syncObservationJob?.cancel()
        syncForegroundJob?.cancel()
        mutableState.update {
            it.copy(
                sessionLine = "Session: ${userId.value}",
                activeUserId = userId,
                syncState = SyncState.Waiting,
                hasPassedSlowSync = false
            )
        }
        syncForegroundJob = scope.launch {
            kaliumLogger.i("WireOne sync: starting foreground sync request for ${userId.value}")
            coreLogic.getSessionScope(userId).syncExecutor.request {
                awaitCancellation()
            }
        }
        syncObservationJob = scope.launch {
            coreLogic.getSessionScope(userId).observeSyncState().collectLatest { syncState ->
                mutableState.update {
                    it.copy(
                        syncLine = syncState.toUiLine(),
                        syncState = syncState,
                        hasPassedSlowSync = syncState !is SyncState.Waiting && syncState !is SyncState.SlowSync
                    )
                }
            }
        }
        observeConversations(userId)
        observeIncomingCalls(userId)
        observeOngoingCalls(userId)
    }

    private fun observeConversations(userId: UserId) {
        conversationListJob?.cancel()
        messagesObservationJob?.cancel()
        conversationListJob = scope.launch {
            coreLogic.getSessionScope(userId)
                .conversations
                .observeConversationListDetails(fromArchive = false)
                .collectLatest { conversationDetails ->
                    val conversations = conversationDetails
                        .map { it.toConversationListItem() }
                    val selectedConversationId = mutableState.value.selectedConversationId
                        ?.takeIf { selectedId -> conversations.any { it.id == selectedId } }
                        ?: conversations.firstOrNull()?.id
                    mutableState.update {
                        it.copy(
                            conversations = conversations,
                            selectedConversationId = selectedConversationId,
                            selectedConversationTitle = conversations.firstOrNull { item -> item.id == selectedConversationId }?.title
                        )
                    }
                    if (selectedConversationId != null) {
                        observeMessages(userId, selectedConversationId)
                    } else {
                        messagesObservationJob?.cancel()
                        mutableState.update { it.copy(messages = emptyList()) }
                    }
                }
        }
    }

    private fun observeIncomingCalls(userId: UserId) {
        incomingCallsObservationJob?.cancel()
        incomingCallsObservationJob = scope.launch {
            coreLogic.getSessionScope(userId)
                .calls
                .getIncomingCalls()
                .collectLatest { incomingCalls ->
                    mutableState.update {
                        it.copy(incomingCallConversationIds = incomingCalls.map { call -> call.conversationId }.toSet())
                    }
                }
        }
    }

    private fun observeOngoingCalls(userId: UserId) {
        ongoingCallsObservationJob?.cancel()
        ongoingCallsObservationJob = scope.launch {
            coreLogic.getSessionScope(userId)
                .calls
                .observeOngoingCalls()
                .collectLatest { ongoingCalls ->
                    mutableState.update {
                        it.copy(ongoingCallConversationIds = ongoingCalls.map { call -> call.conversationId }.toSet())
                    }
                }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun observeMessages(userId: UserId, conversationId: ConversationId) {
        messagesObservationJob?.cancel()
        messagesObservationJob = scope.launch {
            kaliumLogger.i("WireOne observeMessages start: user=${userId.value} conversation=${conversationId.value}")
            try {
                coreLogic.getSessionScope(userId)
                    .messages
                    .getRecentMessages(conversationId = conversationId, limit = 100)
                    .collectLatest { messages ->
                        val mappedMessages = messages
                            .asReversed()
                            .mapNotNull { message ->
                                message.toMessageItemOrNull(userId, conversationId)
                            }
                        mutableState.update {
                            it.copy(messages = mappedMessages)
                        }
                    }
            } catch (cancellation: CancellationException) {
                kaliumLogger.i("WireOne observeMessages cancelled: conversation=${conversationId.value}")
                throw cancellation
            } catch (exception: Exception) {
                handleObserveMessagesFailure(conversationId, exception)
            }
        }
    }

    internal open fun sessionScopeForLogin(
        userId: UserId,
        initialServerConfig: ServerConfig,
    ) = coreLogic.getSessionScope(userId)

    @Suppress("TooGenericExceptionCaught")
    internal open suspend fun persistEmailIfNeeded(userId: UserId, userIdentifier: String, initialServerConfig: ServerConfig) {
        if (!userIdentifier.contains("@")) return
        try {
            sessionScopeForLogin(userId, initialServerConfig).users.persistSelfUserEmail(userIdentifier)
        } catch (exception: Exception) {
            kaliumLogger.e(
                "WireOne login: persistEmailIfNeeded failed for ${userId.value} " +
                    "with ${exception::class.simpleName}: ${exception.message}"
            )
        }
    }

    protected open suspend fun awaitRuntimeReady(coreLogic: CoreLogic) {
        coreLogic
    }
}

private data class LoginCredentials(
    val userIdentifier: String,
    val password: String,
)

private data class SendContext(
    val userId: UserId,
    val conversationId: ConversationId,
    val visibleMessages: Int,
)

private fun CoreLogicKaliumProvider.createSendContext(
    messageText: String,
    state: KaliumUiState,
): SendContext? {
    val activeUserId = state.activeUserId
    val conversationId = state.selectedConversationId
    return if (messageText.isEmpty() || activeUserId == null || conversationId == null) {
        null
    } else {
        SendContext(
            userId = activeUserId,
            conversationId = conversationId,
            visibleMessages = state.messages.size,
        )
    }
}

private fun sanitizeCredentials(userIdentifier: String, password: String): LoginCredentials? {
    val cleanUserIdentifier = userIdentifier.trim()
    val cleanPassword = password.trim()
    return if (cleanUserIdentifier.isEmpty() || cleanPassword.isEmpty()) {
        null
    } else {
        LoginCredentials(cleanUserIdentifier, cleanPassword)
    }
}

private fun CoreLogicKaliumProvider.markLoginStarted() {
    mutableState.update {
        it.copy(
            isLoggingIn = true,
            errorLine = null,
            runtimeLine = "Kalium: logging in",
            syncLine = "Sync: preparing session"
        )
    }
}

private fun CoreLogicKaliumProvider.handleAuthScopeFailure(
    authScopeResult: AutoVersionAuthScopeUseCase.Result.Failure,
) {
    mutableState.update {
        it.copy(
            isLoggingIn = false,
            runtimeLine = readyLine,
            errorLine = authScopeResult.toErrorLine()
        )
    }
}

private suspend fun CoreLogicKaliumProvider.handleAuthScopeSuccess(
    authScopeResult: AutoVersionAuthScopeUseCase.Result.Success,
    credentials: LoginCredentials,
) {
    val loginResult = loginWithScope(authScopeResult, credentials) ?: return
    when (loginResult) {
        is AuthenticationResult.Failure -> handleAuthenticationFailure(loginResult)
        is AuthenticationResult.Success -> handleAuthenticationSuccess(authScopeResult, loginResult, credentials)
    }
}

private suspend fun CoreLogicKaliumProvider.loginWithScope(
    authScopeResult: AutoVersionAuthScopeUseCase.Result.Success,
    credentials: LoginCredentials,
): AuthenticationResult? = runCatching {
    authScopeResult.authenticationScope.login(
        userIdentifier = credentials.userIdentifier,
        password = credentials.password,
        shouldPersistClient = true,
    )
}.onFailure { exception ->
    kaliumLogger.e(
        "WireOne login: authenticationScope.login crashed with ${exception::class.simpleName}: ${exception.message}",
        exception
    )
    mutableState.update {
        it.copy(
            isLoggingIn = false,
            runtimeLine = readyLine,
            errorLine = "Login crash: ${exception::class.simpleName}: ${exception.message}"
        )
    }
}.getOrNull()

private fun CoreLogicKaliumProvider.handleAuthenticationFailure(loginResult: AuthenticationResult.Failure) {
    mutableState.update {
        it.copy(
            isLoggingIn = false,
            runtimeLine = readyLine,
            errorLine = loginResult.toErrorLine()
        )
    }
}

private suspend fun CoreLogicKaliumProvider.handleAuthenticationSuccess(
    authScopeResult: AutoVersionAuthScopeUseCase.Result.Success,
    loginResult: AuthenticationResult.Success,
    credentials: LoginCredentials,
) {
    runCatching {
        storeSessionAndStartSync(
            loginResult = loginResult,
            initialServerConfig = authScopeResult.authenticationScope.currentServerConfig(),
            userIdentifier = credentials.userIdentifier,
            password = credentials.password
        )
    }.onFailure { exception ->
        kaliumLogger.e(
            "WireOne login: storeSessionAndStartSync crashed with ${exception::class.simpleName}: ${exception.message}",
            exception
        )
        mutableState.update {
            it.copy(
                isLoggingIn = false,
                runtimeLine = readyLine,
                errorLine = "Post-login crash: ${exception::class.simpleName}: ${exception.message}"
            )
        }
    }
}

private suspend fun CoreLogicKaliumProvider.addAuthenticatedAccount(
    loginResult: AuthenticationResult.Success,
): AddAuthenticatedUserUseCase.Result =
    coreLogic.getGlobalScope().addAuthenticatedAccount(
        session = StoreSessionParam(
            serverConfigId = loginResult.serverConfigId,
            ssoId = loginResult.ssoID,
            accountTokens = loginResult.authData,
            proxyCredentials = loginResult.proxyCredentials,
            isPersistentWebSocketEnabled = true,
            managedBy = loginResult.managedBy,
        ),
        replace = true,
    )

private fun CoreLogicKaliumProvider.handleStoredSessionFailure(
    storeResult: AddAuthenticatedUserUseCase.Result.Failure,
) {
    mutableState.update {
        it.copy(
            isLoggingIn = false,
            runtimeLine = readyLine,
            errorLine = storeResult.toErrorLine()
        )
    }
}

private suspend fun CoreLogicKaliumProvider.handleStoredSessionSuccess(
    userId: UserId,
    userIdentifier: String,
    password: String,
    initialServerConfig: ServerConfig,
) {
    persistEmailIfNeeded(userId, userIdentifier, initialServerConfig)
    val sessionScope = sessionScopeForLogin(userId, initialServerConfig)
    when (
        val registerResult = sessionScope.client.getOrRegister(
            RegisterClientParam(
                password = password,
                capabilities = null,
            )
        )
    ) {
        is RegisterClientResult.Failure -> handleRegisterClientFailure(userId, registerResult)
        is RegisterClientResult.Success,
        is RegisterClientResult.E2EICertificateRequired -> handleRegisterClientSuccess(userId)
    }
}

private fun CoreLogicKaliumProvider.handleRegisterClientFailure(
    userId: UserId,
    registerResult: RegisterClientResult.Failure,
) {
    mutableState.update {
        it.copy(
            isLoggingIn = false,
            runtimeLine = readyLine,
            sessionLine = "Session: ${userId.value}",
            errorLine = registerResult.toErrorLine()
        )
    }
}

private fun CoreLogicKaliumProvider.handleRegisterClientSuccess(userId: UserId) {
    mutableState.update {
        it.copy(
            isLoggingIn = false,
            runtimeLine = readyLine,
            sessionLine = "Session: ${userId.value}",
            syncLine = "Sync: session ready, waiting for state",
            errorLine = null
        )
    }
}

private fun CoreLogicKaliumProvider.handleObserveMessagesFailure(
    conversationId: ConversationId,
    exception: Exception,
) {
    kaliumLogger.e(
        "WireOne observeMessages crashed for conversation=${conversationId.value}: " +
            "${exception::class.simpleName}: ${exception.message}",
        exception
    )
    mutableState.update {
        it.copy(errorLine = "Messages crash: ${exception::class.simpleName}: ${exception.message}")
    }
}

private fun Message.toMessageItemOrNull(
    selfUserId: UserId,
    conversationId: ConversationId,
): MessageItemUiState? = runCatching {
    toMessageItem(selfUserId)
}.onFailure { exception ->
    kaliumLogger.e(
        "WireOne observeMessages map failed for conversation=${conversationId.value} " +
            "messageId=$id type=${content::class.simpleName}: ${exception::class.simpleName}: ${exception.message}",
        exception
    )
}.getOrNull()

private fun AutoVersionAuthScopeUseCase.Result.Failure.toErrorLine(): String = when (this) {
    AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion -> "Server requires a newer client version."
    AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion -> "Unsupported server version."
    is AutoVersionAuthScopeUseCase.Result.Failure.Generic -> "Auth scope error: $genericFailure"
}

private fun AuthenticationResult.Failure.toErrorLine(): String = when (this) {
    AuthenticationResult.Failure.AccountPendingActivation -> "Account pending activation."
    AuthenticationResult.Failure.AccountSuspended -> "Account suspended."
    AuthenticationResult.Failure.InvalidUserIdentifier -> "Invalid email or handle."
    AuthenticationResult.Failure.SocketError -> "Socket/proxy error during login."
    AuthenticationResult.Failure.InvalidCredentials.Invalid2FA -> "Invalid or expired 2FA code."
    AuthenticationResult.Failure.InvalidCredentials.InvalidPasswordIdentityCombination -> "Invalid login or password."
    AuthenticationResult.Failure.InvalidCredentials.Missing2FA -> "2FA is required for this account."
    is AuthenticationResult.Failure.Generic -> "Login failed: $genericFailure"
}

private fun AddAuthenticatedUserUseCase.Result.Failure.toErrorLine(): String = when (this) {
    AddAuthenticatedUserUseCase.Result.Failure.NomadSingleUserViolation -> "Nomad single-user restriction blocked session storage."
    AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists -> "User already exists with incompatible session."
    is AddAuthenticatedUserUseCase.Result.Failure.Generic -> "Session store failed: $genericFailure"
}

private fun RegisterClientResult.Failure.toErrorLine(): String = when (this) {
    RegisterClientResult.Failure.PasswordAuthRequired -> "Client registration requires password auth."
    RegisterClientResult.Failure.TooManyClients -> "Too many registered clients for this account."
    RegisterClientResult.Failure.InvalidCredentials.Invalid2FA -> "Invalid or expired 2FA code for client registration."
    RegisterClientResult.Failure.InvalidCredentials.InvalidPassword -> "Invalid password for client registration."
    RegisterClientResult.Failure.InvalidCredentials.Missing2FA -> "2FA is required for client registration."
    is RegisterClientResult.Failure.Generic -> "Client registration failed: $genericFailure"
}

private fun SyncState.toUiLine(): String = when (this) {
    SyncState.Waiting -> "Sync: waiting to start"
    SyncState.SlowSync -> "Sync: slow sync in progress"
    SyncState.GatheringPendingEvents -> "Sync: incremental catch-up in progress"
    SyncState.Live -> "Sync: live incremental sync active"
    is SyncState.Failed -> "Sync: failed ($cause), retry in ${retryDelay.inWholeSeconds}s"
}

private fun ConversationDetails.toConversationListItem(): ConversationListItemUiState {
    val conversation = conversation
    return ConversationListItemUiState(
        id = conversation.id,
        title = conversationTitle(),
        subtitle = conversation.type.toSubtitle(),
        protocolLabel = conversation.protocol.name(),
        hasOngoingCall = (this as? ConversationDetails.Group)?.hasOngoingCall == true,
    )
}

private fun Message.toMessageItem(selfUserId: UserId): MessageItemUiState = MessageItemUiState(
    id = id,
    senderLabel = sender?.name ?: senderUserId.value,
    body = content.toPreviewText(),
    dateLabel = status.toString().replace('_', ' '),
    isOwnMessage = senderUserId == selfUserId,
)

private fun MessageContent.toPreviewText(): String =
    previewPrimaryText()
        ?: previewSystemText()
        ?: previewMutationText()
        ?: previewMembershipText()
        ?: previewFallbackText()

private fun ConversationDetails.conversationTitle(): String {
    val conversation = conversation
    return when (this) {
        is ConversationDetails.OneOne -> otherUser.name ?: conversation.name ?: conversation.id.value
        is ConversationDetails.Connection -> otherUser?.name ?: conversation.name ?: conversationId.value
        is ConversationDetails.Group -> conversation.name ?: conversation.id.value
        is ConversationDetails.Self -> conversation.name ?: "Self conversation"
        is ConversationDetails.Team -> conversation.name ?: conversation.id.value
    }
}

private fun Conversation.Type.toSubtitle(): String = when (this) {
    Conversation.Type.Self -> "Self"
    Conversation.Type.OneOnOne -> "1:1"
    Conversation.Type.ConnectionPending -> "Pending connection"
    is Conversation.Type.Group.Channel -> "Channel"
    is Conversation.Type.Group.Regular -> "Group"
}

private fun MessageContent.previewPrimaryText(): String? = when (this) {
    is MessageContent.Text -> value
    is MessageContent.FailedDecryption -> "Failed decryption (${errorCode ?: "unknown"})"
    is MessageContent.RestrictedAsset -> "Restricted asset: $name"
    is MessageContent.Location -> name ?: "Location"
    is MessageContent.ConversationRenamed -> "Conversation renamed to $conversationName"
    is MessageContent.Multipart -> value ?: "Multipart message"
    is MessageContent.TextEdited -> newContent
    else -> null
}

private fun MessageContent.previewSystemText(): String? =
    previewSimpleSystemText() ?: previewConversationSystemText()

private fun MessageContent.previewSimpleSystemText(): String? = when (this) {
    is MessageContent.Asset -> "Asset"
    is MessageContent.Knock -> "Knock"
    is MessageContent.Composite -> "Composite message"
    is MessageContent.Availability -> "Availability"
    is MessageContent.ButtonAction -> "Button action"
    is MessageContent.ButtonActionConfirmation -> "Button action confirmation"
    is MessageContent.Calling -> "Calling event"
    is MessageContent.Cleared -> "Message cleared"
    MessageContent.ClientAction -> "Client action"
    MessageContent.CryptoSessionReset -> "Crypto session reset"
    is MessageContent.DataTransfer -> "Data transfer"
    is MessageContent.MissedCall -> "Missed call"
    else -> null
}

private fun MessageContent.previewConversationSystemText(): String? = when (this) {
    is MessageContent.ConversationCreated -> "Conversation created"
    is MessageContent.ConversationDegradedMLS -> "Conversation verification degraded (MLS)"
    is MessageContent.ConversationDegradedProteus -> "Conversation verification degraded (Proteus)"
    is MessageContent.ConversationMessageTimerChanged -> "Message timer updated"
    is MessageContent.ConversationProtocolChanged -> "Conversation protocol changed"
    MessageContent.ConversationProtocolChangedDuringACall -> "Protocol changed during a call"
    is MessageContent.ConversationReceiptModeChanged -> "Receipt mode changed"
    else -> null
}

private fun MessageContent.previewMutationText(): String? = when (this) {
    is MessageContent.DeleteForMe -> "Delete for me"
    is MessageContent.DeleteMessage -> "Message deleted"
    is MessageContent.HistoryLost -> "History lost"
    MessageContent.HistoryLostProtocolChanged -> "History lost after protocol change"
    MessageContent.Ignored -> "Ignored"
    is MessageContent.InCallEmoji -> "In-call emoji"
    is MessageContent.LastRead -> "Last read update"
    MessageContent.MLSWrongEpochWarning -> "MLS wrong epoch warning"
    is MessageContent.MultipartEdited -> "Multipart edited"
    is MessageContent.NewConversationReceiptMode -> "Conversation receipt mode set"
    is MessageContent.Reaction -> "Reaction"
    is MessageContent.Receipt -> "Receipt"
    else -> null
}

private fun MessageContent.previewMembershipText(): String? = when (this) {
    is MessageContent.MemberChange.Added -> "Members added"
    is MessageContent.MemberChange.CreationAdded -> "Members added on creation"
    is MessageContent.MemberChange.FailedToAdd -> "Failed to add members"
    is MessageContent.MemberChange.Removed -> "Members removed"
    is MessageContent.MemberChange.RemovedFromTeam -> "Removed from team"
    else -> null
}

private fun MessageContent.previewFallbackText(): String = when (this) {
    is MessageContent.Unknown -> "Unknown message"
    else -> getType()
}
