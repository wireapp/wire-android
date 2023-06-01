/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.navigation

import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import com.wire.android.BuildConfig
import com.wire.android.model.ImageAsset
import com.wire.android.navigation.NavigationItemDestinationsRoutes.ADD_CONVERSATION_PARTICIPANTS
import com.wire.android.navigation.NavigationItemDestinationsRoutes.APP_SETTINGS
import com.wire.android.navigation.NavigationItemDestinationsRoutes.BACKUP_AND_RESTORE
import com.wire.android.navigation.NavigationItemDestinationsRoutes.CONVERSATION
import com.wire.android.navigation.NavigationItemDestinationsRoutes.CREATE_ACCOUNT_SUMMARY
import com.wire.android.navigation.NavigationItemDestinationsRoutes.CREATE_ACCOUNT_USERNAME
import com.wire.android.navigation.NavigationItemDestinationsRoutes.CREATE_PERSONAL_ACCOUNT
import com.wire.android.navigation.NavigationItemDestinationsRoutes.CREATE_TEAM
import com.wire.android.navigation.NavigationItemDestinationsRoutes.DEBUG
import com.wire.android.navigation.NavigationItemDestinationsRoutes.DEVICE_DETAILS
import com.wire.android.navigation.NavigationItemDestinationsRoutes.EDIT_CONVERSATION_NAME
import com.wire.android.navigation.NavigationItemDestinationsRoutes.EDIT_DISPLAY_NAME
import com.wire.android.navigation.NavigationItemDestinationsRoutes.EDIT_EMAIL
import com.wire.android.navigation.NavigationItemDestinationsRoutes.EDIT_GUEST_ACCESS
import com.wire.android.navigation.NavigationItemDestinationsRoutes.EDIT_HANDLE
import com.wire.android.navigation.NavigationItemDestinationsRoutes.EDIT_SELF_DELETING_MESSAGES
import com.wire.android.navigation.NavigationItemDestinationsRoutes.GROUP_CONVERSATION_ALL_PARTICIPANTS
import com.wire.android.navigation.NavigationItemDestinationsRoutes.GROUP_CONVERSATION_DETAILS
import com.wire.android.navigation.NavigationItemDestinationsRoutes.HOME
import com.wire.android.navigation.NavigationItemDestinationsRoutes.IMAGE_PICKER
import com.wire.android.navigation.NavigationItemDestinationsRoutes.IMPORT_MEDIA
import com.wire.android.navigation.NavigationItemDestinationsRoutes.INCOMING_CALL
import com.wire.android.navigation.NavigationItemDestinationsRoutes.INITIAL_SYNC
import com.wire.android.navigation.NavigationItemDestinationsRoutes.INITIATING_CALL
import com.wire.android.navigation.NavigationItemDestinationsRoutes.LOGIN
import com.wire.android.navigation.NavigationItemDestinationsRoutes.MEDIA_GALLERY
import com.wire.android.navigation.NavigationItemDestinationsRoutes.MESSAGE_DETAILS
import com.wire.android.navigation.NavigationItemDestinationsRoutes.MIGRATION
import com.wire.android.navigation.NavigationItemDestinationsRoutes.MY_ACCOUNT
import com.wire.android.navigation.NavigationItemDestinationsRoutes.NETWORK_SETTINGS
import com.wire.android.navigation.NavigationItemDestinationsRoutes.NEW_CONVERSATION
import com.wire.android.navigation.NavigationItemDestinationsRoutes.ONGOING_CALL
import com.wire.android.navigation.NavigationItemDestinationsRoutes.OTHER_USER_PROFILE
import com.wire.android.navigation.NavigationItemDestinationsRoutes.PRIVACY_SETTINGS
import com.wire.android.navigation.NavigationItemDestinationsRoutes.REGISTER_DEVICE
import com.wire.android.navigation.NavigationItemDestinationsRoutes.REMOVE_DEVICES
import com.wire.android.navigation.NavigationItemDestinationsRoutes.SELF_DEVICES
import com.wire.android.navigation.NavigationItemDestinationsRoutes.SELF_USER_PROFILE
import com.wire.android.navigation.NavigationItemDestinationsRoutes.SERVICE_DETAILS
import com.wire.android.navigation.NavigationItemDestinationsRoutes.VERIFY_EMAIL
import com.wire.android.navigation.NavigationItemDestinationsRoutes.WELCOME
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.android.ui.authentication.create.personalaccount.CreatePersonalAccountScreen
import com.wire.android.ui.authentication.create.summary.CreateAccountSummaryScreen
import com.wire.android.ui.authentication.create.team.CreateTeamScreen
import com.wire.android.ui.authentication.create.username.CreateAccountUsernameScreen
import com.wire.android.ui.authentication.devices.register.RegisterDeviceScreen
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceScreen
import com.wire.android.ui.authentication.login.LoginScreen
import com.wire.android.ui.authentication.welcome.WelcomeScreen
import com.wire.android.ui.calling.incoming.IncomingCallScreen
import com.wire.android.ui.calling.initiating.InitiatingCallScreen
import com.wire.android.ui.calling.ongoing.OngoingCallScreen
import com.wire.android.ui.debug.DebugScreen
import com.wire.android.ui.home.HomeScreen
import com.wire.android.ui.home.conversations.ConversationScreen
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsScreen
import com.wire.android.ui.home.conversations.details.editguestaccess.EditGuestAccessParams
import com.wire.android.ui.home.conversations.details.editguestaccess.EditGuestAccessScreen
import com.wire.android.ui.home.conversations.details.editselfdeletingmessages.EditSelfDeletingMessagesScreen
import com.wire.android.ui.home.conversations.details.metadata.EditConversationNameScreen
import com.wire.android.ui.home.conversations.details.participants.GroupConversationAllParticipantsScreen
import com.wire.android.ui.home.conversations.messagedetails.MessageDetailsScreen
import com.wire.android.ui.home.conversations.search.AddMembersSearchRouter
import com.wire.android.ui.home.gallery.MediaGalleryScreen
import com.wire.android.ui.home.newconversation.NewConversationRouter
import com.wire.android.ui.home.settings.account.MyAccountScreen
import com.wire.android.ui.home.settings.account.displayname.ChangeDisplayNameScreen
import com.wire.android.ui.home.settings.account.email.updateEmail.ChangeEmailScreen
import com.wire.android.ui.home.settings.account.email.verifyEmail.VerifyEmailScreen
import com.wire.android.ui.home.settings.account.handle.ChangeHandleScreen
import com.wire.android.ui.home.settings.appsettings.AppSettingsScreen
import com.wire.android.ui.home.settings.appsettings.networkSettings.NetworkSettingsScreen
import com.wire.android.ui.home.settings.backup.BackupAndRestoreScreen
import com.wire.android.ui.home.settings.privacy.PrivacySettingsConfigScreen
import com.wire.android.ui.initialsync.InitialSyncScreen
import com.wire.android.ui.migration.MigrationScreen
import com.wire.android.ui.settings.devices.DeviceDetailsScreen
import com.wire.android.ui.settings.devices.SelfDevicesScreen
import com.wire.android.ui.sharing.ImportMediaScreen
import com.wire.android.ui.userprofile.avatarpicker.AvatarPickerScreen
import com.wire.android.ui.userprofile.other.OtherUserProfileScreen
import com.wire.android.ui.userprofile.self.SelfUserProfileScreen
import com.wire.android.ui.userprofile.service.ServiceDetailsScreen
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.BotService
import com.wire.kalium.logic.data.user.UserId
import io.github.esentsov.PackagePrivate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8

