/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.di.accountScoped

import com.wire.android.di.CurrentAccount
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.di.KaliumUserApi
import com.wire.kalium.logic.feature.asset.DeleteAssetUseCase
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCase
import com.wire.kalium.logic.feature.asset.GetAvatarAssetUseCase
import com.wire.kalium.logic.feature.client.FinalizeMLSClientAfterE2EIEnrollmentUseCase
import com.wire.kalium.logic.feature.client.IsProfileQRCodeEnabledUseCase
import com.wire.kalium.logic.feature.client.IsWireCellsEnabledForConversationUseCase
import com.wire.kalium.logic.feature.client.IsWireCellsEnabledUseCase
import com.wire.kalium.logic.feature.client.ObserveIsWireCellsEnabledUseCase
import com.wire.kalium.logic.feature.conversation.GetAllContactsNotInConversationUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.GetMLSClientIdentityUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.GetMembersE2EICertificateStatusesUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.GetUserMlsClientIdentitiesUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.IsOtherUserE2EIVerifiedUseCase
import com.wire.kalium.logic.feature.personaltoteamaccount.CanMigrateFromPersonalToTeamUseCase
import com.wire.kalium.logic.feature.publicuser.GetAllContactsUseCase
import com.wire.kalium.logic.feature.publicuser.GetKnownUserUseCase
import com.wire.kalium.logic.feature.publicuser.RefreshUsersWithoutMetadataUseCase
import com.wire.kalium.logic.feature.user.DeleteAccountUseCase
import com.wire.kalium.logic.feature.user.GetSelfTeamIdUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.GetUserInfoUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import com.wire.kalium.logic.feature.user.IsPreventAdminlessGroupsEnabledUseCase
import com.wire.kalium.logic.feature.user.IsReadOnlyAccountUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserWithTeamUseCase
import com.wire.kalium.logic.feature.user.ObserveUserInfoUseCase
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import com.wire.kalium.logic.feature.user.SetUserHandleUseCase
import com.wire.kalium.logic.feature.user.UpdateAccentColorUseCase
import com.wire.kalium.logic.feature.user.UpdateDisplayNameUseCase
import com.wire.kalium.logic.feature.user.UpdateEmailUseCase
import com.wire.kalium.logic.feature.user.UpdateSelfAvailabilityStatusUseCase
import com.wire.kalium.logic.feature.user.UploadUserAvatarUseCase
import com.wire.kalium.logic.feature.user.linkPreviews.ObserveLinkPreviewsEnabledUseCase
import com.wire.kalium.logic.feature.user.linkPreviews.PersistLinkPreviewsStatusConfigUseCase
import com.wire.kalium.logic.feature.user.readReceipts.ObserveReadReceiptsEnabledUseCase
import com.wire.kalium.logic.feature.user.readReceipts.PersistReadReceiptsStatusConfigUseCase
import com.wire.kalium.logic.feature.user.typingIndicator.ObserveTypingIndicatorEnabledUseCase
import com.wire.kalium.logic.feature.user.typingIndicator.PersistTypingIndicatorStatusConfigUseCase
import com.wire.kalium.logic.sync.ForegroundActionsUseCase
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides

@BindingContainer
@Suppress("TooManyFunctions")
class UserModule {

    @Provides
    fun provideUserApi(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): KaliumUserApi = coreLogic.getSessionScope(currentAccount).userApi

    @Provides
    fun provideRefreshUsersWithoutMetadataUseCase(
        api: KaliumUserApi
    ): RefreshUsersWithoutMetadataUseCase = api.userRefreshUsersWithoutMetadata

    @Provides
    fun provideDeleteAccountUseCase(
        api: KaliumUserApi
    ): DeleteAccountUseCase =
        api.userDeleteAccount

    @Provides
    fun provideUpdateEmailUseCase(
        api: KaliumUserApi
    ): UpdateEmailUseCase =
        api.userUpdateEmail

    @Provides
    fun provideUpdateDisplayNameUseCase(
        api: KaliumUserApi
    ): UpdateDisplayNameUseCase =
        api.userUpdateDisplayName

    @Provides
    fun provideUpdateAccentColorUseCase(
        api: KaliumUserApi
    ): UpdateAccentColorUseCase =
        api.userUpdateAccentColor

    @Provides
    fun provideGetAssetSizeLimitUseCase(
        api: KaliumUserApi
    ): GetAssetSizeLimitUseCase =
        api.userGetAssetSizeLimit

    @Provides
    fun provideObserveReadReceiptsEnabled(api: KaliumUserApi): ObserveReadReceiptsEnabledUseCase =
        api.userObserveReadReceiptsEnabled

    @Provides
    fun providePersistReadReceiptsStatusConfig(api: KaliumUserApi): PersistReadReceiptsStatusConfigUseCase =
        api.userPersistReadReceiptsStatusConfig

    @Provides
    fun provideObserveLinkPreviewsEnabled(api: KaliumUserApi): ObserveLinkPreviewsEnabledUseCase =
        api.userObserveLinkPreviewsEnabled

    @Provides
    fun providePersistLinkPreviewsStatusConfig(api: KaliumUserApi): PersistLinkPreviewsStatusConfigUseCase =
        api.userPersistLinkPreviewsStatusConfig

    @Provides
    fun provideFinalizeMLSClientAfterE2EIEnrollmentUseCase(api: KaliumUserApi): FinalizeMLSClientAfterE2EIEnrollmentUseCase =
        api.userFinalizeMLSClientAfterE2EIEnrollment

