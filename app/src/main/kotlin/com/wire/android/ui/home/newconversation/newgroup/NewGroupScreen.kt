package com.wire.android.ui.home.newconversation.newgroup

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.ui.common.GroupNameScreen

@Composable
fun NewGroupScreen(
    newGroupState: NewGroupState,
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
        NewGroupState(),
        {},
        {},
        {},
        {}
    )
}