/**
 * The class encapsulating the app main navigational items.
 */
enum class NavigationItem(
    @PackagePrivate
    internal val primaryRoute: String,
    private val canonicalRoute: String = primaryRoute,
    val deepLinks: List<NavDeepLink> = listOf(),
    val content: @Composable (ContentParams) -> Unit,
    val animationConfig: NavigationAnimationConfig = NavigationAnimationConfig.NoAnimation,
    val screenMode: ScreenMode = ScreenMode.NONE
) {
    Welcome(
        primaryRoute = WELCOME,
        content = { WelcomeScreen() },
        animationConfig = NavigationAnimationConfig.CustomAnimation(smoothSlideInFromRight(), smoothSlideOutFromLeft())
    ),

    Migration(
        primaryRoute = MIGRATION,
        canonicalRoute = "$MIGRATION?$EXTRA_USER_ID={$EXTRA_USER_ID}",
        content = { MigrationScreen() },
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String {
            val userIdString: String = arguments.filterIsInstance<UserId>().firstOrNull()?.toString() ?: "{$EXTRA_USER_ID}"
            return "$MIGRATION?$EXTRA_USER_ID=$userIdString"
        }
    },

    Login(
        primaryRoute = LOGIN,
        canonicalRoute = "$LOGIN?$EXTRA_SSO_LOGIN_RESULT={$EXTRA_SSO_LOGIN_RESULT}&$EXTRA_USER_HANDLE={$EXTRA_USER_HANDLE}",
        content = { LoginScreen() },
        animationConfig = NavigationAnimationConfig.CustomAnimation(smoothSlideInFromRight(), smoothSlideOutFromLeft())
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String {
            val ssoLoginResultString = arguments.filterIsInstance<DeepLinkResult.SSOLogin>().firstOrNull()?.let { Json.encodeToString(it) }
                ?: "{$EXTRA_SSO_LOGIN_RESULT}"
            val userHandleString: String = arguments.filterIsInstance<String>().firstOrNull()?.toString() ?: "{$EXTRA_USER_HANDLE}"
            return "$LOGIN?$EXTRA_SSO_LOGIN_RESULT=$ssoLoginResultString&$EXTRA_USER_HANDLE=$userHandleString"
        }
    },

    CreateTeam(
        primaryRoute = CREATE_TEAM,
        content = { CreateTeamScreen() },
        animationConfig = NavigationAnimationConfig.CustomAnimation(smoothSlideInFromRight(), smoothSlideOutFromLeft())
    ),

    CreatePersonalAccount(
        primaryRoute = CREATE_PERSONAL_ACCOUNT,
        content = { CreatePersonalAccountScreen() },
        animationConfig = NavigationAnimationConfig.CustomAnimation(smoothSlideInFromRight(), smoothSlideOutFromLeft())
    ),

    CreateUsername(
        primaryRoute = CREATE_ACCOUNT_USERNAME,
        content = { CreateAccountUsernameScreen() },
        animationConfig = NavigationAnimationConfig.CustomAnimation(smoothSlideInFromRight(), smoothSlideOutFromLeft())
    ),

    CreateAccountSummary(
        primaryRoute = CREATE_ACCOUNT_SUMMARY,
        canonicalRoute = "$CREATE_ACCOUNT_SUMMARY/{$EXTRA_CREATE_ACCOUNT_FLOW_TYPE}",
        content = { CreateAccountSummaryScreen() },
        animationConfig = NavigationAnimationConfig.CustomAnimation(smoothSlideInFromRight(), smoothSlideOutFromLeft())
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String {
            val type: CreateAccountFlowType = checkNotNull(
                arguments.filterIsInstance<CreateAccountFlowType>().firstOrNull()
            ) { "Unknown CreateAccountFlowType" }
            return "$primaryRoute/${type.routeArg}"
        }
    },

    RemoveDevices(
        primaryRoute = REMOVE_DEVICES,
        content = { RemoveDeviceScreen() },
        animationConfig = NavigationAnimationConfig.CustomAnimation(smoothSlideInFromRight(), smoothSlideOutFromLeft())
    ),

    RegisterDevice(
        primaryRoute = REGISTER_DEVICE,
        content = { RegisterDeviceScreen() },
        animationConfig = NavigationAnimationConfig.CustomAnimation(smoothSlideInFromRight(), smoothSlideOutFromLeft())
    ),

    Home(
        primaryRoute = HOME,
        content = { HomeScreen(it.navBackStackEntry.savedStateHandle.getBackNavArgs()) },
        animationConfig = NavigationAnimationConfig.DelegatedAnimation
    ),

    InitialSync(
        primaryRoute = INITIAL_SYNC,
        content = { InitialSyncScreen() },
    ),

    AppSettings(
        primaryRoute = APP_SETTINGS,
        content = { AppSettingsScreen() },
    ),

    SelfDevices(
        primaryRoute = SELF_DEVICES,
        content = { SelfDevicesScreen() }
    ),

    DeviceDetails(
        primaryRoute = DEVICE_DETAILS,
        canonicalRoute = "$DEVICE_DETAILS?$EXTRA_USER_ID={$EXTRA_USER_ID}&$EXTRA_DEVICE_ID={$EXTRA_DEVICE_ID}",
        content = { DeviceDetailsScreen(it.navBackStackEntry.savedStateHandle.getBackNavArgs()) },
        animationConfig = NavigationAnimationConfig.DelegatedAnimation
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String {
            val clientIdString: String = arguments.filterIsInstance<ClientId>().firstOrNull()?.value!!
            val userId: UserId = arguments.filterIsInstance<UserId>().firstOrNull()!!
            return "$DEVICE_DETAILS?$EXTRA_USER_ID=$userId&$EXTRA_DEVICE_ID=$clientIdString"
        }
    },

    PrivacySettings(
        primaryRoute = PRIVACY_SETTINGS,
        content = { PrivacySettingsConfigScreen() }
    ),

    BackupAndRestore(
        primaryRoute = BACKUP_AND_RESTORE,
        content = { BackupAndRestoreScreen() }
    ),

    Debug(
        primaryRoute = DEBUG,
        content = { DebugScreen() },
    ),

    EditDisplayName(
        primaryRoute = EDIT_DISPLAY_NAME,
        content = { ChangeDisplayNameScreen() }
    ),

    EditEmailAddress(
        primaryRoute = EDIT_EMAIL,
        content = { ChangeEmailScreen() }
    ),
    VerifyEmailAddress(
        primaryRoute = "$VERIFY_EMAIL?$EXTRA_NEW_EMAIL={$EXTRA_NEW_EMAIL}",
        content = { VerifyEmailScreen() }
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String {
            val newEmail: String = arguments.filterIsInstance<String>().first().let {
                URLEncoder.encode(it, UTF_8.name())
            }
            return "$VERIFY_EMAIL?$EXTRA_NEW_EMAIL=$newEmail"
        }
    },

    EditHandle(
        primaryRoute = EDIT_HANDLE,
        content = { ChangeHandleScreen() }
    ),

    NetworkSettings(
        primaryRoute = NETWORK_SETTINGS,
        content = { NetworkSettingsScreen() },
    ),

    Support(
        primaryRoute = BuildConfig.URL_SUPPORT,
        content = { },
    ),

    MyAccount(
        primaryRoute = MY_ACCOUNT,
        content = { MyAccountScreen(it.navBackStackEntry.savedStateHandle.getBackNavArgs()) }
    ),

    SelfUserProfile(
        primaryRoute = SELF_USER_PROFILE,
        content = { SelfUserProfileScreen() },
        animationConfig = NavigationAnimationConfig.CustomAnimation(expandInToView(), shrinkOutFromView())
    ),

    OtherUserProfile(
        primaryRoute = OTHER_USER_PROFILE,
        canonicalRoute = "$OTHER_USER_PROFILE?$EXTRA_USER_ID={$EXTRA_USER_ID}&$EXTRA_CONVERSATION_ID={$EXTRA_CONVERSATION_ID}",
        content = { OtherUserProfileScreen() },
        animationConfig = NavigationAnimationConfig.NoAnimation
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String {
            val userId: QualifiedID = arguments.filterIsInstance<QualifiedID>()[0]
            val conversationId: QualifiedID? = arguments.filterIsInstance<QualifiedID>().getOrNull(1)
            val baseRoute = "$primaryRoute?$EXTRA_USER_ID=$userId"
            return conversationId?.let { "$baseRoute&$EXTRA_CONVERSATION_ID=$it" } ?: baseRoute
        }
    },

    ServiceDetails(
        primaryRoute = SERVICE_DETAILS,
        canonicalRoute = "$SERVICE_DETAILS?$EXTRA_BOT_SERVICE_ID={$EXTRA_BOT_SERVICE_ID}&$EXTRA_CONVERSATION_ID={$EXTRA_CONVERSATION_ID}",
        content = { ServiceDetailsScreen() },
        animationConfig = NavigationAnimationConfig.NoAnimation
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String {
            val botServiceId: BotService = arguments.filterIsInstance<BotService>()[0]
            val conversationId: QualifiedID = arguments.filterIsInstance<QualifiedID>()[0]
            return "$primaryRoute?$EXTRA_BOT_SERVICE_ID=$botServiceId&$EXTRA_CONVERSATION_ID=$conversationId"
        }
    },

    ProfileImagePicker(
        primaryRoute = IMAGE_PICKER,
        content = { AvatarPickerScreen() },
    ),

    Conversation(
        primaryRoute = CONVERSATION,
        canonicalRoute = "$CONVERSATION?$EXTRA_CONVERSATION_ID={$EXTRA_CONVERSATION_ID}",
        content = { ConversationScreen(backNavArgs = it.navBackStackEntry.savedStateHandle.getBackNavArgs()) },
        animationConfig = NavigationAnimationConfig.NoAnimation
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String {
            val conversationIdString: String = arguments.filterIsInstance<ConversationId>().firstOrNull()?.toString()
                ?: "{$EXTRA_CONVERSATION_ID}"
            return "$CONVERSATION?$EXTRA_CONVERSATION_ID=$conversationIdString"
        }
    },

    MessageDetails(
        primaryRoute = MESSAGE_DETAILS,
        canonicalRoute = "$MESSAGE_DETAILS?$EXTRA_CONVERSATION_ID={$EXTRA_CONVERSATION_ID}" +
                "&$EXTRA_MESSAGE_ID={$EXTRA_MESSAGE_ID}&$EXTRA_IS_SELF_MESSAGE={$EXTRA_IS_SELF_MESSAGE}",
        content = { MessageDetailsScreen() }
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String {
            val conversationIdString: String = arguments.filterIsInstance<ConversationId>().firstOrNull()?.toString()
                ?: "{$EXTRA_CONVERSATION_ID}"
            val messageIdString: String = arguments.filterIsInstance<String>().firstOrNull()?.toString()
                ?: "{$EXTRA_MESSAGE_ID}"
            val isSelfMessage: String = arguments.filterIsInstance<Boolean>().firstOrNull()?.toString() ?: "{$EXTRA_IS_SELF_MESSAGE}"
            return "$MESSAGE_DETAILS?$EXTRA_CONVERSATION_ID=$conversationIdString" +
                    "&$EXTRA_MESSAGE_ID=$messageIdString&$EXTRA_IS_SELF_MESSAGE=$isSelfMessage"
        }
    },

    GroupConversationDetails(
        primaryRoute = GROUP_CONVERSATION_DETAILS,
        canonicalRoute = "$GROUP_CONVERSATION_DETAILS/{$EXTRA_CONVERSATION_ID}",
        content = { GroupConversationDetailsScreen(it.navBackStackEntry.savedStateHandle.getBackNavArgs()) },
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String = routeWithConversationIdArg(arguments)
    },

    AddConversationParticipants(
        primaryRoute = ADD_CONVERSATION_PARTICIPANTS,
        canonicalRoute = "$ADD_CONVERSATION_PARTICIPANTS/{$EXTRA_CONVERSATION_ID}/{$EXTRA_IS_SERVICES_ALLOWED}",
        content = { AddMembersSearchRouter() }
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String {
            val conversationId: QualifiedID = arguments.filterIsInstance<QualifiedID>()[0]
            val isServicesAllowed: Boolean = arguments.filterIsInstance<Boolean>()[0]
            return "$primaryRoute/$conversationId/$isServicesAllowed"
        }
    },

    GroupConversationAllParticipants(
        primaryRoute = GROUP_CONVERSATION_ALL_PARTICIPANTS,
        canonicalRoute = "$GROUP_CONVERSATION_ALL_PARTICIPANTS/{$EXTRA_CONVERSATION_ID}",
        content = { GroupConversationAllParticipantsScreen() },
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String = routeWithConversationIdArg(arguments)
    },

    NewConversation(
        primaryRoute = NEW_CONVERSATION,
        canonicalRoute = NEW_CONVERSATION,
        content = { NewConversationRouter() }
    ),

    EditConversationName(
        primaryRoute = EDIT_CONVERSATION_NAME,
        canonicalRoute = "$EDIT_CONVERSATION_NAME/{$EXTRA_CONVERSATION_ID}",
        content = { EditConversationNameScreen() }
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String = routeWithConversationIdArg(arguments)
    },

    EditGuestAccess(
        primaryRoute = EDIT_GUEST_ACCESS,
        canonicalRoute = "$EDIT_GUEST_ACCESS?$EXTRA_CONVERSATION_ID={$EXTRA_CONVERSATION_ID}" +
                "&$EXTRA_EDIT_GUEST_ACCESS_PARAMS={$EXTRA_EDIT_GUEST_ACCESS_PARAMS}",
        content = { EditGuestAccessScreen() },
        animationConfig = NavigationAnimationConfig.CustomAnimation(smoothSlideInFromRight(), smoothSlideOutFromLeft())
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String {
            val conversationIdString: String = arguments.filterIsInstance<ConversationId>().firstOrNull()?.toString()
                ?: "{$EXTRA_CONVERSATION_ID}"

            val editGuestAccessParams = Json.encodeToString(arguments.filterIsInstance<EditGuestAccessParams>().firstOrNull())

            return "$EDIT_GUEST_ACCESS?$EXTRA_CONVERSATION_ID=$conversationIdString" +
                    "&$EXTRA_EDIT_GUEST_ACCESS_PARAMS=$editGuestAccessParams"
        }
    },

    EditSelfDeletingMessages(
        primaryRoute = EDIT_SELF_DELETING_MESSAGES,
        canonicalRoute = "$EDIT_SELF_DELETING_MESSAGES?$EXTRA_CONVERSATION_ID={$EXTRA_CONVERSATION_ID}",
        content = { EditSelfDeletingMessagesScreen() },
        animationConfig = NavigationAnimationConfig.CustomAnimation(smoothSlideInFromRight(), smoothSlideOutFromLeft())
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String {
            val conversationIdString: String = arguments.filterIsInstance<ConversationId>().firstOrNull()?.toString()
                ?: "{$EXTRA_CONVERSATION_ID}"

            return "$EDIT_SELF_DELETING_MESSAGES?$EXTRA_CONVERSATION_ID=$conversationIdString"
        }
    },

    OngoingCall(
        primaryRoute = ONGOING_CALL,
        canonicalRoute = "$ONGOING_CALL/{$EXTRA_CONVERSATION_ID}",
        content = { OngoingCallScreen() },
        screenMode = ScreenMode.WAKE_UP,
        animationConfig = NavigationAnimationConfig.CustomAnimation(null, fadeOut())
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String = routeWithConversationIdArg(arguments)
    },

    InitiatingCall(
        primaryRoute = INITIATING_CALL,
        canonicalRoute = "$INITIATING_CALL/{$EXTRA_CONVERSATION_ID}",
        content = { InitiatingCallScreen() },
        screenMode = ScreenMode.KEEP_ON,
        animationConfig = NavigationAnimationConfig.DelegatedAnimation
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String = routeWithConversationIdArg(arguments)
    },

    IncomingCall(
        primaryRoute = INCOMING_CALL,
        canonicalRoute = "$INCOMING_CALL?$EXTRA_CONVERSATION_ID={$EXTRA_CONVERSATION_ID}",
        content = { IncomingCallScreen() },
        screenMode = ScreenMode.WAKE_UP,
        animationConfig = NavigationAnimationConfig.DelegatedAnimation
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String {
            val conversationIdString: String = arguments.filterIsInstance<ConversationId>().firstOrNull()?.toString()
                ?: "{$EXTRA_CONVERSATION_ID}"
            return "$INCOMING_CALL?$EXTRA_CONVERSATION_ID=$conversationIdString"
        }
    },

    Gallery(
        primaryRoute = MEDIA_GALLERY,
        canonicalRoute = "$MEDIA_GALLERY/{$EXTRA_IMAGE_DATA}",
        content = { MediaGalleryScreen() }
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String {
            val imageAssetId: ImageAsset.PrivateAsset? = arguments.filterIsInstance<ImageAsset.PrivateAsset>().firstOrNull()
            val mappedArgs = imageAssetId?.toString() ?: ""
            return imageAssetId?.run { "$primaryRoute/$mappedArgs" } ?: primaryRoute
        }
    },

    ImportMedia(
        primaryRoute = IMPORT_MEDIA,
        content = { ImportMediaScreen() }
    );

    /**
     * The item theoretical route. If the route includes a route ID, this method will return the route with the placeholder.
     * This should only be accessed to create the initial navigation graph, not as a navigation item route
     */
    @PackagePrivate
    fun getCanonicalRoute() = canonicalRoute

    open fun getRouteWithArgs(arguments: List<Any> = emptyList()): String = primaryRoute

    internal fun routeWithConversationIdArg(arguments: List<Any> = emptyList()): String {
        val conversationId: ConversationId? = arguments.filterIsInstance<ConversationId>().firstOrNull()
        return conversationId?.let { "$primaryRoute/$it" } ?: primaryRoute
    }

    val itemName: String get() = ITEM_NAME_PREFIX + this.name

    companion object {
        private const val ITEM_NAME_PREFIX = "NavigationItem."
        private val map: Map<String, NavigationItem> = values().associateBy { it.primaryRoute }
        fun fromRoute(fullRoute: String): NavigationItem? = map[fullRoute.getPrimaryRoute()]
    }
}

