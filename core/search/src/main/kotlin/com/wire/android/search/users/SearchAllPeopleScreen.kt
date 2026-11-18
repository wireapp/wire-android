/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
 */

@file:Suppress("TooManyFunctions")

package com.wire.android.search.users

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import com.wire.android.model.Clickable
import com.wire.android.model.Contact
import com.wire.android.model.ItemActionType
import com.wire.android.search.R
import com.wire.android.search.SearchQuery
import com.wire.android.search.util.ui.PreviewMultipleThemes
import com.wire.android.search.widget.SearchFailureBox
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.CenteredCircularProgressBarIndicator
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.FolderType
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.keepOnTopWhenNotScrolled
import com.wire.android.util.ui.sectionWithElements
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import com.wire.android.ui.common.R as commonR

private const val DEFAULT_SEARCH_RESULT_ITEM_SIZE = 4

/**
 * state of the search screen
 * Loading - loading state
 * EmptyQuery - empty search query, show all users
 * NoResults - no results found
 * ShowResults - search results are found
 */
sealed interface SearchUiState {
    object Loading : SearchUiState
    object EmptyQuery : SearchUiState
    object NoResults : SearchUiState
    object ShowResults : SearchUiState
}

@Composable
fun SearchAllPeopleScreen(
    query: SearchQuery,
    contactsSearchResult: ImmutableList<Contact>,
    publicSearchResult: ImmutableList<Contact>,
    contactsSelectedSearchResult: ImmutableList<Contact>,
    isLoading: Boolean,
    isSearchActive: Boolean,
    actionType: ItemActionType,
    onChecked: (Boolean, Contact) -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
    emailInvitationsEnabled: Boolean = false,
    selectedEmails: Set<String> = emptySet(),
    onEmailChecked: (Boolean, String) -> Unit = { _, _ -> },
    firstContactFocusRequester: FocusRequester? = null,
    nextFocusRequester: FocusRequester? = null,
    selectedContactResultsExpanded: Boolean = false,
    onSelectedContactResultsExpansionChanged: (Boolean) -> Unit = {},
    contactResultsExpanded: Boolean = true,
    onContactResultsExpansionChanged: (Boolean) -> Unit = {},
    publicResultsExpanded: Boolean = true,
    onPublicResultsExpansionChanged: (Boolean) -> Unit = {},
    lazyListState: LazyListState = rememberLazyListState(),
) {

    val noSearchResults: Boolean by remember(publicSearchResult, contactsSearchResult, contactsSelectedSearchResult) {
        derivedStateOf {
            publicSearchResult.isEmpty() &&
                    contactsSearchResult.isEmpty() &&
                    contactsSelectedSearchResult.isEmpty()
        }
    }

    val uiState by remember(query, isLoading, noSearchResults, emailInvitationsEnabled) {
        derivedStateOf {
            when {
                isLoading -> SearchUiState.Loading
                emailInvitationsEnabled -> SearchUiState.ShowResults
                query.text.isBlank() && noSearchResults -> SearchUiState.EmptyQuery
                query.text.isNotBlank() && noSearchResults -> SearchUiState.NoResults
                else -> SearchUiState.ShowResults
            }
        }
    }

    when (uiState) {
        SearchUiState.Loading -> CenteredCircularProgressBarIndicator()
        SearchUiState.EmptyQuery -> EmptySearchQueryScreen()
        SearchUiState.NoResults -> SearchFailureBox(R.string.label_no_results_found)
        SearchUiState.ShowResults -> SearchResult(
            query = query,
            emailInvitationsEnabled = emailInvitationsEnabled,
            selectedEmails = selectedEmails,
            onEmailChecked = onEmailChecked,
            publicSearchResult = publicSearchResult,
            contactsSearchResult = contactsSearchResult,
            contactsSelectedSearchResult = contactsSelectedSearchResult,
            onChecked = onChecked,
            onOpenUserProfile = onOpenUserProfile,
            lazyListState = lazyListState,
            isSearchActive = isSearchActive,
            actionType = actionType,
            selectedContactResultsExpanded = selectedContactResultsExpanded,
            onSelectedContactResultsExpansionChanged = onSelectedContactResultsExpansionChanged,
            contactResultsExpanded = contactResultsExpanded,
            onContactResultsExpansionChanged = onContactResultsExpansionChanged,
            publicResultsExpanded = publicResultsExpanded,
            onPublicResultsExpansionChanged = onPublicResultsExpansionChanged,
            firstContactFocusRequester = firstContactFocusRequester,
            nextFocusRequester = nextFocusRequester,
        )
    }
}