    @Provides
    fun provideObserveTypingIndicatorEnabled(api: KaliumUserApi): ObserveTypingIndicatorEnabledUseCase =
        api.userObserveTypingIndicatorEnabled

    @Provides
    fun providePersistTypingIndicatorStatusConfig(api: KaliumUserApi): PersistTypingIndicatorStatusConfigUseCase =
        api.userPersistTypingIndicatorStatusConfig

    @Provides
    fun provideSelfServerConfig(
        api: KaliumUserApi
    ): SelfServerConfigUseCase = api.userServerLinks

    @Provides
    fun provideObserveUserInfoUseCase(
        api: KaliumUserApi
    ): ObserveUserInfoUseCase = api.userObserveUserInfo

    @Provides
    fun provideIsPasswordRequiredUseCase(
        api: KaliumUserApi
    ): IsPasswordRequiredUseCase = api.userIsPasswordRequired

    @Provides
    fun provideIsPreventAdminlessGroupsEnabledUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): IsPreventAdminlessGroupsEnabledUseCase =
        coreLogic.getSessionScope(currentAccount).isPreventAdminlessGroupsEnabled

    @Provides
    fun provideIsReadOnlyAccountUseCase(
        api: KaliumUserApi
    ): IsReadOnlyAccountUseCase = api.userIsReadOnlyAccount

    @Provides
    fun provideGetAllContactsNotInTheConversationUseCase(
        api: KaliumUserApi
    ): GetAllContactsNotInConversationUseCase =
        api.userGetAllContactsNotInConversation

    @Provides
    fun provideGetUserInfoUseCase(api: KaliumUserApi): GetUserInfoUseCase =
        api.userGetUserInfo

    @Provides
    fun provideUpdateSelfAvailabilityStatusUseCase(api: KaliumUserApi): UpdateSelfAvailabilityStatusUseCase =
        api.userUpdateSelfAvailabilityStatus

    @Provides
    fun provideGetAllContactsUseCase(
        api: KaliumUserApi
    ): GetAllContactsUseCase =
        api.userGetAllKnownUsers

    @Provides
    fun provideGetKnownUserUseCase(
        api: KaliumUserApi
    ): GetKnownUserUseCase =
        api.userGetKnownUser

    @Provides
    fun provideGetSelfUseCase(api: KaliumUserApi): GetSelfUserUseCase =
        api.userGetSelfUser

    @Provides
    fun provideGetSelfTeamIdUseCase(api: KaliumUserApi): GetSelfTeamIdUseCase =
        api.userGetSelfTeamId

    @Provides
    fun provideObserveSelfUseCase(api: KaliumUserApi): ObserveSelfUserUseCase =
        api.userObserveSelfUser

    @Provides
    fun provideObserveSelfUserWithTeamUseCase(api: KaliumUserApi): ObserveSelfUserWithTeamUseCase =
        api.userObserveSelfUserWithTeam

    @Provides
    fun provideGetAvatarAssetUseCase(api: KaliumUserApi): GetAvatarAssetUseCase =
        api.userGetPublicAsset

    @Provides
    fun provideDeleteAssetUseCase(api: KaliumUserApi): DeleteAssetUseCase =
        api.userDeleteAsset

    @Provides
    fun provideUploadUserAvatarUseCase(api: KaliumUserApi): UploadUserAvatarUseCase =
        api.userUploadUserAvatar

    @Provides
    fun provideSetUserHandleUseCase(api: KaliumUserApi): SetUserHandleUseCase =
        api.userSetUserHandle

    @Provides
    fun provideGetE2EICertificateUseCase(api: KaliumUserApi): GetMLSClientIdentityUseCase =
        api.userGetE2EICertificate

    @Provides
    fun provideGetUserE2eiCertificateStatusUseCase(api: KaliumUserApi): IsOtherUserE2EIVerifiedUseCase =
        api.userGetUserE2eiCertificateStatus

    @Provides
    fun provideGetMembersE2EICertificateStatusesUseCase(api: KaliumUserApi): GetMembersE2EICertificateStatusesUseCase =
        api.userGetMembersE2EICertificateStatuses

    @Provides
    fun provideGetUserMlsClientIdentities(api: KaliumUserApi): GetUserMlsClientIdentitiesUseCase =
        api.userGetUserMlsClientIdentities

    @Provides
    fun provideIsPersonalToTeamAccountSupportedByBackendUseCase(api: KaliumUserApi): CanMigrateFromPersonalToTeamUseCase =
        api.userIsPersonalToTeamAccountSupportedByBackend

    @Provides
    fun provideForegroundActionsUseCase(api: KaliumUserApi): ForegroundActionsUseCase = api.userForegroundActions

    @Provides
    fun provideCellsConfigUseCase(api: KaliumUserApi): IsWireCellsEnabledUseCase = api.userIsWireCellsEnabled

    @Provides
    fun provideObserveCellsConfigUseCase(api: KaliumUserApi): ObserveIsWireCellsEnabledUseCase = api.userObserveIsWireCellsEnabled

    @Provides
    fun provideIsWireCellsEnabledForConversationUseCase(api: KaliumUserApi): IsWireCellsEnabledForConversationUseCase =
        api.userIsWireCellsEnabledForConversation

    @Provides
    fun provideProfileQRCodeConfigUseCase(api: KaliumUserApi): IsProfileQRCodeEnabledUseCase =
        api.userIsProfileQRCodeEnabled
}