object NavigationItemDestinationsRoutes {
    const val WELCOME = "welcome_screen"
    const val MIGRATION = "migration_screen"
    const val LOGIN = "login_screen"
    const val CREATE_TEAM = "create_team_screen"
    const val CREATE_PERSONAL_ACCOUNT = "create_personal_account_screen"
    const val CREATE_ACCOUNT_USERNAME = "create_account_username_screen"
    const val CREATE_ACCOUNT_SUMMARY = "create_account_summary_screen"
    const val HOME = "home_landing_screen"
    const val INITIAL_SYNC = "initial_sync_screen"
    const val SELF_USER_PROFILE = "self_user_profile_screen"
    const val OTHER_USER_PROFILE = "other_user_profile_screen"
    const val SERVICE_DETAILS = "service_details_screen"
    const val CONVERSATION = "detailed_conversation_screen"
    const val EDIT_CONVERSATION_NAME = "edit_conversation_name_screen"
    const val EDIT_GUEST_ACCESS = "edit_guest_access_screen"
    const val EDIT_SELF_DELETING_MESSAGES = "edit_self_deleting_messages_screen"
    const val GROUP_CONVERSATION_DETAILS = "group_conversation_details_screen"
    const val MESSAGE_DETAILS = "message_details_screen"
    const val GROUP_CONVERSATION_ALL_PARTICIPANTS = "group_conversation_all_participants_screen"
    const val ADD_CONVERSATION_PARTICIPANTS = "add_conversation_participants"
    const val APP_SETTINGS = "app_settings_screen"
    const val SELF_DEVICES = "self_devices_screen"
    const val PRIVACY_SETTINGS = "privacy_settings"
    const val BACKUP_AND_RESTORE = "backup_and_restore_screen"
    const val MY_ACCOUNT = "my_account_screen"
    const val EDIT_DISPLAY_NAME = "edit_display_name_screen"
    const val EDIT_EMAIL = "edit_email_screen"
    const val VERIFY_EMAIL = "verify_email_screen"
    const val EDIT_HANDLE = "edit_handle_screen"
    const val DEBUG = "debug_screen"
    const val REMOVE_DEVICES = "remove_devices_screen"
    const val REGISTER_DEVICE = "register_device_screen"
    const val DEVICE_DETAILS = "device_details_screen"
    const val IMAGE_PICKER = "image_picker_screen"
    const val NEW_CONVERSATION = "new_conversation_screen"
    const val ONGOING_CALL = "ongoing_call_screen"
    const val INITIATING_CALL = "initiating_call_screen"
    const val INCOMING_CALL = "incoming_call_screen"
    const val MEDIA_GALLERY = "media_gallery"
    const val NETWORK_SETTINGS = "network_settings_screen"
    const val IMPORT_MEDIA = "import_media"
}

