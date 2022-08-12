package com.wire.android.ui.userprofile.other

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceItem
import com.wire.android.ui.common.SurfaceBackgroundWrapper
import com.wire.kalium.logic.data.client.OtherUserClients

@Composable
fun OtherUserDevicesScreen(
    otherUserClient: List<OtherUserClients>,
    lazyListState: LazyListState = rememberLazyListState()
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "user_group_name") {

        }

//        itemsIndexed(otherUserClient){index, item ->
//
//            RemoveDeviceItem(Device())
//
//        }

    }


}
@Composable
private fun RemoveDeviceItemsList(
    lazyListState: LazyListState,
    items: List<Device>,
    placeholders: Boolean,
    onItemClicked: (Device) -> Unit,
) {
    SurfaceBackgroundWrapper {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(items) { index, device ->
                RemoveDeviceItem(device, placeholders, onItemClicked)
                if (index < items.lastIndex) Divider()
            }
        }
    }
}