@Composable
private fun SearchResult(
    query: SearchQuery,
    contactsSearchResult: ImmutableList<Contact>,
    publicSearchResult: ImmutableList<Contact>,
    contactsSelectedSearchResult: ImmutableList<Contact>,
    isSearchActive: Boolean,
    actionType: ItemActionType,
    onChecked: (Boolean, Contact) -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
    selectedContactResultsExpanded: Boolean,
    onSelectedContactResultsExpansionChanged: (Boolean) -> Unit,
    contactResultsExpanded: Boolean,
    onContactResultsExpansionChanged: (Boolean) -> Unit,
    publicResultsExpanded: Boolean,
    onPublicResultsExpansionChanged: (Boolean) -> Unit,
    emailInvitationsEnabled: Boolean = false,
    selectedEmails: Set<String> = emptySet(),
    onEmailChecked: (Boolean, String) -> Unit = { _, _ -> },
    lazyListState: LazyListState = rememberLazyListState(),
    firstContactFocusRequester: FocusRequester? = null,
    nextFocusRequester: FocusRequester? = null,
) {
    val searchPeopleScreenState = rememberSearchPeopleScreenState()
    val firstContactId = listOfNotNull(
        contactsSelectedSearchResult.firstOrNull()?.id.takeIf { selectedContactResultsExpanded },
        contactsSearchResult.firstOrNull()?.id.takeIf { contactResultsExpanded },
        publicSearchResult.firstOrNull()?.id.takeIf { publicResultsExpanded }
    ).firstOrNull()

    Column {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .onPreviewKeyEvent { event ->
                    if (event.isForwardTabKey && nextFocusRequester != null) {
                        nextFocusRequester.requestFocus()
                        true
                    } else {
                        false
                    }
                },
        ) {
            val selectedSearchResults = selectedEmails
                .filter { emailInvitationsEnabled && it.contains(query.text.trim(), ignoreCase = true) }
                .map { email ->
                    InternalSearchItem.EmailItem(
                        email = email,
                        selected = true,
                        message = UIText.StringResource(R.string.email_invitation_description),
                        enabled = true,
                    )
                }
                .plus(contactsSelectedSearchResult.map { contact -> InternalSearchItem.ContactItem(contact = contact, selected = true) })
                .toImmutableList()

            if (selectedSearchResults.isNotEmpty()) {
                internalSearchResults(
                    searchTitle = UIText.StringResource(commonR.string.label_selected, selectedSearchResults.size),
                    searchResult = selectedSearchResults,
                    query = query,
                    onChecked = onChecked,
                    onEmailChecked = onEmailChecked,
                    onOpenUserProfile = onOpenUserProfile,
                    actionType = actionType,
                    expanded = selectedContactResultsExpanded,
                    onExpansionChanged = onSelectedContactResultsExpansionChanged,
                    firstContactId = firstContactId,
                    firstContactFocusRequester = firstContactFocusRequester,
                )
            }

            val internalSearchResults = listOfNotNull(
                emailInvitationCandidate(emailInvitationsEnabled, query, selectedEmails)
            ).plus(
                contactsSearchResult.map { InternalSearchItem.ContactItem(it, selected = false) }
            ).toImmutableList()

            if (internalSearchResults.isNotEmpty()) { // non-selected contacts section filtered by search query
                internalSearchResults(
                    searchTitle = UIText.StringResource(commonR.string.label_contacts),
                    query = query,
                    onChecked = onChecked,
                    onEmailChecked = onEmailChecked,
                    searchResult = internalSearchResults,
                    allItemsVisible = !isSearchActive || searchPeopleScreenState.contactsAllResultsCollapsed,
                    showMoreOrLessButtonVisible = isSearchActive,
                    onShowAllButtonClicked = searchPeopleScreenState::toggleShowAllContactsResult,
                    onOpenUserProfile = onOpenUserProfile,
                    actionType = actionType,
                    expanded = contactResultsExpanded,
                    onExpansionChanged = onContactResultsExpansionChanged,
                    firstContactId = firstContactId,
                    firstContactFocusRequester = firstContactFocusRequester,
                )
            }

            if (publicSearchResult.isNotEmpty()) { // public results section filtered by search query
                externalSearchResults(
                    searchTitle = UIText.StringResource(commonR.string.label_public_wire),
                    searchTerm = query.contactTerm,
                    searchResult = publicSearchResult,
                    allItemsVisible = searchPeopleScreenState.publicResultsCollapsed,
                    showMoreOrLessButtonVisible = isSearchActive,
                    onShowAllButtonClicked = searchPeopleScreenState::toggleShowAllPublicResult,
                    onOpenUserProfile = onOpenUserProfile,
                    expanded = publicResultsExpanded,
                    onExpansionChanged = onPublicResultsExpansionChanged,
                    firstContactId = firstContactId,
                    firstContactFocusRequester = firstContactFocusRequester,
                )
            }
        }

        SideEffect {
            keepOnTopWhenNotScrolled(lazyListState)
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun InternalContactResultItem(
    contact: Contact,
    isSelected: Boolean,
    searchTerm: String,
    onChecked: (Boolean, Contact) -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
    actionType: ItemActionType,
    modifier: Modifier = Modifier,
) {
    with(contact) {
        val onCheckDescription = stringResource(
            id = when {
                isSelected -> commonR.string.content_description_unselect_label
                else -> commonR.string.content_description_select_label
            }
        )
        val clickDescription = stringResource(id = commonR.string.content_description_open_user_profile_label)
        val onCheckClickable = remember(contact, isSelected) {
            Clickable(enabled = !isOnboardingPending || isSelected, onClickDescription = onCheckDescription) {
                if (!isOnboardingPending || isSelected) onChecked(!isSelected, contact)
            }
        }
        InternalContactSearchResultItem(
            avatarData = avatarData,
            name = name,
            label = label,
            membership = membership,
            searchQuery = searchTerm,
            connectionState = connectionState,
            isOnboardingPending = isOnboardingPending,
            isSelected = isSelected,
            onCheckClickable = onCheckClickable,
            actionType = actionType,
            clickable = remember(contact) { Clickable(onClickDescription = clickDescription) { onOpenUserProfile(contact) } },
            modifier = modifier
        )
    }
}

private sealed interface InternalSearchItem {
    val key: String

    data class ContactItem(val contact: Contact, val selected: Boolean) : InternalSearchItem {
        override val key: String get() = "contact:${contact.id}@${contact.domain}"
    }
    data class EmailItem(val email: String, val selected: Boolean, val message: UIText, val enabled: Boolean) : InternalSearchItem {
        override val key: String get() = "email:$email"
    }
}

@Suppress("LongParameterList")
private fun LazyListScope.internalSearchResults(
    searchTitle: UIText,
    actionType: ItemActionType,
    onChecked: (Boolean, Contact) -> Unit,
    searchResult: ImmutableList<InternalSearchItem>,
    query: SearchQuery,
    onOpenUserProfile: (Contact) -> Unit,
    onExpansionChanged: (Boolean) -> Unit,
    expanded: Boolean,
    firstContactId: String?,
    firstContactFocusRequester: FocusRequester?,
    onEmailChecked: (Boolean, String) -> Unit = { _, _ -> },
    allItemsVisible: Boolean = true,
    showMoreOrLessButtonVisible: Boolean = false,
    onShowAllButtonClicked: () -> Unit = {},
) {
    if (searchResult.isNotEmpty()) {
        sectionWithElements(
            header = searchTitle,
            items = when {
                allItemsVisible -> searchResult
                else -> searchResult.take(DEFAULT_SEARCH_RESULT_ITEM_SIZE)
            }.associateBy { it.key },
            folderType = FolderType.Collapsing(expanded = expanded, onChanged = onExpansionChanged),
        ) { result: InternalSearchItem ->
            when (result) {
                is InternalSearchItem.EmailItem -> EmailInvitationItem(
                    email = result.email,
                    searchQuery = query.text,
                    selected = result.selected,
                    message = result.message.asString(),
                    enabled = result.enabled,
                    onChecked = { onEmailChecked(!result.selected, result.email) },
                )

                is InternalSearchItem.ContactItem -> InternalContactResultItem(
                    contact = result.contact,
                    isSelected = result.selected,
                    searchTerm = query.contactTerm,
                    onChecked = onChecked,
                    onOpenUserProfile = onOpenUserProfile,
                    actionType = actionType,
                    modifier = result.contact.firstContactFocusModifier(firstContactId, firstContactFocusRequester),
                )
            }
        }
    }

    if (searchResult.size > DEFAULT_SEARCH_RESULT_ITEM_SIZE && showMoreOrLessButtonVisible && expanded) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                ShowButton(
                    isShownAll = allItemsVisible,
                    onShowButtonClicked = onShowAllButtonClicked,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = dimensions().spacing8x)
                )
            }
        }
    }
}

