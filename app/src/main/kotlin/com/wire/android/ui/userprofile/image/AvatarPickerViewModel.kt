package com.wire.android.ui.userprofile.image

import android.content.Context
import android.net.Uri
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.UserDataStore
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.common.imagepreview.PictureState
import com.wire.android.util.saveAvatarToInternalStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@ExperimentalMaterial3Api
@HiltViewModel
class AvatarPickerViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val dataStore: UserDataStore,
) : ViewModel() {

    fun onNewPicturePicked(chosenImgUri: Uri, context: Context) {
        viewModelScope.launch {
            saveAvatarToInternalStorage(chosenImgUri, context)
            dataStore.updateUserAvatarChanged(true)
        }
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }
}