const val EXTRA_USER_ID = "extra_user_id"
const val EXTRA_USER_NAME = "extra_user_name"
const val EXTRA_USER_DOMAIN = "extra_user_domain"
const val EXTRA_USER_HANDLE = "extra_user_handle"
const val EXTRA_CONNECTION_STATE = "extra_connection_state"

const val EXTRA_NEW_EMAIL = "extra_new_email"

const val EXTRA_CONVERSATION_ID = "extra_conversation_id"
const val EXTRA_CREATE_ACCOUNT_FLOW_TYPE = "extra_create_account_flow_type"
const val EXTRA_IMAGE_DATA = "extra_image_data"
const val EXTRA_MESSAGE_ID = "extra_message_id"
const val EXTRA_IS_SELF_MESSAGE = "extra_is_self_message"
const val EXTRA_MESSAGE_TO_DELETE_ID = "extra_message_to_delete"
const val EXTRA_MESSAGE_TO_DELETE_IS_SELF = "extra_message_to_delete_is_self"
const val EXTRA_DEVICE_ID = "extra_device_id"
const val EXTRA_ON_MESSAGE_REACTED = "extra_on_message_reacted"
const val EXTRA_ON_MESSAGE_REPLIED = "extra_on_message_replied"
const val EXTRA_ON_MESSAGE_DETAILS_CLICKED = "extra_on_message_details_clicked"
const val EXTRA_CONNECTION_IGNORED_USER_NAME = "extra_connection_ignored_user_name"
const val EXTRA_GROUP_DELETED_NAME = "extra_group_deleted_name"
const val EXTRA_GROUP_NAME_CHANGED = "extra_group_name_changed"
const val EXTRA_LEFT_GROUP = "extra_left_group"
const val EXTRA_SETTINGS_DISPLAY_NAME_CHANGED = "extra_settings_display_name_changed"

const val EXTRA_BACK_NAVIGATION_ARGUMENTS = "extra_back_navigation_arguments"

const val EXTRA_EDIT_GUEST_ACCESS_PARAMS = "extra_edit_guest_access_params"
const val EXTRA_BOT_SERVICE_ID = "extra_bot_service_id"
const val EXTRA_IS_SERVICES_ALLOWED = "extra_is_services_allowed"

const val EXTRA_SSO_LOGIN_RESULT = "sso_login_result"

fun NavigationItem.isExternalRoute() = this.getRouteWithArgs().startsWith("http")

data class ContentParams(
    val navBackStackEntry: NavBackStackEntry,
    val arguments: List<Any?> = emptyList()
)

enum class ScreenMode {
    KEEP_ON, // keep screen on while that NavigationItem is visible (i.e CallScreen)
    WAKE_UP, // wake up the device on navigating to that NavigationItem (i.e IncomingCall)
    NONE // do not wake up and allow device to sleep
}
