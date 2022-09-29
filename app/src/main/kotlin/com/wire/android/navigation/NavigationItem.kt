package com.wire.android.navigation

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.navDeepLink
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
import com.wire.android.navigation.NavigationItemDestinationsRoutes.GROUP_CONVERSATION_ALL_PARTICIPANTS
import com.wire.android.navigation.NavigationItemDestinationsRoutes.GROUP_CONVERSATION_DETAILS
import com.wire.android.navigation.NavigationItemDestinationsRoutes.HOME
import com.wire.android.navigation.NavigationItemDestinationsRoutes.IMAGE_PICKER
import com.wire.android.navigation.NavigationItemDestinationsRoutes.INCOMING_CALL
import com.wire.android.navigation.NavigationItemDestinationsRoutes.INITIATING_CALL
import com.wire.android.navigation.NavigationItemDestinationsRoutes.LOGIN
import com.wire.android.navigation.NavigationItemDestinationsRoutes.MEDIA_GALLERY
import com.wire.android.navigation.NavigationItemDestinationsRoutes.NETWORK_SETTINGS
import com.wire.android.navigation.NavigationItemDestinationsRoutes.NEW_CONVERSATION
import com.wire.android.navigation.NavigationItemDestinationsRoutes.ONGOING_CALL
import com.wire.android.navigation.NavigationItemDestinationsRoutes.OTHER_USER_PROFILE
import com.wire.android.navigation.NavigationItemDestinationsRoutes.REGISTER_DEVICE
import com.wire.android.navigation.NavigationItemDestinationsRoutes.REMOVE_DEVICES
import com.wire.android.navigation.NavigationItemDestinationsRoutes.REPORT_BUG
import com.wire.android.navigation.NavigationItemDestinationsRoutes.SELF_USER_PROFILE
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
import com.wire.android.ui.calling.OngoingCallScreen
import com.wire.android.ui.calling.incoming.IncomingCallScreen
import com.wire.android.ui.calling.initiating.InitiatingCallScreen
import com.wire.android.ui.debugscreen.DebugScreen
import com.wire.android.ui.home.HomeScreen
import com.wire.android.ui.home.conversations.ConversationScreen
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsScreen
import com.wire.android.ui.home.conversations.details.participants.GroupConversationAllParticipantsScreen
import com.wire.android.ui.home.conversations.search.AddMembersSearchRouter
import com.wire.android.ui.home.gallery.MediaGalleryScreen
import com.wire.android.ui.home.newconversation.NewConversationRouter
import com.wire.android.ui.home.settings.appsettings.AppSettingsScreen
import com.wire.android.ui.home.settings.appsettings.networkSettings.NetworkSettingsScreen
import com.wire.android.ui.home.settings.backup.BackupAndRestoreScreen
import com.wire.android.ui.userprofile.avatarpicker.AvatarPickerScreen
import com.wire.android.ui.userprofile.other.OtherUserProfileScreen
import com.wire.android.ui.userprofile.self.SelfUserProfileScreen
import com.wire.android.util.deeplink.DeepLinkProcessor
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import io.github.esentsov.PackagePrivate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
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

    Login(
        primaryRoute = LOGIN,
        canonicalRoute = LOGIN,
        content = { contentParams ->
            val ssoLoginResult = contentParams.arguments.filterIsInstance<DeepLinkResult.SSOLogin>().firstOrNull()
            LoginScreen(ssoLoginResult)
        },
        animationConfig = NavigationAnimationConfig.CustomAnimation(smoothSlideInFromRight(), smoothSlideOutFromLeft())
    ),

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
        content = {
            HomeScreen(
                hiltSavedStateViewModel(it.navBackStackEntry),
                hiltViewModel(),
                hiltViewModel(),
                hiltViewModel()
            )
        },
        animationConfig = NavigationAnimationConfig.DelegatedAnimation
    ),

    AppSettings(
        primaryRoute = APP_SETTINGS,
        content = { AppSettingsScreen() },
    ),

    BackupAndRestore(
        primaryRoute = BACKUP_AND_RESTORE,
        content = { BackupAndRestoreScreen() }
    ),

    Debug(
        primaryRoute = DEBUG,
        content = { DebugScreen() },
    ),

    NetworkSettings(
        primaryRoute = NETWORK_SETTINGS,
        content = { NetworkSettingsScreen() },
    ),

    Support(
        primaryRoute = BuildConfig.SUPPORT_URL,
        content = { },
    ),

    ReportBug(
        primaryRoute = REPORT_BUG,
        content = { },
    ),

    SelfUserProfile(
        primaryRoute = SELF_USER_PROFILE,
        content = { SelfUserProfileScreen() },
        animationConfig = NavigationAnimationConfig.CustomAnimation(expandInToView(), shrinkOutFromView())
    ),

    OtherUserProfile(
        primaryRoute = OTHER_USER_PROFILE,
        canonicalRoute = "$OTHER_USER_PROFILE?$EXTRA_USER_ID={$EXTRA_USER_ID}&$EXTRA_CONVERSATION_ID={$EXTRA_CONVERSATION_ID}",
        deepLinks = listOf(
            navDeepLink {
                uriPattern = "${DeepLinkProcessor.DEEP_LINK_SCHEME}://" +
                        "${DeepLinkProcessor.OTHER_USER_PROFILE_DEEPLINK_HOST}/" +
                        "{$EXTRA_USER_ID}"
            }
        ),
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

    ProfileImagePicker(
        primaryRoute = IMAGE_PICKER,
        content = { AvatarPickerScreen(hiltViewModel()) },
    ),

    Conversation(
        primaryRoute = CONVERSATION,
        canonicalRoute = "$CONVERSATION?$EXTRA_CONVERSATION_ID={$EXTRA_CONVERSATION_ID}",
        deepLinks = listOf(
            navDeepLink {
                uriPattern = "${DeepLinkProcessor.DEEP_LINK_SCHEME}://" +
                        "${DeepLinkProcessor.CONVERSATION_DEEPLINK_HOST}/" +
                        "{$EXTRA_CONVERSATION_ID}"
            }
        ),
        content = {
            ConversationScreen(
                messageComposerViewModel = hiltSavedStateViewModel(it.navBackStackEntry),
                conversationCallViewModel = hiltSavedStateViewModel(it.navBackStackEntry),
                conversationInfoViewModel = hiltSavedStateViewModel(it.navBackStackEntry),
                conversationMessagesViewModel = hiltSavedStateViewModel(it.navBackStackEntry),
                commonTopAppBarViewModel = hiltViewModel()
            )
        },
        animationConfig = NavigationAnimationConfig.NoAnimation
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String {
            val conversationIdString: String = arguments.filterIsInstance<ConversationId>().firstOrNull()?.toString()
                ?: "{$EXTRA_CONVERSATION_ID}"
            return "$CONVERSATION?$EXTRA_CONVERSATION_ID=$conversationIdString"
        }
    },

    GroupConversationDetails(
        primaryRoute = GROUP_CONVERSATION_DETAILS,
        canonicalRoute = "$GROUP_CONVERSATION_DETAILS/{$EXTRA_CONVERSATION_ID}",
        content = { GroupConversationDetailsScreen(hiltViewModel()) },
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String = routeWithConversationIdArg(arguments)
    },

    AddConversationParticipants(
        primaryRoute = ADD_CONVERSATION_PARTICIPANTS,
        canonicalRoute = "$ADD_CONVERSATION_PARTICIPANTS/{$EXTRA_CONVERSATION_ID}",
        content = { AddMembersSearchRouter() }
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String = routeWithConversationIdArg(arguments)
    },

    GroupConversationAllParticipants(
        primaryRoute = GROUP_CONVERSATION_ALL_PARTICIPANTS,
        canonicalRoute = "$GROUP_CONVERSATION_ALL_PARTICIPANTS/{$EXTRA_CONVERSATION_ID}",
        content = { GroupConversationAllParticipantsScreen(hiltViewModel()) },
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String = routeWithConversationIdArg(arguments)
    },

    NewConversation(
        primaryRoute = NEW_CONVERSATION,
        canonicalRoute = NEW_CONVERSATION,
        content = { NewConversationRouter() }
    ),

    OngoingCall(
        primaryRoute = ONGOING_CALL,
        canonicalRoute = "$ONGOING_CALL/{$EXTRA_CONVERSATION_ID}",
        content = { OngoingCallScreen() },
        screenMode = ScreenMode.WAKE_UP,
        animationConfig = NavigationAnimationConfig.CustomAnimation(null, shrinkOutFromView())
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
        deepLinks = listOf(
            navDeepLink {
                uriPattern = "${DeepLinkProcessor.DEEP_LINK_SCHEME}://" +
                        "${DeepLinkProcessor.INCOMING_CALL_DEEPLINK_HOST}/" +
                        "{$EXTRA_CONVERSATION_ID}"
            }
        ),
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
    };

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

    companion object {
        private val map: Map<String, NavigationItem> = values().associateBy { it.primaryRoute }

        fun fromRoute(fullRoute: String): NavigationItem? {
            val splitByQuestion = fullRoute.split("?")
            val splitBySlash = fullRoute.split("/")

            val primaryRoute = when {
                splitByQuestion.size > 1 -> splitByQuestion[0]
                splitBySlash.size > 1 -> splitBySlash[0]
                else -> fullRoute
            }

            return map[primaryRoute]
        }
    }
}