@Suppress("LongParameterList")
private fun LazyListScope.externalSearchResults(
    searchTitle: UIText,
    allItemsVisible: Boolean,
    showMoreOrLessButtonVisible: Boolean,
    searchResult: List<Contact>,
    searchTerm: String,
    onShowAllButtonClicked: () -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
    expanded: Boolean,
    onExpansionChanged: (Boolean) -> Unit,
    firstContactId: String?,
    firstContactFocusRequester: FocusRequester?,
) {
    val itemsList =
        if (allItemsVisible) searchResult else searchResult.take(DEFAULT_SEARCH_RESULT_ITEM_SIZE)

    sectionWithElements(
        header = searchTitle,
        items = itemsList.associateBy { it.id },
        folderType = FolderType.Collapsing(expanded = expanded, onChanged = onExpansionChanged),
    ) { contact ->
        with(contact) {
            val clickDescription = stringResource(id = commonR.string.content_description_open_user_profile_label)
            ExternalContactSearchResultItem(
                avatarData = avatarData,
                userId = UserId(id, domain),
                name = name,
                label = label,
                membership = membership,
                connectionState = connectionState,
                isOnboardingPending = isOnboardingPending,
                searchQuery = searchTerm,
                clickable = remember(contact) { Clickable(onClickDescription = clickDescription) { onOpenUserProfile(contact) } },
                modifier = contact.firstContactFocusModifier(firstContactId, firstContactFocusRequester)
            )
        }
    }
    if (searchResult.size > DEFAULT_SEARCH_RESULT_ITEM_SIZE && showMoreOrLessButtonVisible && expanded) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                ShowButton(
                    isShownAll = allItemsVisible,
                    onShowButtonClicked = onShowAllButtonClicked,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = dimensions().spacing8x)
                )
            }
        }
    }
}

