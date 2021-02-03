package com.wire.android.feature.conversation.list.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagedList
import com.wire.android.core.async.DispatcherProvider
import com.wire.android.core.events.Event
import com.wire.android.core.events.EventsHandler
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.onFailure
import com.wire.android.core.functional.onSuccess
import com.wire.android.core.usecase.DefaultUseCaseExecutor
import com.wire.android.core.usecase.UseCaseExecutor
import com.wire.android.feature.conversation.list.toolbar.ToolbarData
import com.wire.android.shared.team.Team
import com.wire.android.shared.team.usecase.GetUserTeamUseCase
import com.wire.android.shared.team.usecase.GetUserTeamUseCaseParams
import com.wire.android.shared.team.usecase.NotATeamUser
import com.wire.android.shared.user.User
import com.wire.android.shared.user.usecase.GetCurrentUserUseCase

class ConversationListViewModel(
    override val dispatcherProvider: DispatcherProvider,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getUserTeamUseCase: GetUserTeamUseCase,
    conversationListPagingDelegate: ConversationListPagingDelegate,
    private val eventsHandler: EventsHandler
) : ViewModel(), UseCaseExecutor by DefaultUseCaseExecutor(dispatcherProvider) {

    private val _toolbarDataLiveData = MutableLiveData<ToolbarData>()
    val toolbarDataLiveData: LiveData<ToolbarData> = _toolbarDataLiveData

    val conversationListItemsLiveData: LiveData<PagedList<ConversationListItem>> =
        conversationListPagingDelegate.conversationList(CONVERSATIONS_PAGE_SIZE)

    fun fetchToolbarData() {
        fetchUserData()
    }

    private fun fetchUserData() =
        getCurrentUserUseCase(viewModelScope, Unit) {
            it.onSuccess(::fetchTeamData)
        }

    private fun fetchTeamData(user: User) =
        getUserTeamUseCase(viewModelScope, GetUserTeamUseCaseParams(user)) { result ->
            result.onSuccess { updateToolbarData(user, it) }
                .onFailure {
                    if (it is NotATeamUser) updateToolbarData(user, null)
                    else handleToolbarDataFailure(it)
                }
        }

    private fun updateToolbarData(user: User, team: Team?) {
        _toolbarDataLiveData.value = ToolbarData(user, team)
    }

    private fun handleToolbarDataFailure(failure: Failure) {
        //TODO: display some kind of error
    }

    fun subscribeToEvents() = with(eventsHandler) {
        subscribe<Event.UsernameChanged> { fetchUserData() }
        subscribe<Event.ConversationNameChanged> { TODO() }
    }

    companion object {
        private const val CONVERSATIONS_PAGE_SIZE = 30
    }
}
