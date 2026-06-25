/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.tests.core.pages

import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.StaleObjectException
import backendUtils.team.TeamHelper
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import uiautomatorutils.UiWaitUtils.findElementOrNull
import user.usermanager.ClientUserManager
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class SearchPage(private val device: UiDevice) {
    private val searchFieldSearchPeople = UiSelectorParams(description = "Search people by name or username")
    private val searchInputField = UiSelectorParams(className = "android.widget.EditText")
    private val closeSearchInputFieldButton = UiSelectorParams(
        className = "android.view.View",
        description = "Go back to add participants view"
    )

    fun assertUsernameInSearchResultIs(expectedHandle: String): SearchPage {
        val handleSelector = UiSelectorParams(
            className = "android.widget.TextView",
            text = expectedHandle
        )
        try {
            UiWaitUtils.waitElement(params = handleSelector)
        } catch (e: AssertionError) {
            throw AssertionError(
                "Expected user name in search results to be '$expectedHandle' but its not '$expectedHandle'",
                e
            )
        }
        return this
    }

    fun assertUsernameNotInSearchResult(
        expectedHandle: String,
        timeout: Duration = 5.seconds
    ): SearchPage {
        val handleSelector = UiSelectorParams(
            className = "android.widget.TextView",
            text = expectedHandle
        )
        val gone = UiWaitUtils.retryUntilTimeout(
            timeout = timeout,
            pollingInterval = UiWaitUtils.POLLING_DEFAULT
        ) {
            val element = findElementOrNull(handleSelector)
            element == null || runCatching { element.visibleBounds.isEmpty }
                .getOrElse { error ->
                    if (error is StaleObjectException) {
                        true
                    } else {
                        throw error
                    }
                }
        }
        if (!gone) {
            throw AssertionError("User name '$expectedHandle' is visible in search results.")
        }
        return this
    }

    fun tapSearchPeopleField(): SearchPage {
        val searchField = UiWaitUtils.waitElement(searchFieldSearchPeople)
        searchField.click()
        return this
    }

    fun tapUsernameInSearchResult(userName: String): SearchPage {
        val userName = UiWaitUtils.waitElement(UiSelectorParams(text = userName))
        userName.click()
        return this
    }

    fun clickCloseButtonOnSearchInputField(): SearchPage {
        UiWaitUtils.waitElement(closeSearchInputFieldButton).click()
        return this
    }

    fun typeUniqueUserNameInSearchField(teamHelper: TeamHelper, alias: String): SearchPage {
        // Resolve the alias to the (unique) username
        val uniqueUserName = teamHelper.usersManager.findUserBy(
            alias,
            ClientUserManager.FindBy.NAME_ALIAS
        )
        return typeRawTextInSearchField(uniqueUserName.uniqueUsername.orEmpty())
    }

    fun typeUserNameInSearchField(teamHelper: TeamHelper, alias: String): SearchPage {
        val userName = teamHelper.usersManager.replaceAliasesOccurrences(
            alias,
            ClientUserManager.FindBy.NAME_ALIAS
        )
        return typeRawTextInSearchField(userName)
    }

    fun typeUserNameInSearchField(alias: String): SearchPage {
        val teamHelper by lazy {
            TeamHelper()
        }
        // Resolve the alias to the username
        val userName = teamHelper.usersManager.replaceAliasesOccurrences(
            alias,
            ClientUserManager.FindBy.NAME_ALIAS
        )
        return typeRawTextInSearchField(userName)
    }

    fun typeRawTextInSearchField(text: String): SearchPage {
        val field = UiWaitUtils.waitElement(searchFieldSearchPeople)
        field.click()
        UiWaitUtils.waitElement(searchInputField).text = text
        return this
    }
}