private fun Contact.firstContactFocusModifier(
    firstContactId: String?,
    focusRequester: FocusRequester?,
): Modifier = if (id == firstContactId && focusRequester != null) {
    Modifier.focusRequester(focusRequester)
} else {
    Modifier
}

private val KeyEvent.isForwardTabKey: Boolean
    get() = type == KeyEventType.KeyDown && key == Key.Tab && !isShiftPressed

@Composable
private fun ShowButton(
    isShownAll: Boolean,
    onShowButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        AnimatedContent(isShownAll) { showAll ->
            WireSecondaryButton(
                text = if (!showAll) stringResource(commonR.string.label_show_more) else stringResource(commonR.string.label_show_less),
                onClick = onShowButtonClicked,
                minSize = dimensions().buttonSmallMinSize,
                minClickableSize = dimensions().buttonMinClickableSize,
                fillMaxWidth = false,
            )
        }
    }
}

private fun emailInvitationCandidate(
    enabled: Boolean,
    query: SearchQuery,
    selectedEmails: Set<String>,
): InternalSearchItem.EmailItem? {
    if (!enabled || query.text.isBlank()) return null
    val email = query.text.trim()
    val duplicate = selectedEmails.any { it.equals(email, ignoreCase = true) }
    val message = when {
        duplicate -> R.string.email_invitation_duplicate
        !query.isValidEmail -> R.string.email_invitation_invalid
        else -> R.string.email_invitation_description
    }
    return InternalSearchItem.EmailItem(
        email = email,
        selected = false,
        message = UIText.StringResource(message),
        enabled = query.isValidEmail && !duplicate,
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewShowButton() {
    WireTheme {
        ShowButton(isShownAll = false, onShowButtonClicked = {})
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllPeopleScreen_Loading() = WireTheme {
    SearchAllPeopleScreen(
        query = SearchQuery(text = "Search query"),
        contactsSearchResult = persistentListOf(),
        publicSearchResult = persistentListOf(),
        contactsSelectedSearchResult = persistentListOf(),
        isLoading = true,
        isSearchActive = true,
        actionType = ItemActionType.CHECK,
        onChecked = { _, _ -> },
        onOpenUserProfile = { },
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllPeopleScreen_InitialResults() = WireTheme {
    val contacts = previewContactsList(count = 10, startIndex = 0, isContact = true)
    SearchAllPeopleScreen(
        query = SearchQuery(text = ""),
        contactsSearchResult = contacts,
        publicSearchResult = persistentListOf(),
        contactsSelectedSearchResult = persistentListOf(),
        isLoading = false,
        isSearchActive = true,
        actionType = ItemActionType.CHECK,
        onChecked = { _, _ -> },
        onOpenUserProfile = { },
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllPeopleScreen_EmptyInitialResults() = WireTheme {
    SearchAllPeopleScreen(
        query = SearchQuery(text = ""),
        contactsSearchResult = persistentListOf(),
        publicSearchResult = persistentListOf(),
        contactsSelectedSearchResult = persistentListOf(),
        isLoading = false,
        isSearchActive = false,
        actionType = ItemActionType.CLICK,
        onChecked = { _, _ -> },
        onOpenUserProfile = { },
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllPeopleScreen_SearchResults_TypeClick() = WireTheme {
    val contacts = previewContactsList(count = 10, startIndex = 0, isContact = true)
    val public = previewContactsList(count = 10, startIndex = 10, isContact = false)
    SearchAllPeopleScreen(
        query = SearchQuery(text = "Con"),
        contactsSearchResult = contacts,
        publicSearchResult = public,
        contactsSelectedSearchResult = persistentListOf(),
        isLoading = false,
        isSearchActive = true,
        actionType = ItemActionType.CLICK,
        onChecked = { _, _ -> },
        onOpenUserProfile = { },
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllPeopleScreen_SearchResults_TypeCheck() = WireTheme {
    val contacts = previewContactsList(count = 10, startIndex = 0, isContact = true)
    val public = previewContactsList(count = 10, startIndex = 10, isContact = false)
    val selectedContacts = previewContactsList(count = 5, startIndex = 20, isContact = true)
    SearchAllPeopleScreen(
        query = SearchQuery(text = "Con"),
        contactsSearchResult = contacts,
        publicSearchResult = public,
        contactsSelectedSearchResult = selectedContacts,
        emailInvitationsEnabled = true,
        selectedEmails = persistentSetOf("contact_email@wire.com"),
        isLoading = false,
        isSearchActive = true,
        actionType = ItemActionType.CHECK,
        onChecked = { _, _ -> },
        onOpenUserProfile = { },
        selectedContactResultsExpanded = true,
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllPeopleScreen_EmptySearchResults() = WireTheme {
    SearchAllPeopleScreen(
        query = SearchQuery(text = "Con"),
        contactsSearchResult = persistentListOf(),
        publicSearchResult = persistentListOf(),
        contactsSelectedSearchResult = persistentListOf(),
        isLoading = false,
        isSearchActive = true,
        actionType = ItemActionType.CLICK,
        onChecked = { _, _ -> },
        onOpenUserProfile = {},
        selectedContactResultsExpanded = true,
    )
}

private fun previewContact(index: Int, isContact: Boolean) = Contact(
    id = index.toString(),
    domain = "wire.com",
    name = "Contact nr $index",
    handle = "contact_$index",
    connectionState = if (isContact) ConnectionState.ACCEPTED else ConnectionState.NOT_CONNECTED,
    membership = Membership.Standard,
)

private fun previewContactsList(count: Int, startIndex: Int = 0, isContact: Boolean): ImmutableList<Contact> = buildList {
    repeat(count) { index -> add(previewContact(startIndex + index, isContact)) }
}.toPersistentList()
