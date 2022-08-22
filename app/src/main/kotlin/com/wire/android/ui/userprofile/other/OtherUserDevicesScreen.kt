package com.wire.android.ui.userprofile.other

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceItem
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.ui.LinkText
import com.wire.android.util.ui.LinkTextData
import com.wire.kalium.logic.data.client.OtherUserClients
import com.wire.kalium.logic.data.conversation.ClientId

@Composable
fun OtherUserDevicesScreen(
    otherUserClient: List<OtherUserClients>,
    lazyListState: LazyListState = rememberLazyListState(),
    state: OtherUserProfileState
) {
    val context = LocalContext.current
    val supportUrl = BuildConfig.SUPPORT_URL + stringResource(id = R.string.url_why_verify_conversation)
    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = MaterialTheme.wireColorScheme.surface
            )
    ) {
        item {
            LinkText(
                linkTextData = listOf(
                    LinkTextData(
                        text = stringResource(R.string.other_user_devices_decription, state.fullName),
                    ),
                    LinkTextData(
                        text = stringResource(id = R.string.label_learn_more),
                        tag = "learn_more",
                        annotation = supportUrl,
                        onClick = {
                            CustomTabsHelper.launchUrl(context, supportUrl)
                        },
                    )
                ),
                modifier = Modifier
                    .padding(
                        all = dimensions().spacing16x,
                    )
            )
        }

        itemsIndexed(otherUserClient) { index, item ->
            RemoveDeviceItem(Device(item.deviceType.name, ClientId(item.id), ""), false, null)
            if (index < otherUserClient.lastIndex) Divider()
        }

    }
}

