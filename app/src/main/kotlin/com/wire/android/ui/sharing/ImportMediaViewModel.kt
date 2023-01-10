package com.wire.android.ui.sharing

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ShareCompat
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.mapper.ContactMapper
import com.wire.android.model.ImageAsset
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.search.SearchAllPeopleViewModel
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.getBitmapFromUri
import com.wire.android.util.getMetaDataFromUri
import com.wire.android.util.getMimeType
import com.wire.android.util.isImageFile
import com.wire.android.util.parcelableArrayList
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
@Suppress("LongParameterList")
class ImportMediaViewModel @Inject constructor(
    private val getSelf: GetSelfUserUseCase,
    val wireSessionImageLoader: WireSessionImageLoader,
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
        ImportMediaState()
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


    fun handleReceivedDataFromSharingIntent(activity: AppCompatActivity) {
        val incomingIntent = ShareCompat.IntentReader(activity)
        appLogger.e("Received data from sharing intent ${incomingIntent.streamCount}")
        when (incomingIntent.streamCount) {
            0 -> {
                // if stream count is 0 the type will be text, we check the type to double check if it is text
                // todo : handle the text , we can get the text from incomingIntent.text
            }
            1 -> {
                // ACTION_SEND
                incomingIntent.stream?.let { incomingIntent.type?.let { it1 -> handleMimeType(activity, it1, it) } }
            }
            else -> {
                // ACTION_SEND_MULTIPLE
                activity.intent.parcelableArrayList<Parcelable>(Intent.EXTRA_STREAM)?.forEach {
                    val fileUri = it.toString().toUri()
                    handleMimeType(activity, fileUri.getMimeType(activity).toString(), fileUri)
                }
            }
        }
    }

    private fun handleMimeType(context: Context, type: String, uri: Uri) {
        when {
            isImageFile(type) -> {
                uri.getMetaDataFromUri(context).let {
                    appLogger.d("image type $it")

                    importMediaState.importedAssets.add(
                        ImportedMediaAsset.Image(
                            name = it.name,
                            size = it.size,
                            mimeType = type,
                            dataUri = uri
                        )
                    )
                }

            }
            else -> {
                uri.getMetaDataFromUri(context).let {
                    importMediaState.importedAssets.add(
                        ImportedMediaAsset.GenericAsset(
                            name = it.name,
                            size = it.size,
                            mimeType = type,
                            dataUri = uri
                        )
                    )
                    appLogger.d("other types $it")
                }
            }
        }
    }
}

data class ImportMediaState(
    val avatarAsset: ImageAsset.UserAvatarAsset? = null,
    val importedAssets: ArrayList<ImportedMediaAsset> = arrayListOf()
)
