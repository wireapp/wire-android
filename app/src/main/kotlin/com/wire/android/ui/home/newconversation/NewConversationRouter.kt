package com.wire.android.ui.home.newconversation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wire.android.R
import com.wire.android.ui.common.NavigationIconType
import com.wire.android.ui.common.topappbar.AppTopBarWithSearchBarLayout
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.newconversation.contacts.ContactsScreen
import com.wire.android.ui.home.newconversation.search.SearchPeopleScreen
import com.wire.android.ui.home.newconversation.search.SearchPeopleViewModel


@Composable
fun NewConversationRouter(newConversationViewModel: NewConversationViewModel = hiltViewModel()) {
    val newConversationState = rememberNewConversationState()

    AppTopBarWithSearchBarLayout(
        scrollPosition = newConversationState.scrollPosition,
        searchBarHint = stringResource(R.string.label_search_people),
        searchQuery = newConversationState.searchQuery,
        onSearchQueryChanged = {
            newConversationState.searchQuery = it
        },
        onSearchClicked = { newConversationState.navigateToSearch() },
        onCloseSearchClicked = {
            newConversationState.clearSearchQuery()
            newConversationState.navigateBack()
        },
        appTopBar = {
            WireCenterAlignedTopAppBar(
                elevation = 0.dp,
                title = stringResource(R.string.label_new_conversation),
                navigationIconType = NavigationIconType.Close,
                onNavigationPressed = { newConversationViewModel.close() }
            )
        },
        content = {
            NavHost(newConversationState.navController, startDestination = "contacts") {
                composable(
                    route = "contacts",
                    content = {
                        ContactsScreen(onScrollPositionChanged = { newConversationState.updateScrollPosition(it) })
                    }
                )
                composable(
                    route = "search_people",
                    content = {
                        val searchPeopleViewModel: SearchPeopleViewModel = hiltViewModel()

                        LaunchedEffect(newConversationState.searchQuery) {
                            searchPeopleViewModel.search(newConversationState.searchQuery)
                        }

                        SearchPeopleScreen(
                            searchPeopleState = searchPeopleViewModel.state,
                            onScrollPositionChanged = { newConversationState.updateScrollPosition(it) }
                        )
                    }
                )
            }
        }
    )
}

class NewConversationScreenState(
    val navController: NavHostController
) {

    var searchQuery by mutableStateOf("")

    var scrollPosition by mutableStateOf(0)
        private set

    fun updateScrollPosition(newScrollPosition: Int) {
        scrollPosition = newScrollPosition
    }

    fun navigateToSearch() {
        navController.navigate("search_people")
    }

    fun navigateBack() {
        navController.navigate("contacts")
    }

    fun clearSearchQuery() {
        searchQuery = ""
    }
}

@Composable
private fun rememberNewConversationState(
    navController: NavHostController = rememberNavController()
): NewConversationScreenState {
    return remember {
        NewConversationScreenState(navController)
    }
}
