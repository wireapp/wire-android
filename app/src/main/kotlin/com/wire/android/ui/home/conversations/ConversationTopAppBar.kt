package com.wire.android.ui.home.conversations

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.topappbar.BackNavigationIconButton
import com.wire.android.ui.home.conversationslist.common.GroupConversationAvatar
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun ConversationScreenTopAppBar(
    title: String,
    avatar: @Composable () -> Unit = {},
    onBackButtonClick: () -> Unit,
    onDropDownClick: () -> Unit,
    onSearchButtonClick: () -> Unit,
    onPhoneButtonClick: () -> Unit
) {
    SmallTopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(MaterialTheme.wireDimensions.buttonCornerSize))
                    .clickable(onClick = onDropDownClick)

            ) {
                avatar()
                Spacer(Modifier.width(MaterialTheme.wireDimensions.spacing6x))
                Text(
                    text = title,
                    style = MaterialTheme.wireTypography.title01,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(weight = 1f, fill = false)
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_dropdown_icon),
                    contentDescription = stringResource(R.string.content_description_drop_down_icon)
                )
            }
        },
        navigationIcon = { BackNavigationIconButton(onBackButtonClick = onBackButtonClick) },
        actions = {
            WireSecondaryButton(
                onClick = onSearchButtonClick,
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_search),
                        contentDescription = stringResource(R.string.content_description_conversation_search_icon),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                },
                fillMaxWidth = false,
                minHeight = MaterialTheme.wireDimensions.spacing32x,
                minWidth = MaterialTheme.wireDimensions.spacing40x,
                shape = RoundedCornerShape(size = MaterialTheme.wireDimensions.corner12x),
                contentPadding = PaddingValues(0.dp)
            )
            Spacer(Modifier.width(MaterialTheme.wireDimensions.spacing6x))
            WireSecondaryButton(
                onClick = onPhoneButtonClick,
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_phone),
                        contentDescription = stringResource(R.string.content_description_conversation_phone_icon),
                    )
                },
                fillMaxWidth = false,
                minHeight = MaterialTheme.wireDimensions.spacing32x,
                minWidth = MaterialTheme.wireDimensions.spacing40x,
                shape = RoundedCornerShape(size = MaterialTheme.wireDimensions.corner12x),
                contentPadding = PaddingValues(0.dp)
            )
            Spacer(Modifier.width(MaterialTheme.wireDimensions.spacing6x))
        }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Preview("Topbar with a very long conversation title")
@Composable
fun ConversationScreenTopAppBarLongTitlePreview() {
    ConversationScreenTopAppBar(
        "This is some very very very very very very very very very very long conversation title",
        { GroupConversationAvatar(color = Color.Green) },
        {},
        {},
        {},
        {},
    )
}

@Preview("Topbar with a short  conversation title")
@Composable
fun ConversationScreenTopAppBarShortTitlePreview() {
    ConversationScreenTopAppBar(
        "Short title",
        { GroupConversationAvatar(color = Color.Blue) },
        {},
        {},
        {},
        {},
    )
}
