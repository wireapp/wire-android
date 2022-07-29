package com.wire.android.navigation

import android.os.Parcelable
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.androidx.AndroidScreen
import cafe.adriel.voyager.hilt.getViewModel
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.di.getAssistedViewModel
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.android.ui.authentication.create.personalaccount.CreatePersonalAccountScreen
import com.wire.android.ui.authentication.create.summary.CreateAccountSummaryScreen
import com.wire.android.ui.authentication.create.team.CreateTeamScreen
import com.wire.android.ui.authentication.create.username.CreateAccountUsernameScreen
import com.wire.android.ui.authentication.devices.register.RegisterDeviceScreen
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceScreen
import com.wire.android.ui.authentication.login.LoginScreen
import com.wire.android.ui.authentication.login.email.LoginEmailViewModel
import com.wire.android.ui.authentication.login.sso.LoginSSOViewModel
import com.wire.android.ui.authentication.welcome.WelcomeScreen
import com.wire.android.ui.calling.OngoingCallScreen
import com.wire.android.ui.calling.incoming.IncomingCallScreen
import com.wire.android.ui.calling.initiating.InitiatingCallScreen
import com.wire.android.ui.debugscreen.DebugScreen
import com.wire.android.ui.home.HomeScreen
import com.wire.android.ui.home.HomeTabsViewModels
import com.wire.android.ui.home.conversations.ConversationScreen
import com.wire.android.ui.home.conversations.details.AddMembersToConversationViewModel
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsScreen
import com.wire.android.ui.home.conversations.details.participants.GroupConversationAllParticipantsScreen
import com.wire.android.ui.home.conversations.search.SearchPeopleRouter
import com.wire.android.ui.home.gallery.MediaGalleryScreen
import com.wire.android.ui.home.gallery.MediaGalleryViewModel
import com.wire.android.ui.home.newconversation.NewConversationRouter
import com.wire.android.ui.settings.SettingsScreen
import com.wire.android.ui.userprofile.avatarpicker.AvatarPickerScreen
import com.wire.android.ui.userprofile.other.OtherUserProfileScreen
import com.wire.android.ui.userprofile.other.OtherUserProfileScreenViewModel
import com.wire.android.ui.userprofile.self.SelfUserProfileScreen
import com.wire.android.util.EMPTY
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.kalium.logic.data.id.QualifiedID
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize


