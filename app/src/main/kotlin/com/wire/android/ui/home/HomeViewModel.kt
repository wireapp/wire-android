package com.wire.android.ui.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@ExperimentalMaterial3Api
@HiltViewModel
class HomeViewModel
@Inject constructor(
    private val navigationManager: NavigationManager
) : ViewModel() {

    private var lastScrollIndex = 0
    private val _scrollDown = MutableLiveData(false)

    val scrollDown: LiveData<Boolean>
        get() = _scrollDown.distinctUntilChanged()

    fun updateScrollPosition(newScrollIndex: Int) {
        if (newScrollIndex == lastScrollIndex) return
        _scrollDown.value = newScrollIndex > lastScrollIndex
        lastScrollIndex = newScrollIndex
    }

    suspend fun navigateTo(item: NavigationItem) {
        navigationManager.navigate(NavigationCommand(item.route))
    }

    suspend fun navigateToUserProfile() = navigateTo(NavigationItem.UserProfile)
}
