package com.wire.android.feature.conversation.list.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.core.async.DispatcherProvider
import com.wire.android.core.functional.onSuccess
import com.wire.android.core.usecase.DefaultUseCaseExecutor
import com.wire.android.core.usecase.UseCaseExecutor
import com.wire.android.shared.auth.activeuser.GetActiveUserUseCase

class ConversationListViewModel(
    override val dispatcherProvider: DispatcherProvider,
    private val getActiveUserUseCase: GetActiveUserUseCase
) : ViewModel(), UseCaseExecutor by DefaultUseCaseExecutor(dispatcherProvider) {

    private val _userNameLiveData = MutableLiveData<String>()
    val userNameLiveData: LiveData<String> = _userNameLiveData

    fun fetchUserName() {
        getActiveUserUseCase(viewModelScope, Unit) {
            it.onSuccess { user -> _userNameLiveData.value = user.name }
        }
    }
}
