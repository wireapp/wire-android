package com.wire.android.ui.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.ViewModel
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import javax.inject.Inject

@ExperimentalMaterial3Api
@HiltViewModel
class HomeViewModel
@Inject constructor(
    private val navigationManager: NavigationManager
) : ViewModel() {

    private var scrollIndexFlow = MutableStateFlow(0)

    val scrollDownFlow: Flow<Boolean> = scrollIndexFlow
        .scan(0 to 0) { prevPair, newScrollIndex ->
            if (prevPair.second == newScrollIndex || newScrollIndex == prevPair.second + 1) prevPair
            else prevPair.second to newScrollIndex
        }
        .map { (prevScrollIndex, newScrollIndex) ->
            newScrollIndex > prevScrollIndex + 1
        }
        .distinctUntilChanged()

    suspend fun updateScrollPosition(newScrollIndex: Int) {
        scrollIndexFlow.emit(newScrollIndex)
    }

    suspend fun navigateTo(item: NavigationItem) {
        navigationManager.navigate(NavigationCommand(item.route))
    }

    suspend fun navigateToUserProfile() = navigateTo(NavigationItem.UserProfile)
}
