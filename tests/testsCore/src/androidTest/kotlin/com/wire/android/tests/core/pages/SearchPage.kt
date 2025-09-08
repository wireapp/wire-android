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
import androidx.test.uiautomator.type
import backendUtils.team.TeamHelper
import org.junit.Assert
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import user.usermanager.ClientUserManager

data class SearchPage(private val device: UiDevice) {
    private val searchFieldSearchPage = UiSelectorParams(className = "android.widget.EditText")
    private val searchFieldSearchPeople = UiSelectorParams(description = "Search people by name or username")

    fun assertUsernameInSearchResultIs(expectedName: String): SearchPage {
        val searchField = UiWaitUtils.waitElement(searchFieldSearchPage)
        val actualText = searchField.text
        Assert.assertEquals(
            "Expected search field text to be '$expectedName', but found '$actualText'",
            expectedName,
            actualText
        )
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

    fun typeUniqueUserNameInSearchField(teamHelper: TeamHelper, alias: String): SearchPage {
        // Resolve the alias to the (unique) username
        val uniqueUserName = teamHelper.usersManager.findUserBy(
            alias,
            ClientUserManager.FindBy.NAME_ALIAS
        )
        val field = UiWaitUtils.waitElement(searchFieldSearchPeople)
        field.click()
        val toType = uniqueUserName.uniqueUsername.orEmpty().replace(" ", "%s")
        device.type(toType)
        return this
    }
}
