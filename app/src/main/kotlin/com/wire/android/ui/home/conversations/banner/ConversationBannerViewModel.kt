package com.wire.android.ui.home.conversations.banner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.SavedStateViewModel
import com.wire.android.ui.home.conversations.banner.usecase.ObserveConversationMembersByTypesUseCase
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LongParameterList")
@HiltViewModel
class ConversationBannerViewModel @Inject constructor(
    qualifiedIdMapper: QualifiedIdMapper,
    override val savedStateHandle: SavedStateHandle,
    private val observeConversationMembersByTypes: ObserveConversationMembersByTypesUseCase,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
) : SavedStateViewModel(savedStateHandle) {

    var bannerState by mutableStateOf<UIText?>(null)

    val conversationId: QualifiedID = qualifiedIdMapper.fromStringToQualifiedID(
        savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)!!
    )

    init {
        viewModelScope.launch {
            observeConversationDetails(conversationId)
                .filter {
                    when (it) {
                        is ObserveConversationDetailsUseCase.Result.Failure -> false
                        is ObserveConversationDetailsUseCase.Result.Success -> it.conversationDetails is ConversationDetails.Group
                    }
                }
                .flatMapLatest { observeConversationMembersByTypes(conversationId) }
                .collect(::handleConversationMemberTypes)
        }
    }

    @Suppress("ComplexMethod")
    private fun handleConversationMemberTypes(userTypes: Set<UserType>) {
        val containsGuests = userTypes.contains(UserType.GUEST)
        val containsFederated = userTypes.contains(UserType.FEDERATED)
        val containsExternal = userTypes.contains(UserType.EXTERNAL)
        val containsService = userTypes.contains(UserType.SERVICE)

        bannerState = when {
            (containsFederated && containsExternal && containsGuests && containsService)
            -> UIText.StringResource(R.string.conversation_banner_federated_externals_guests_services_present)
            (containsFederated && containsExternal && containsGuests)
            -> UIText.StringResource(R.string.conversation_banner_federated_externals_guests_present)
            (containsFederated && containsExternal && containsService)
            -> UIText.StringResource(R.string.conversation_banner_federated_externals_services_present)
            (containsFederated && containsGuests && containsService)
            -> UIText.StringResource(R.string.conversation_banner_federated_guests_services_present)
            (containsExternal && containsGuests && containsService)
            -> UIText.StringResource(R.string.conversation_banner_externals_guests_services_present)
            (containsFederated && containsService) -> UIText.StringResource(R.string.conversation_banner_federated_services_present)
            (containsFederated && containsGuests) -> UIText.StringResource(R.string.conversation_banner_federated_guests_present)
            (containsFederated && containsExternal) -> UIText.StringResource(R.string.conversation_banner_federated_externals_present)
            (containsExternal && containsService) -> UIText.StringResource(R.string.conversation_banner_externals_services_present)
            (containsExternal && containsGuests) -> UIText.StringResource(R.string.conversation_banner_externals_guests_present)
            (containsGuests && containsService) -> UIText.StringResource(R.string.conversation_banner_guests_services_present)
            (containsFederated) -> UIText.StringResource(R.string.conversation_banner_federated_present)
            (containsGuests) -> UIText.StringResource(R.string.conversation_banner_guests_present)
            (containsExternal) -> UIText.StringResource(R.string.conversation_banner_externals_present)
            (containsService) -> UIText.StringResource(R.string.conversation_banner_services_active)
            else -> null
        }

    }

}
