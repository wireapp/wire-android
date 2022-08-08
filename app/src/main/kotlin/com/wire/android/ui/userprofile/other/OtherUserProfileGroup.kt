package com.wire.android.ui.userprofile.other

import androidx.compose.foundation.layout.fillMaxSize
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
import com.wire.android.ui.common.button.WireIconButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.stringWithStyledArgs
import com.wire.kalium.logic.data.conversation.Member

@Composable
fun OtherUserProfileGroup(
    state: OtherUserProfileGroupState,
    lazyListState: LazyListState = rememberLazyListState(),
    openChangeRoleBottomSheet: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "user_group_name") {
            UserGroupInformation(
                value = context.resources.stringWithStyledArgs(
                    R.string.user_profile_group_member,
                    MaterialTheme.wireTypography.body01,
                    MaterialTheme.wireTypography.body02,
                    MaterialTheme.wireColorScheme.onBackground,
                    MaterialTheme.wireColorScheme.onBackground,
                    state.groupName
                )
            )
        }
        item(key = "user_group_role") {
            UserGroupInformation(
                title = stringResource(id = R.string.user_profile_group_role),
                value = AnnotatedString(state.role.name.asString()),
                actions = {
                    if (state.isSelfAnAdmin)
                        EditButton(onEditClicked = openChangeRoleBottomSheet)
                },
            )
        }
    }
}

@Composable
private fun UserGroupInformation(
    title: String? = null,
    value: AnnotatedString,
    clickable: Clickable = Clickable(enabled = false) {},
    actions: @Composable () -> Unit = {},
) {
    RowItemTemplate(
        modifier = Modifier.padding(horizontal = dimensions().spacing8x),
        title = title?.let {
            {
                Text(
                    style = MaterialTheme.wireTypography.subline01,
                    color = MaterialTheme.wireColorScheme.labelText,
                    text = title.uppercase()
                )
            }
        } ?: {},
        subtitle = {
            Text(
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onBackground,
                text = value
            )
        },
        actions = actions,
        clickable = clickable
    )
}

@Composable
fun EditButton(onEditClicked: () -> Unit, modifier: Modifier = Modifier) {
    WireIconButton(
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
fun OtherUserProfileGroupPreview() {
    OtherUserProfileGroup(OtherUserProfileState.PREVIEW.groupState!!) {}
}
