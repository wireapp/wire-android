package com.wire.android.ui.home.conversations.details.metadata

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.ui.common.groupname.GroupNameScreen

@Composable
fun EditConversationNameScreen(viewModel: EditConversationMetadataViewModel = hiltViewModel()) {
    with(viewModel.editConversationState) {
        GroupNameScreen(
            newGroupState = this,
            onGroupNameChange = viewModel::onGroupNameChange,
            onGroupNameErrorAnimated = viewModel::onGroupNameErrorAnimated,
            onContinuePressed = viewModel::saveNewGroupName,
            onBackPressed = viewModel::navigateBack
        )
    }
}
