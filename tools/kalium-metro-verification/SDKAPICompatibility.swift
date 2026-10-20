// Compile-only public SDK client. No top-level execution, test hooks, or runtime assertions.
// Apple hosts must await openSessionScope before cached synchronous session access (ADR 0011).
import KaliumLogic

func legacyFacadeAccessors(core: CoreLogic, session: UserSessionScope, userID: DataQualifiedID) {
    let _: CoreLogicCommon = core
    let global: GlobalKaliumScope = core.getGlobalScope()
    let _: SessionScope = global.session
    let _: UserSessionScope = core.getSessionScope(userId: userID)
    let _: AuthenticationScopeForConfigIdUseCase = core.authenticationScopeForConfigId
    let _: ConversationScope = session.conversations
    let _: MessageScope = session.messages
    let _: ClientScope = session.client
    let _: UserScope = session.users
    let _: SearchScope = session.search
    let _: ChannelsScope = session.channels
    let _: MeetingScope = session.meetings
    let _: DebugScope = session.debug
    let _: CallsScope = session.calls
    let _: BackupScope = session.backup
    let _: MultiPlatformBackupScope = session.multiPlatformBackup
    let _: TeamScope = session.team
    let _: ServiceScope = session.service
    let _: AppScope = session.apps
    let _: ConnectionScope = session.connection
    let _: AuthenticationScope = session.authenticationScope
    let _: WorkLongWorkScope = session.longWork
    let _: CellsCellsScope = session.cells
    let _: ObserveConversationDetailsUseCase = session.conversations.observeConversationDetails
    let _: ObserveMessageByIdUseCase = session.messages.observeMessageById
    let _: GetMessageByIdUseCase = session.messages.getMessageById
}

func individualGlobalAPIs(core: CoreLogic, authentication: AuthenticationScope) {
    let accounts: any KaliumGlobalAccountsApi = core.globalAccountsApi
    let validation: any KaliumGlobalValidationApi = core.globalValidationApi
    let runtime: any KaliumGlobalRuntimeApi = core.globalRuntimeApi
    let factory: any KaliumSessionSelectionApiFactory = core.sessionSelectionApiFactory
    let selection: any KaliumSessionSelectionApi = factory.create()
    let verification: any KaliumAuthenticationVerificationApi = authentication.individualVerificationApi
    let _: AddAuthenticatedUserUseCase = accounts.globalAddAuthenticatedAccount
    let _: any ValidateEmailUseCase = validation.globalValidateEmailUseCase
    let _: any ObserveIfAppUpdateRequiredUseCase = runtime.globalObserveIfAppUpdateRequired
    let _: CurrentSessionUseCase = selection.currentSession
    let _: RequestSecondFactorVerificationCodeUseCase = verification.requestSecondFactorVerificationCode
}

func individualUserAPIs(session: UserSessionScope) {
    let conversation: any KaliumConversationApi = session.conversationApi
    let message: any KaliumMessageApi = session.messageApi
    let user: any KaliumUserApi = session.userApi
    let client: any KaliumClientApi = session.clientApi
    let search: any KaliumSearchApi = session.searchApi
    let channels: any KaliumChannelsApi = session.channelsApi
    let meeting: any KaliumMeetingApi = session.meetingApi
    let debug: any KaliumDebugApi = session.debugApi
    let _: any ObserveConversationListDetailsUseCase = conversation.conversationObserveConversationListDetails
    let _: any ObserveMessageReactionsUseCase = message.messageObserveMessageReactions
    let _: DeleteAccountUseCase = user.userDeleteAccount
    let _: any DeleteClientUseCase = client.clientDeleteClient
    let _: FederatedSearchParser = search.searchFederatedSearchParser
    let _: ObserveChannelsCreationPermissionUseCase = channels.channelsObserveChannelsCreationPermissionUseCase
    let _: any CreateNewMeetingUseCase = meeting.meetingCreateNewMeeting
    let _: ChangeProfilingUseCase = debug.debugChangeProfiling
}

func freshOperationAPIs(session: UserSessionScope) {
    let callsFactory: any KaliumCallsApiFactory = session.callsApiFactory
    let backupFactory: any KaliumBackupApiFactory = session.backupApiFactory
    let mpBackupFactory: any KaliumMultiPlatformBackupApiFactory = session.multiPlatformBackupApiFactory
    let teamFactory: any KaliumTeamApiFactory = session.teamApiFactory
    let serviceFactory: any KaliumServiceApiFactory = session.serviceApiFactory
    let appFactory: any KaliumAppApiFactory = session.appApiFactory
    let connectionFactory: any KaliumConnectionApiFactory = session.connectionApiFactory
    let calls: any KaliumCallsApi = callsFactory.create()
    let backup: any KaliumBackupApi = backupFactory.create()
    let mpBackup: any KaliumMultiPlatformBackupApi = mpBackupFactory.create()
    let team: any KaliumTeamApi = teamFactory.create()
    let service: any KaliumServiceApi = serviceFactory.create()
    let app: any KaliumAppApi = appFactory.create()
    let connection: any KaliumConnectionApi = connectionFactory.create()
    let _: any AnswerCallUseCase = calls.callsAnswerCall
    let _: any CreateBackupUseCase = backup.backupCreate
    let _: any CreateMPBackupUseCase = mpBackup.mpBackupCreate
    let _: any IsSelfATeamMemberUseCase = team.teamIsSelfATeamMember
    let _: any GetServiceByIdUseCase = service.serviceGetServiceById
    let _: any GetAppByIdUseCase = app.appGetAppById
    let _: any AcceptConnectionRequestUseCase = connection.connectionAcceptConnectionRequest
}

func consumerContracts(session: UserSessionScope) {
    let consumer: any KaliumConsumer = session.createConsumer()
    let _: ObserveConversationDetailsUseCase = consumer.observeConversationDetails
    let _: ObserveMessageByIdUseCase = consumer.observeMessageById
    let _: GetMessageByIdUseCase = consumer.getMessageById
    // Kotlin generic T is erased in Objective-C; its default argument is not a Swift overload.
    let _: Any? = consumer.assemble(
        create: { consumer.getMessageById },
        onDiscard: { (_: Any?) in },
        jobOnDiscard: { (_: Any?) -> (any Kotlinx_coroutines_coreJob)? in nil }
    )
    let _: any KotlinAutoCloseable = consumer
    consumer.close()
}

func asyncAppleContracts(
    core: CoreLogic, consumer: any KaliumConsumer, userID: DataQualifiedID,
    conversationID: DataQualifiedID, messageID: String
) async throws {
    let _: UserSessionScope = try await core.openSessionScope(userId: userID)
    let _: any Kotlinx_coroutines_coreFlow = try await consumer.observeConversationDetails.invoke(conversationId: conversationID)
    let _: any Kotlinx_coroutines_coreFlow = try await consumer.observeMessageById.invoke(conversationId: conversationID, messageId: messageID)
    let _: any GetMessageByIdUseCaseResult = try await consumer.getMessageById.invoke(conversationId: conversationID, messageId: messageID)
    try await consumer.closeAndJoin()
    try await core.suspendSessionScope(userId: userID)
    try await core.releaseSessionScope(userId: userID)
}
