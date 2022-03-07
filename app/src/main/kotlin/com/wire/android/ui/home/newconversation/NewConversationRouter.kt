package com.wire.android.ui.home.newconversation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wire.android.R
import com.wire.android.ui.common.NavigationIconType
import com.wire.android.ui.home.newconversation.contacts.ContactsScreen
import com.wire.android.ui.home.newconversation.search.SearchPeopleScreen


@Composable
fun NewConversationRouter(newConversationViewModel: NewConversationViewModel = hiltViewModel()) {
    val newConversationState = rememberNewConversationState()

    ConstraintLayout(Modifier.fillMaxSize()) {
        val (topBarRef, contentRef) = createRefs()

        AppTopBarWithSearchBar(
            scrollPosition = newConversationState.scrollPosition,
            searchBarHint = stringResource(R.string.label_search_people),
            topBarTitle = stringResource(R.string.label_new_conversation),
            navigationIconType = NavigationIconType.Close,
            searchQuery = newConversationState.searchQuery,
            onSearchQueryChanged = {
                newConversationState.searchQuery = it
            },
            onSearchClicked = { newConversationState.navigateToSearch() },
            onCloseSearchClicked = {
                newConversationState.clearSearchQuery()
                newConversationState.navigateBack()
            },
            onNavigateBackClicked = { newConversationViewModel.close() },
            modifier = Modifier.constrainAs(topBarRef) {
                top.linkTo(parent.top)
                bottom.linkTo(contentRef.top)
            }
        )

        NavHost(newConversationState.navController, startDestination = "contacts", modifier = Modifier.constrainAs(contentRef) {
            top.linkTo(topBarRef.bottom)
            bottom.linkTo(parent.bottom)

            height = Dimension.fillToConstraints
        }) {
            composable(
                route = "contacts",
                content = {
                    ContactsScreen(onScrollPositionChanged = { newConversationState.updateScrollPosition(it) })
                }
            )
            composable(
                route = "search_people",
                content = {
                    SearchPeopleScreen(
                        searchQuery = newConversationState.searchQuery,
                        onScrollPositionChanged = { newConversationState.updateScrollPosition(it) })
                }
            )
        }
    }

}

class NewConversationState(
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
): NewConversationState {
    return remember {
        NewConversationState(navController)
    }
}