object NavigationItemDestinationsRoutes {
    const val WELCOME = "welcome_screen"
    const val LOGIN = "login_screen"
    const val CREATE_TEAM = "create_team_screen"
    const val CREATE_PERSONAL_ACCOUNT = "create_personal_account_screen"
    const val CREATE_ACCOUNT_USERNAME = "create_account_username_screen"
    const val CREATE_ACCOUNT_SUMMARY = "create_account_summary_screen"
    const val HOME = "home_landing_screen"
    const val SELF_USER_PROFILE = "self_user_profile_screen"
    const val OTHER_USER_PROFILE = "other_user_profile_screen"
    const val CONVERSATION = "detailed_conversation_screen"
    const val GROUP_CONVERSATION_DETAILS = "group_conversation_details_screen"
    const val GROUP_CONVERSATION_ALL_PARTICIPANTS = "group_conversation_all_participants_screen"
    const val ADD_CONVERSATION_PARTICIPANTS = "add_conversation_participants"
    const val APP_SETTINGS = "app_settings_screen"
    const val BACKUP_AND_RESTORE = "backup_and_restore_screen"
    const val DEBUG = "debug_screen"
    const val REPORT_BUG = "report_bug"
    const val REMOVE_DEVICES = "remove_devices_screen"
    const val REGISTER_DEVICE = "register_device_screen"
    const val IMAGE_PICKER = "image_picker_screen"
    const val NEW_CONVERSATION = "new_conversation_screen"
    const val ONGOING_CALL = "ongoing_call_screen"
    const val INITIATING_CALL = "initiating_call_screen"
    const val INCOMING_CALL = "incoming_call_screen"
    const val MEDIA_GALLERY = "media_gallery"
    const val NETWORK_SETTINGS = "network_settings_screen"
}

const val EXTRA_USER_ID = "extra_user_id"
const val EXTRA_USER_DOMAIN = "extra_user_domain"

const val EXTRA_CONVERSATION_ID = "extra_conversation_id"
const val EXTRA_CREATE_ACCOUNT_FLOW_TYPE = "extra_create_account_flow_type"
const val EXTRA_IMAGE_DATA = "extra_image_data"
const val EXTRA_MESSAGE_TO_DELETE_ID = "extra_message_to_delete"
const val EXTRA_MESSAGE_TO_DELETE_IS_SELF = "extra_message_to_delete_is_self"

const val EXTRA_CONNECTION_IGNORED_USER_NAME = "extra_connection_ignored_user_name"
const val EXTRA_GROUP_DELETED_NAME = "extra_group_deleted_name"
const val EXTRA_LEFT_GROUP = "extra_left_group"

const val EXTRA_BACK_NAVIGATION_ARGUMENTS = "extra_back_navigation_arguments"

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