@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
sealed class VoyagerNavigationItem(
    val screenMode: ScreenMode = ScreenMode.NONE
) : AndroidScreen() {

    object Welcome : VoyagerNavigationItem() {
        @Composable
        override fun Content() {
            WelcomeScreen(getViewModel())
        }
    }

    data class Login(val ssoLoginResult: DeepLinkResult.SSOLogin? = null) : VoyagerNavigationItem() {

        // state saving and restoration
        var userIdentifier: String = String.EMPTY
        var ssoCode: String = String.EMPTY

        @Composable
        override fun Content() {
            val ssoViewModel: LoginSSOViewModel = getAssistedViewModel(param = ssoCode)
            val emailViewModel: LoginEmailViewModel = getAssistedViewModel(param = userIdentifier)
            LoginScreen(ssoLoginResult, getViewModel(), ssoViewModel, emailViewModel)

            DisposableEffect(LocalLifecycleOwner.current) {
                onDispose {
                    userIdentifier = emailViewModel.loginState.userIdentifier.text
                    ssoCode = ssoViewModel.loginState.ssoCode.text
                }
            }
        }
    }

    object CreateTeam : VoyagerNavigationItem() {
        @Composable
        override fun Content() {
            CreateTeamScreen(getViewModel())
        }
    }

    object CreatePersonalAccount : VoyagerNavigationItem() {
        @Composable
        override fun Content() {
            CreatePersonalAccountScreen(getViewModel())
        }
    }

    object CreateUsername : VoyagerNavigationItem() {
        @Composable
        override fun Content() {
            CreateAccountUsernameScreen(getViewModel())
        }
    }

    data class CreateAccountSummary(val flowType: CreateAccountFlowType) : VoyagerNavigationItem() {
        @Composable
        override fun Content() {
            CreateAccountSummaryScreen(getAssistedViewModel(param = flowType))
        }
    }

    object RemoveDevices : VoyagerNavigationItem() {
        @Composable
        override fun Content() {
            RemoveDeviceScreen(getViewModel())
        }
    }

    object RegisterDevice : VoyagerNavigationItem() {
        @Composable
        override fun Content() {
            RegisterDeviceScreen(getViewModel())
        }
    }

    object Home : VoyagerNavigationItem() {
        @Composable
        override fun Content() {
            HomeScreen(
                getViewModel(),
                getViewModel(),
                HomeTabsViewModels(
                    getViewModel()
                )
            )
        }
    }

    object Settings : VoyagerNavigationItem() {
        @Composable
        override fun Content() {
            SettingsScreen()
        }
    }

    object Debug : VoyagerNavigationItem() {
        @Composable
        override fun Content() {
            DebugScreen(getViewModel())
        }
    }

    sealed class UrlNavigationItem(val url: String) : VoyagerNavigationItem() {
        @Suppress("EmptyFunctionBlock")
        @Composable
        override fun Content() {
        }
    }

    object Support : UrlNavigationItem(BuildConfig.SUPPORT_URL)

    object SelfUserProfile : VoyagerNavigationItem() {
        @Composable
        override fun Content() {
            SelfUserProfileScreen(getViewModel())
        }
    }

    data class OtherUserProfile(
        val userId: NavQualifiedId,
        val conversationId: NavQualifiedId? = null,
        val onConnectionIgnored: (String) -> Unit = {}
    ) : VoyagerNavigationItem() {
        @Composable
        override fun Content() {
            OtherUserProfileScreen(
                getAssistedViewModel(param = OtherUserProfileScreenViewModel.Params(userId, conversationId, onConnectionIgnored))
            )
        }
    }

    object ProfileImagePicker : VoyagerNavigationItem() {
        @Composable
        override fun Content() {
            AvatarPickerScreen(getViewModel())
        }
    }

    data class Conversation(
        val conversationId: NavQualifiedId
    ) : VoyagerNavigationItem() {
        @Composable
        override fun Content() {
            ConversationScreen(getAssistedViewModel(param = conversationId))
        }
    }

    data class GroupConversationDetails(
        val conversationId: NavQualifiedId
    ) : VoyagerNavigationItem() {
        @Composable
        override fun Content() {
            GroupConversationDetailsScreen(getAssistedViewModel(param = conversationId))
        }
    }

    data class AddConversationParticipants(
        val conversationId: NavQualifiedId
    ) : VoyagerNavigationItem() {
        @Composable
        override fun Content() {
            val viewModel: AddMembersToConversationViewModel = getAssistedViewModel(param = conversationId)

            SearchPeopleRouter(
                searchPeopleViewModel = viewModel,
                searchBarTitle = stringResource(id = R.string.label_add_participants),
                onPeoplePicked = viewModel::addMembersToConversation
            )
        }
    }

    data class GroupConversationAllParticipants(
        val conversationId: NavQualifiedId
    ) : VoyagerNavigationItem() {
        @Composable
        override fun Content() {
            GroupConversationAllParticipantsScreen(getAssistedViewModel(param = conversationId))
        }
    }

    object NewConversation : VoyagerNavigationItem() {
        @Composable
        override fun Content() {
            NewConversationRouter(getViewModel())
        }
    }

    data class OngoingCall(
        val conversationId: NavQualifiedId
    ) : VoyagerNavigationItem(ScreenMode.WAKE_UP) {
        @Composable
        override fun Content() {
            OngoingCallScreen(
                getAssistedViewModel(param = conversationId),
                getAssistedViewModel(param = conversationId)
            )
        }
    }

    data class InitiatingCall(
        val conversationId: NavQualifiedId
    ) : VoyagerNavigationItem(ScreenMode.KEEP_ON) {
        @Composable
        override fun Content() {
            InitiatingCallScreen(
                getAssistedViewModel(param = conversationId),
                getAssistedViewModel(param = conversationId)
            )
        }
    }

    data class IncomingCall(
        val conversationId: NavQualifiedId
    ) : VoyagerNavigationItem(ScreenMode.WAKE_UP) {
        @Composable
        override fun Content() {
            IncomingCallScreen(
                getAssistedViewModel(param = conversationId),
                getAssistedViewModel(param = conversationId)
            )
        }
    }

    data class Gallery(
        val conversationId: NavQualifiedId,
        val messageId: String,
        val isSelfAsset: Boolean,
        val onResult: (String, Boolean) -> Unit
    ) : VoyagerNavigationItem() {
        @Composable
        override fun Content() {
            MediaGalleryScreen(getAssistedViewModel(param = MediaGalleryViewModel.Params(conversationId, messageId, isSelfAsset, onResult)))
        }
    }
}

@Parcelize
class NavQualifiedId(val value: String, val domain: String) : Parcelable {

    constructor(qualifiedId: QualifiedID) : this(qualifiedId.value, qualifiedId.domain)

    @IgnoredOnParcel
    val qualifiedId: QualifiedID = QualifiedID(value, domain)
}

fun QualifiedID.nav() = NavQualifiedId(this)

enum class ScreenMode {
    KEEP_ON,  // keep screen on while that NavigationItem is visible (i.e CallScreen)
    WAKE_UP,  // wake up the device on navigating to that NavigationItem (i.e IncomingCall)
    NONE      // do not wake up and allow device to sleep
}
