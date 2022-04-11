package com.wire.android.navigation

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import com.wire.android.BuildConfig
import com.wire.android.navigation.NavigationItemDestinationsRoutes.CONVERSATION
import com.wire.android.navigation.NavigationItemDestinationsRoutes.CREATE_ACCOUNT_USERNAME
import com.wire.android.navigation.NavigationItemDestinationsRoutes.CREATE_PERSONAL_ACCOUNT
import com.wire.android.navigation.NavigationItemDestinationsRoutes.CREATE_TEAM
import com.wire.android.navigation.NavigationItemDestinationsRoutes.HOME
import com.wire.android.navigation.NavigationItemDestinationsRoutes.IMAGE_PICKER
import com.wire.android.navigation.NavigationItemDestinationsRoutes.LOGIN
import com.wire.android.navigation.NavigationItemDestinationsRoutes.NEW_CONVERSATION
import com.wire.android.navigation.NavigationItemDestinationsRoutes.OTHER_USER_PROFILE
import com.wire.android.navigation.NavigationItemDestinationsRoutes.REGISTER_DEVICE
import com.wire.android.navigation.NavigationItemDestinationsRoutes.REMOVE_DEVICES
import com.wire.android.navigation.NavigationItemDestinationsRoutes.SELF_USER_PROFILE
import com.wire.android.navigation.NavigationItemDestinationsRoutes.SETTINGS
import com.wire.android.navigation.NavigationItemDestinationsRoutes.WELCOME
import com.wire.android.navigation.NavigationItemDestinationsRoutes.ONGOING_CALL
import com.wire.android.ui.authentication.create.personalaccount.CreatePersonalAccountScreen
import com.wire.android.ui.authentication.create.team.CreateTeamScreen
import com.wire.android.ui.authentication.create.username.CreateAccountUsernameScreen
import com.wire.android.ui.authentication.devices.register.RegisterDeviceScreen
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceScreen
import com.wire.android.ui.authentication.login.LoginScreen
import com.wire.android.ui.authentication.welcome.WelcomeScreen
import com.wire.android.ui.calling.OngoingCallScreen
import com.wire.android.ui.home.HomeScreen
import com.wire.android.ui.home.conversations.ConversationScreen
import com.wire.android.ui.home.newconversation.NewConversationRouter
import com.wire.android.ui.settings.SettingsScreen
import com.wire.android.ui.userprofile.avatarpicker.AvatarPickerScreen
import com.wire.android.ui.userprofile.other.OtherUserProfileScreen
import com.wire.android.ui.userprofile.self.SelfUserProfileScreen
import com.wire.kalium.logic.configuration.ServerConfig
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import io.github.esentsov.PackagePrivate

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class
)
/**
 * The class encapsulating the app main navigational items.
 */
