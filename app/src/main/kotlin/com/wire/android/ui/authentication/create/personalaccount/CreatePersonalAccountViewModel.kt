package com.wire.android.ui.authentication.create.personalaccount

import androidx.compose.material.ExperimentalMaterialApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.NavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterialApi::class)
@HiltViewModel
class CreatePersonalAccountViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
) : ViewModel() {

    var moveToStep = MutableSharedFlow<CreatePersonalAccountNavigationItem>()
    var moveBack = MutableSharedFlow<Unit>()

    fun goToStep(item: CreatePersonalAccountNavigationItem) { viewModelScope.launch { moveToStep.emit(item) } }
    fun goBackToPreviousStep() { viewModelScope.launch { moveBack.emit(Unit) } }
    fun closeForm() { viewModelScope.launch { navigationManager.navigateBack() } }
}
