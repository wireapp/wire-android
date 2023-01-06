package com.wire.android.ui.sharing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.wire.android.mapper.ContactMapper
import com.wire.android.model.ImageAsset
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.HomeState
import com.wire.android.ui.home.conversations.search.SearchAllPeopleViewModel
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.feature.connection.SendConnectionRequestUseCase
import com.wire.kalium.logic.feature.publicuser.GetAllContactsUseCase
import com.wire.kalium.logic.feature.publicuser.search.SearchKnownUsersUseCase
import com.wire.kalium.logic.feature.publicuser.search.SearchPublicUsersUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ImportMediaViewModel @Inject constructor(
    private val getSelf: GetSelfUserUseCase,
    private val wireSessionImageLoader: WireSessionImageLoader,
    val dispatchers: DispatcherProvider,
    getAllKnownUsers: GetAllContactsUseCase,
    searchKnownUsers: SearchKnownUsersUseCase,
    searchPublicUsers: SearchPublicUsersUseCase,
    contactMapper: ContactMapper,
    sendConnectionRequest: SendConnectionRequestUseCase,
    navigationManager: NavigationManager
) : SearchAllPeopleViewModel(
    getAllKnownUsers = getAllKnownUsers,
    sendConnectionRequest = sendConnectionRequest,
    searchKnownUsers = searchKnownUsers,
    searchPublicUsers = searchPublicUsers,
    contactMapper = contactMapper,
    dispatcher = dispatchers,
    navigationManager = navigationManager
) {
    var importMediaState by mutableStateOf(
        ImportMediaState(
            importedAssets = listOf(
                ImportedMediaAsset.GenericAsset(
                    "Some dummy assetº.zip",
                    1238L,
                    "application/zip"
                ),
                ImportedMediaAsset.GenericAsset(
                    "Some dummy videoº.mp4",
                    7444L,
                    "video/mp4"
                ),
                ImportedMediaAsset.GenericAsset(
                    "Some dummy asset1.zip",
                    921251L,
                    "application/zip"
                ),
                ImportedMediaAsset.GenericAsset(
                    "Some dummy video2.mp4",
                    21L,
                    "video/mp4"
                ),
            )
        )
    )
        private set

    init {
        loadUserAvatar()
    }

    private fun loadUserAvatar() = viewModelScope.launch(dispatchers.io()) {
        getSelf().collect { selfUser ->
            withContext(dispatchers.main()) {
                importMediaState = importMediaState.copy(avatarAsset = selfUser.previewPicture?.let {
                    ImageAsset.UserAvatarAsset(wireSessionImageLoader, it)
                })
            }
        }
    }

    fun navigateBack() = viewModelScope.launch(dispatchers.main()) { navigationManager.navigateBack() }
}

data class ImportMediaState(
    val avatarAsset: ImageAsset.UserAvatarAsset? = null,
    val importedAssets: List<ImportedMediaAsset> = listOf()
)
