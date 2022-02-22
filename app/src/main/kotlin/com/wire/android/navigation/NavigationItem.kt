package com.wire.android.navigation

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.wire.android.BuildConfig
import com.wire.android.navigation.NavigationItemDestinationsRoutes.CONVERSATION
import com.wire.android.navigation.NavigationItemDestinationsRoutes.CREATE_ENTERPRISE_ACCOUNT
import com.wire.android.navigation.NavigationItemDestinationsRoutes.CREATE_PRIVATE_ACCOUNT
import com.wire.android.navigation.NavigationItemDestinationsRoutes.HOME
import com.wire.android.navigation.NavigationItemDestinationsRoutes.IMAGE_PICKER
import com.wire.android.navigation.NavigationItemDestinationsRoutes.LOGIN
import com.wire.android.navigation.NavigationItemDestinationsRoutes.REMOVE_DEVICES
import com.wire.android.navigation.NavigationItemDestinationsRoutes.SETTINGS
import com.wire.android.navigation.NavigationItemDestinationsRoutes.USER_PROFILE
import com.wire.android.navigation.NavigationItemDestinationsRoutes.WELCOME
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
    private val canonicalRoute: String,
    val arguments: List<NamedNavArgument> = emptyList(),
    open val content: @Composable (NavBackStackEntry) -> Unit
    // TODO add animations here
) {
    Welcome(
        primaryRoute = WELCOME,
        canonicalRoute = WELCOME,
        content = { WelcomeScreen() }
    ),

    Login(
        primaryRoute = LOGIN,
        canonicalRoute = LOGIN,
        content = { LoginScreen(ServerConfig.STAGING) }
    ),

    CreateEnterpriseAccount(
        primaryRoute = CREATE_ENTERPRISE_ACCOUNT,
        canonicalRoute = CREATE_ENTERPRISE_ACCOUNT,
        content = { UnderConstructionScreen("Create Enterprise Account Screen") }
    ),

    CreatePrivateAccount(
        primaryRoute = CREATE_PRIVATE_ACCOUNT,
        canonicalRoute = CREATE_PRIVATE_ACCOUNT,
        content = { UnderConstructionScreen("Create Private Account Screen") }
    ),

    RemoveDevices(
        primaryRoute = REMOVE_DEVICES,
        canonicalRoute = REMOVE_DEVICES,
        content = { RemoveDeviceScreen() }
    ),

    Home(
        primaryRoute = HOME,
        canonicalRoute = HOME,
        content = { HomeScreen(it.arguments?.getString(EXTRA_HOME_TAB_ITEM), hiltViewModel()) },
        arguments = listOf(
            navArgument(EXTRA_HOME_TAB_ITEM) { type = NavType.StringType }
        )
    ),

    Settings(
        primaryRoute = SETTINGS,
        canonicalRoute = SETTINGS,
        content = { SettingsScreen() },
    ),

    Support(
        primaryRoute = BuildConfig.SUPPORT_URL,
        canonicalRoute = BuildConfig.SUPPORT_URL,
        content = { },
    ),

    UserProfile(
        primaryRoute = USER_PROFILE,
        canonicalRoute = "$USER_PROFILE/{$EXTRA_USER_ID}",
        content = { UserProfileScreen() },
        arguments = listOf(
            navArgument(EXTRA_USER_ID) { type = NavType.StringType }
        )
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String {
            val userProfileId: String? = arguments.filterIsInstance<String>().firstOrNull()
            return if (userProfileId != null) "$primaryRoute/$userProfileId" else primaryRoute
        }
    },

    ProfileImagePicker(
        primaryRoute = IMAGE_PICKER,
        canonicalRoute = IMAGE_PICKER,
        content = { AvatarPickerScreen(hiltViewModel()) },
    ),

    Conversation(
        primaryRoute = CONVERSATION,
        canonicalRoute = "$CONVERSATION/{$EXTRA_CONVERSATION_ID}",
        content = { ConversationScreen(hiltViewModel()) },
        arguments = listOf(
            navArgument(EXTRA_CONVERSATION_ID) { type = NavType.StringType }
        )
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String {
            val conversationId: ConversationId? = arguments.filterIsInstance<ConversationId>().firstOrNull()
            return if (conversationId != null) "$primaryRoute/${conversationId.toJson()}" else primaryRoute
        }
    };

    /**
     * The item theoretical route. If the route includes a route ID, this method will return the route with the placeholder. This should be
     * only accessed to create the initial navigation graph, not to navigate to
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
    const val CREATE_ENTERPRISE_ACCOUNT = "create_enterprise_account_screen"
    const val CREATE_PRIVATE_ACCOUNT = "create_private_account_screen"
    const val HOME = "home_landing_screen"
    const val USER_PROFILE = "user_profile_screen"
    const val CONVERSATION = "detailed_conversation_screen"
    const val SETTINGS = "settings_screen"
    const val REMOVE_DEVICES = "remove_devices_screen"
    const val IMAGE_PICKER = "image_picker_screen"
}

private const val EXTRA_HOME_TAB_ITEM = "extra_home_tab_item"
private const val EXTRA_USER_ID = "extra_user_id"
private const val EXTRA_INITIAL_BITMAP = "extra_initial_bitmap"
private const val EXTRA_CONVERSATION_ID = "extra_conversation_id"

fun NavigationItem.isExternalRoute() = this.getRouteWithArgs().startsWith("http")
