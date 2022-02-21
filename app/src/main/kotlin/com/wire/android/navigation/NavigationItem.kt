package com.wire.android.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.wire.android.BuildConfig
import com.wire.android.ui.home.HomeScreen
import com.wire.android.ui.home.conversations.ConversationScreen
import com.wire.android.ui.settings.SettingsScreen
import com.wire.android.ui.userprofile.UserProfileScreen
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
    val primaryRoute: String,
    private val canonicalRoute: String,
    val arguments: List<NamedNavArgument> = emptyList(),
    open val content: @Composable (AnimatedVisibilityScope.(NavBackStackEntry) -> Unit),
    open val enterTransition: EnterTransition = EnterTransition.None,
    open val exitTransition: ExitTransition = ExitTransition.None
) {
    Home(
        primaryRoute = "home",
        canonicalRoute = "home",
        content = { HomeScreen(it.arguments?.getString(EXTRA_HOME_TAB_ITEM), hiltViewModel()) },
        arguments = listOf(
            navArgument(EXTRA_HOME_TAB_ITEM) { type = NavType.StringType }
        )
    ),

    Settings(
        primaryRoute = "settings",
        canonicalRoute = "settings",
        content = { SettingsScreen() },
    ),

    Support(
        primaryRoute = BuildConfig.SUPPORT_URL,
        canonicalRoute = BuildConfig.SUPPORT_URL,
        content = { },
    ),

    UserProfile(
        primaryRoute = "user_profile",
        canonicalRoute = "user_profile/{$EXTRA_USER_ID}",
        content = { UserProfileScreen(it.arguments?.getString(EXTRA_USER_ID), hiltViewModel()) },
        arguments = listOf(
            navArgument(EXTRA_USER_ID) { type = NavType.StringType }
        ),
        enterTransition = wireSlideInFromRight(),
        exitTransition = wireSlideOutFromLeft()
    ) {
        override fun getRouteWithArgs(arguments: List<Any>): String {
            val userProfileId: String? = arguments.filterIsInstance<String>().firstOrNull()
            return if (userProfileId != null) "$primaryRoute/$userProfileId" else primaryRoute
        }
    },

    Conversation(
        primaryRoute = "conversation",
        canonicalRoute = "conversation/{$EXTRA_CONVERSATION_ID}",
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

private const val EXTRA_HOME_TAB_ITEM = "extra_home_tab_item"
private const val EXTRA_USER_ID = "extra_user_id"
private const val EXTRA_CONVERSATION_ID = "extra_conversation_id"

fun NavigationItem.isExternalRoute() = this.getRouteWithArgs().startsWith("http")
