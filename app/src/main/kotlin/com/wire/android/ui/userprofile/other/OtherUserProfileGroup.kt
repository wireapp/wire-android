/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package com.wire.android.ui.userprofile.other

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.SurfaceBackgroundWrapper
import com.wire.android.ui.common.button.WireButton
import com.wire.android.ui.common.button.WireSecondaryIconButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversationslist.model.allowsRoleEdition
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.ui.userprofile.group.RemoveConversationMemberState
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.stringWithStyledArgs
import com.wire.kalium.logic.data.conversation.Conversation.Member

@Composable
fun OtherUserProfileGroup(
    state: OtherUserProfileState,
    lazyListState: LazyListState = rememberLazyListState(),
    onRemoveFromConversation: (RemoveConversationMemberState) -> Unit,
    openChangeRoleBottomSheet: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "user_group_name") {
            UserGroupDetailsInformation(
                title = context.resources.stringWithStyledArgs(
                    R.string.user_profile_group_member,
                    MaterialTheme.wireTypography.body01,
                    MaterialTheme.wireTypography.body02,
                    MaterialTheme.wireColorScheme.onBackground,
                    MaterialTheme.wireColorScheme.onBackground,
                    state.groupState!!.groupName
                ),
                isSelfAdmin = state.groupState.isSelfAdmin,
                onRemoveFromConversation = {
                    onRemoveFromConversation(
                        RemoveConversationMemberState(
                            conversationId = state.conversationId!!,
                            fullName = state.fullName,
                            userName = state.userName,
                            userId = state.userId
                        )
                    )
                }
            )
        }
        item(key = "user_group_role") {
            UserRoleInformation(
                label = stringResource(id = R.string.user_profile_group_role),
                value = AnnotatedString(state.groupState!!.role.name.asString()),
                isSelfAdmin = state.groupState.isSelfAdmin,
                openChangeRoleBottomSheet = openChangeRoleBottomSheet,
                isRoleEditable = state.membership.allowsRoleEdition() && !state.isMetadataEmpty()
            )
        }
    }
}

@Composable
private fun UserGroupDetailsInformation(
    title: AnnotatedString,
    isSelfAdmin: Boolean,
    onRemoveFromConversation: () -> Unit
) {
    SurfaceBackgroundWrapper {
        Column(modifier = Modifier.padding(horizontal = dimensions().spacing16x)) {
            Spacer(modifier = Modifier.height(dimensions().spacing16x))
            Text(
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.labelText,
                text = title
            )
            Spacer(modifier = Modifier.height(dimensions().spacing16x))
            if (isSelfAdmin) {
                WireButton(
                    text = stringResource(id = R.string.user_profile_group_remove_button),
                    minSize = MaterialTheme.wireDimensions.buttonSmallMinSize,
                    minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
                    fillMaxWidth = false,
                    onClick = onRemoveFromConversation
                )
                Spacer(modifier = Modifier.height(dimensions().spacing16x))
            }
        }
    }
}

@Composable
private fun UserRoleInformation(
    label: String,
    value: AnnotatedString,
    clickable: Clickable = Clickable(enabled = false) {},
    isSelfAdmin: Boolean,
    isRoleEditable: Boolean,
    openChangeRoleBottomSheet: () -> Unit
) {
    RowItemTemplate(
        modifier = Modifier.padding(horizontal = dimensions().spacing8x),
        title = {
            Text(
                style = MaterialTheme.wireTypography.subline01,
                color = MaterialTheme.wireColorScheme.labelText,
                text = label.uppercase()
            )
        },
        subtitle = {
            Text(
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onBackground,
                text = value
            )
        },
        actions = {
            if (isSelfAdmin && isRoleEditable) {
                EditButton(onEditClicked = openChangeRoleBottomSheet)
            }
        },
        clickable = clickable
    )
}

@Composable
fun EditButton(onEditClicked: () -> Unit, modifier: Modifier = Modifier) {
    WireSecondaryIconButton(
        onButtonClicked = onEditClicked,
        iconResource = R.drawable.ic_edit,
        contentDescription = R.string.content_description_edit,
        modifier = modifier
    )
}

val Member.Role.name
    get() = when (this) {
        Member.Role.Admin -> UIText.StringResource(R.string.group_role_admin)
        Member.Role.Member -> UIText.StringResource(R.string.group_role_member)
        is Member.Role.Unknown -> UIText.DynamicString(name)
    }

@Composable
@Preview
fun PreviewOtherUserProfileGroup() {
    OtherUserProfileGroup(OtherUserProfileState.PREVIEW, rememberLazyListState(), {}) {}
}

@Composable
@PreviewMultipleThemes
fun PreviewUserGroupDetailsInformation() {
    WireTheme {
        UserGroupDetailsInformation(
            title = AnnotatedString("Group name"),
            isSelfAdmin = true,
            onRemoveFromConversation = {}
        )
    }
}
