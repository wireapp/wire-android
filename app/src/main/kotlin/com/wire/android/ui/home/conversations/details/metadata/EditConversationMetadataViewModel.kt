package com.wire.android.ui.home.conversations.details.metadata

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.common.groupname.GroupMetadataState
import com.wire.android.ui.common.groupname.GroupNameMode
import com.wire.android.ui.common.groupname.GroupNameValidator
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.RenameConversationUseCase
import com.wire.kalium.logic.feature.conversation.RenamingResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class EditConversationMetadataViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val dispatcher: DispatcherProvider,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val renameConversation: RenameConversationUseCase,
    savedStateHandle: SavedStateHandle,
    qualifiedIdMapper: QualifiedIdMapper
) : ViewModel() {

    private val conversationId: QualifiedID = qualifiedIdMapper.fromStringToQualifiedID(
        savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)!!
    )

    var editConversationState by mutableStateOf(GroupMetadataState(mode = GroupNameMode.EDITION))
        private set

    init {
        observeConversationDetails()
    }

    private fun observeConversationDetails() {
        viewModelScope.launch {
            observeConversationDetails(conversationId)
                .filterIsInstance<ObserveConversationDetailsUseCase.Result.Success>()
                .map { it.conversationDetails }
                .filterIsInstance<ConversationDetails.Group>()
                .distinctUntilChanged()
                .flowOn(dispatcher.io())
                .shareIn(this, SharingStarted.WhileSubscribed(), 1)
                .collectLatest {
                    editConversationState = editConversationState.copy(
                        groupName = TextFieldValue(it.conversation.name.orEmpty()),
                    )
                }
        }
    }

    fun onGroupNameChange(newText: TextFieldValue) {
        editConversationState = GroupNameValidator.onGroupNameChange(newText, editConversationState)
    }

    fun onGroupNameErrorAnimated() {
        editConversationState = GroupNameValidator.onGroupNameErrorAnimated(editConversationState)
    }

    fun saveNewGroupName() {
        viewModelScope.launch {
            when (withContext(dispatcher.io()) { renameConversation(conversationId, editConversationState.groupName.text) }) {
                is RenamingResult.Failure -> appLogger.e("Failure changing name for conversation")
                is RenamingResult.Success -> appLogger.d("Name changed :)")
            }
        }
    }

    fun navigateBack() {
        viewModelScope.launch { navigationManager.navigateBack() }
    }
}
