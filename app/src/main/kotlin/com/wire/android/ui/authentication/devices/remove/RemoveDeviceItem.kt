package com.wire.android.ui.authentication.devices.remove

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.kalium.logic.data.client.Client
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.button.getMinTouchMargins
import com.wire.android.ui.common.shimmerPlaceholder
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.formatMediumDateTime
import com.wire.kalium.logic.data.client.ClientType

@Composable
fun RemoveDeviceItem(client: Client, placeholder: Boolean, onRemoveDeviceClick: (Client) -> Unit) {
    RemoveDeviceItemContent(client = client, placeholder = placeholder, onRemoveDeviceClick = onRemoveDeviceClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoveDeviceItemContent(client: Client, placeholder: Boolean, onRemoveDeviceClick: (Client) -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Row(
            modifier = Modifier
                .padding(MaterialTheme.wireDimensions.removeDeviceItemPadding)
                .weight(1f)
        ) {
            Icon(
                modifier = Modifier.shimmerPlaceholder(visible = placeholder),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_devices),
                contentDescription = stringResource(R.string.content_description_remove_device_item_icon)
            )
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .padding(start = MaterialTheme.wireDimensions.removeDeviceItemPadding)
                    .weight(1f),
            ) { RemoveDeviceItemTexts(client, placeholder) }
        }
        val (buttonTopPadding, buttonEndPadding) = getMinTouchMargins(minSize = MaterialTheme.wireDimensions.buttonSmallMinSize)
            .let {
                // default button touch area [48x48] is higher than button size [40x32] so it will have margins, we have to subtract
                // these margins from the default item padding so that all elements are the same distance from the edge
                Pair(
                    MaterialTheme.wireDimensions.removeDeviceItemPadding - it.calculateTopPadding(),
                    MaterialTheme.wireDimensions.removeDeviceItemPadding - it.calculateEndPadding(LocalLayoutDirection.current)
                )
            }
        if (!placeholder)
            WireSecondaryButton(
                modifier = Modifier
                    .padding(top = buttonTopPadding, end = buttonEndPadding)
                    .testTag("remove device button"),
                onClick = { onRemoveDeviceClick(client) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_remove),
                        contentDescription = stringResource(R.string.content_description_remove_device_remove_icon),
                    )
                },
                fillMaxWidth = false,
                minHeight = MaterialTheme.wireDimensions.buttonSmallMinSize.height,
                minWidth = MaterialTheme.wireDimensions.buttonSmallMinSize.width,
                shape = RoundedCornerShape(size = MaterialTheme.wireDimensions.buttonSmallCornerSize),
                contentPadding = PaddingValues(0.dp),
            )
    }
}

@Composable
private fun RemoveDeviceItemTexts(client: Client, placeholder: Boolean) {
    Text(
        style = MaterialTheme.wireTypography.body02,
        color = MaterialTheme.wireColorScheme.onBackground,
        text = client.name,
        modifier = Modifier
            .fillMaxWidth()
            .shimmerPlaceholder(visible = placeholder)
    )
    Spacer(modifier = Modifier.height(MaterialTheme.wireDimensions.removeDeviceItemTitleVerticalPadding))
    val details = stringResource(
        R.string.remove_device_id_and_time_label,
        client.id.value,
        client.registrationTime.formatMediumDateTime() ?: ""
    )
    Text(
        style = MaterialTheme.wireTypography.subline01,
        color = MaterialTheme.wireColorScheme.labelText,
        text = details,
        modifier = Modifier
            .fillMaxWidth()
            .shimmerPlaceholder(visible = placeholder)
    )
}

@Preview
@Composable
private fun RemoveDeviceItemPreview() {
    Box(modifier = Modifier.fillMaxWidth()) {
        RemoveDeviceItem(
            Client(
                ClientId("client_id"),
                ClientType.Permanent,
                "2022",
                null,
                null,
                "client lable",
                null,
                null,
                null
            ), false
        ) {}
    }
}
