package com.wire.android.navigation

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import com.wire.android.BuildConfig
import com.wire.android.navigation.NavigationItemDestinationsRoutes.CONVERSATION
import com.wire.android.navigation.NavigationItemDestinationsRoutes.CREATE_PERSONAL_ACCOUNT
import com.wire.android.navigation.NavigationItemDestinationsRoutes.CREATE_TEAM
import com.wire.android.navigation.NavigationItemDestinationsRoutes.HOME
import com.wire.android.navigation.NavigationItemDestinationsRoutes.IMAGE_PICKER
import com.wire.android.navigation.NavigationItemDestinationsRoutes.LOGIN
import com.wire.android.navigation.NavigationItemDestinationsRoutes.REMOVE_DEVICES
import com.wire.android.navigation.NavigationItemDestinationsRoutes.SETTINGS
import com.wire.android.navigation.NavigationItemDestinationsRoutes.USER_PROFILE
import com.wire.android.navigation.NavigationItemDestinationsRoutes.WELCOME
import com.wire.android.ui.authentication.create.personalaccount.CreatePersonalAccountScreen
import com.wire.android.ui.authentication.devices.RemoveDeviceScreen
import com.wire.android.ui.authentication.login.LoginScreen
import com.wire.android.ui.authentication.welcome.WelcomeScreen
import com.wire.android.ui.common.UnderConstructionScreen
import com.wire.android.ui.home.HomeScreen
import com.wire.android.ui.home.conversations.ConversationScreen
import com.wire.android.ui.settings.SettingsScreen
import com.wire.android.ui.userprofile.UserProfileScreen
import com.wire.android.ui.userprofile.image.AvatarPickerScreen
import com.wire.kalium.logic.configuration.ServerConfig
import com.wire.kalium.logic.data.conversation.ConversationId
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
        content = { WelcomeScreen() }
    ),

    Login(
        primaryRoute = LOGIN,
        content = { contentParams ->
            val serverConfig = contentParams.arguments.filterIsInstance<ServerConfig>().firstOrNull()
            LoginScreen(serverConfig ?: ServerConfig.DEFAULT)
        }
    ),

    CreateTeam(
        primaryRoute = CREATE_TEAM,
        content = { UnderConstructionScreen("Create Team Screen") }
    ),

    CreatePersonalAccount(
        primaryRoute = CREATE_PERSONAL_ACCOUNT,
        content = { CreatePersonalAccountScreen(ServerConfig.STAGING) }
    ),

    RemoveDevices(
        primaryRoute = REMOVE_DEVICES,
        content = { RemoveDeviceScreen() }
    ),

    Home(
        primaryRoute = HOME,
        content = { HomeScreen(it.navBackStackEntry.arguments?.getString(EXTRA_HOME_TAB_ITEM), hiltViewModel()) },
    ),

    Settings(
        primaryRoute = SETTINGS,
        content = { SettingsScreen() },
    ),

    Support(
        primaryRoute = BuildConfig.SUPPORT_URL,
        content = { },
    ),

    UserProfile(
        primaryRoute = USER_PROFILE,
        canonicalRoute = "$USER_PROFILE/{$EXTRA_USER_ID}",
        content = { UserProfileScreen() },
        animationConfig = NavigationAnimationConfig.CustomAnimation(smoothSlideInFromRight(), smoothSlideOutFromLeft())
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String {
            val userProfileId: String? = arguments.filterIsInstance<String>().firstOrNull()
            return if (userProfileId != null) "$primaryRoute/$userProfileId" else primaryRoute
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
    const val HOME = "home_landing_screen"
    const val USER_PROFILE = "user_profile_screen"
    const val CONVERSATION = "detailed_conversation_screen"
    const val SETTINGS = "settings_screen"
    const val REMOVE_DEVICES = "remove_devices_screen"
    const val IMAGE_PICKER = "image_picker_screen"
}

private const val EXTRA_HOME_TAB_ITEM = "extra_home_tab_item"
private const val EXTRA_USER_ID = "extra_user_id"
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