enum class NavigationItem(
    @PackagePrivate
    internal val primaryRoute: String,
    private val canonicalRoute: String = primaryRoute,
    open val content: @Composable (ContentParams) -> Unit,
    val animationConfig: NavigationAnimationConfig = NavigationAnimationConfig.NoAnimation
) {
    Welcome(
        primaryRoute = WELCOME,
        content = { WelcomeScreen() },
        animationConfig = NavigationAnimationConfig.CustomAnimation(smoothSlideInFromRight(), smoothSlideOutFromLeft())
    ),

    Login(
        primaryRoute = LOGIN,
        content = { contentParams ->
            val serverConfig = contentParams.arguments.filterIsInstance<ServerConfig>().firstOrNull()
            LoginScreen(serverConfig ?: ServerConfig.DEFAULT)
        },
        animationConfig = NavigationAnimationConfig.CustomAnimation(smoothSlideInFromRight(), smoothSlideOutFromLeft())
    ),

    CreateTeam(
        primaryRoute = CREATE_TEAM,
        content = { CreateTeamScreen(serverConfig = ServerConfig.STAGING) },
        animationConfig = NavigationAnimationConfig.CustomAnimation(smoothSlideInFromRight(), smoothSlideOutFromLeft())
    ),

    CreatePersonalAccount(
        primaryRoute = CREATE_PERSONAL_ACCOUNT,
        content = { CreatePersonalAccountScreen(ServerConfig.STAGING) },
        animationConfig = NavigationAnimationConfig.CustomAnimation(smoothSlideInFromRight(), smoothSlideOutFromLeft())
    ),

    CreateUsername(
        primaryRoute = CREATE_ACCOUNT_USERNAME,
        content = { CreateAccountUsernameScreen() },
        animationConfig = NavigationAnimationConfig.CustomAnimation(smoothSlideInFromRight(), smoothSlideOutFromLeft())
    ),

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
        content = { HomeScreen(it.navBackStackEntry.arguments?.getString(EXTRA_HOME_TAB_ITEM), hiltViewModel()) },
        animationConfig = NavigationAnimationConfig.DelegatedAnimation
    ),

    Settings(
        primaryRoute = SETTINGS,
        content = { SettingsScreen() },
    ),

    Support(
        primaryRoute = BuildConfig.SUPPORT_URL,
        content = { },
    ),

    SelfUserProfile(
        primaryRoute = SELF_USER_PROFILE,
        canonicalRoute = "$SELF_USER_PROFILE/{$EXTRA_USER_ID}",
        content = { SelfUserProfileScreen() },
        animationConfig = NavigationAnimationConfig.CustomAnimation(expandInToView(), shrinkOutFromView())
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String {
            val userProfileId: String? = arguments.filterIsInstance<String>().firstOrNull()
            return if (userProfileId != null) "$primaryRoute/$userProfileId" else primaryRoute
        }
    },

    //TODO: internal is here until we can get the ConnectionStatus from the user
    // for now it is just to be able to proceed forward
    OtherUserProfile(
        primaryRoute = OTHER_USER_PROFILE,
        canonicalRoute = "$OTHER_USER_PROFILE/{$EXTRA_USER_ID}/{$EXTRA_CONNECTED_STATUS}",
        content = { OtherUserProfileScreen() },
        animationConfig = NavigationAnimationConfig.CustomAnimation(smoothSlideInFromRight(), smoothSlideOutFromLeft())
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String {
            val userProfileId: String? = arguments.filterIsInstance<String>().firstOrNull()
            val internal: Boolean? = arguments.filterIsInstance<Boolean>().firstOrNull()

            return "$primaryRoute/${userProfileId!!}/${internal!!}"
        }
    },

    ProfileImagePicker(
        primaryRoute = IMAGE_PICKER,
        content = { AvatarPickerScreen(hiltViewModel()) },
    ),

    Conversation(
        primaryRoute = CONVERSATION,
        canonicalRoute = "$CONVERSATION/{$EXTRA_CONVERSATION_ID}",
        content = { ConversationScreen(hiltViewModel()) }
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String {
            val conversationId: ConversationId? = arguments.filterIsInstance<ConversationId>().firstOrNull()
            return conversationId?.run { "$primaryRoute/${mapIntoArgumentString()}" } ?: primaryRoute
        }
    },

    NewConversation(
        primaryRoute = NEW_CONVERSATION,
        canonicalRoute = NEW_CONVERSATION,
        content = { NewConversationRouter() }
    ),

    OngoingCall(
        primaryRoute = ONGOING_CALL,
        canonicalRoute = "$ONGOING_CALL/{$EXTRA_CONVERSATION_ID}",
        content = { OngoingCallScreen() }
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String {
            val conversationId: ConversationId? = arguments.filterIsInstance<ConversationId>().firstOrNull()
            println("getRouteWithArgs = $conversationId")
            return conversationId?.run { "$primaryRoute/${mapIntoArgumentString()}" } ?: primaryRoute
        }
    };

    /**
     * The item theoretical route. If the route includes a route ID, this method will return the route with the placeholder.
     * This should only be accessed to create the initial navigation graph, not as a navigation item route
     */
    @PackagePrivate
    fun getCanonicalRoute() = canonicalRoute

    open fun getRouteWithArgs(arguments: List<Any> = emptyList()): String = primaryRoute

    companion object {
        @OptIn(ExperimentalMaterialApi::class)
        private val map: Map<String, NavigationItem> = values().associateBy { it.canonicalRoute }

        fun fromRoute(route: String?): NavigationItem? = map[route]
    }
}

object NavigationItemDestinationsRoutes {
    const val WELCOME = "welcome_screen"
    const val LOGIN = "login_screen"
    const val CREATE_TEAM = "create_team_screen"
    const val CREATE_PERSONAL_ACCOUNT = "create_personal_account_screen"
    const val CREATE_ACCOUNT_USERNAME = "create_account_username_screen"
    const val HOME = "home_landing_screen"
    const val SELF_USER_PROFILE = "self_user_profile_screen"
    const val OTHER_USER_PROFILE = "other_user_profile_screen"
    const val CONVERSATION = "detailed_conversation_screen"
    const val SETTINGS = "settings_screen"
    const val REMOVE_DEVICES = "remove_devices_screen"
    const val REGISTER_DEVICE = "register_device_screen"
    const val IMAGE_PICKER = "image_picker_screen"
    const val NEW_CONVERSATION = "new_conversation_screen"
    const val ONGOING_CALL = "ongoing_call_screen"
}

private const val EXTRA_HOME_TAB_ITEM = "extra_home_tab_item"
const val EXTRA_USER_ID = "extra_user_id"

//TODO: internal is here untill we can get the ConnectionStatus from the user
// for now it is just to be able to proceed forward
const val EXTRA_CONNECTED_STATUS = "extra_connected_status"
const val EXTRA_CONVERSATION_ID = "extra_conversation_id"

fun NavigationItem.isExternalRoute() = this.getRouteWithArgs().startsWith("http")

private fun QualifiedID.mapIntoArgumentString(): String = "$domain@$value"

fun String.parseIntoQualifiedID(): QualifiedID {
    val components = split("@")
    return QualifiedID(components.last(), components.first())
}

data class ContentParams(
    val navBackStackEntry: NavBackStackEntry,
    val arguments: List<Any> = emptyList()
)
