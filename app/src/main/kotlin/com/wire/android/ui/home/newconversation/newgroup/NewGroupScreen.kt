package com.wire.android.ui.home.newconversation.newgroup

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.ui.common.groupname.GroupMetadataState
import com.wire.android.ui.common.groupname.GroupNameScreen

@Composable
fun NewGroupScreen(
    newGroupState: GroupMetadataState,
    onGroupNameChange: (TextFieldValue) -> Unit,
    onContinuePressed: () -> Unit,
    onGroupNameErrorAnimated: () -> Unit,
    onBackPressed: () -> Unit
) {
    GroupNameScreen(
        newGroupState = newGroupState,
        onGroupNameChange = onGroupNameChange,
        onContinuePressed = onContinuePressed,
        onGroupNameErrorAnimated = onGroupNameErrorAnimated,
        onBackPressed = onBackPressed
    )
}

@Composable
@Preview
private fun NewGroupScreenPreview() {
    GroupNameScreen(
        GroupMetadataState(),
        {},
        {},
        {},
        {}
    )
}
