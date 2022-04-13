package com.wire.android.ui.home.newconversation.common


sealed class Screen(val route: String) {
    object NewGroupNameScreen : Screen("new_group_name")
    object SearchListNavHostScreens : Screen("search_list_nav_host")
}

sealed class SearchListScreens(val route: String) {
    object KnownContactsScreen : SearchListScreens("known_contacts")
    object SearchPeopleScreen : SearchListScreens("search_people")
}
