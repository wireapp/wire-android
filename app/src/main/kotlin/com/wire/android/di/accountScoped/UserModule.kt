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
import com.wire.kalium.logic.feature.asset.DeleteAssetUseCase
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCase
import com.wire.kalium.logic.feature.asset.GetAvatarAssetUseCase
import com.wire.kalium.logic.feature.client.FinalizeMLSClientAfterE2EIEnrollmentUseCase
import com.wire.kalium.logic.feature.client.IsProfileQRCodeEnabledUseCase
import com.wire.kalium.logic.feature.client.IsWireCellsEnabledForConversationUseCase
import com.wire.kalium.logic.feature.client.IsWireCellsEnabledUseCase
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
import com.wire.kalium.logic.feature.user.UserScope
import com.wire.kalium.logic.feature.user.readReceipts.ObserveReadReceiptsEnabledUseCase
import com.wire.kalium.logic.feature.user.readReceipts.PersistReadReceiptsStatusConfigUseCase
import com.wire.kalium.logic.feature.user.typingIndicator.ObserveTypingIndicatorEnabledUseCase
import com.wire.kalium.logic.feature.user.typingIndicator.PersistTypingIndicatorStatusConfigUseCase
import com.wire.kalium.logic.sync.ForegroundActionsUseCase
import dagger.Module
import dagger.Provides

@Module
@Suppress("TooManyFunctions")
class UserModule {

    @Provides
    fun provideUserScope(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): UserScope = coreLogic.getSessionScope(currentAccount).users

    @Provides
    fun provideRefreshUsersWithoutMetadataUseCase(
        userScope: UserScope
    ): RefreshUsersWithoutMetadataUseCase = userScope.refreshUsersWithoutMetadata

    @Provides
    fun provideDeleteAccountUseCase(
        userScope: UserScope
    ): DeleteAccountUseCase =
        userScope.deleteAccount

    @Provides
    fun provideUpdateEmailUseCase(
        userScope: UserScope
    ): UpdateEmailUseCase =
        userScope.updateEmail

    @Provides
    fun provideUpdateDisplayNameUseCase(
        userScope: UserScope
    ): UpdateDisplayNameUseCase =
        userScope.updateDisplayName

    @Provides
    fun provideUpdateAccentColorUseCase(
        userScope: UserScope
    ): UpdateAccentColorUseCase =
        userScope.updateAccentColor

    @Provides
    fun provideGetAssetSizeLimitUseCase(
        userScope: UserScope
    ): GetAssetSizeLimitUseCase =
        userScope.getAssetSizeLimit

    @Provides
    fun provideObserveReadReceiptsEnabled(userScope: UserScope): ObserveReadReceiptsEnabledUseCase =
        userScope.observeReadReceiptsEnabled

    @Provides
    fun providePersistReadReceiptsStatusConfig(userScope: UserScope): PersistReadReceiptsStatusConfigUseCase =
        userScope.persistReadReceiptsStatusConfig

    @Provides
    fun provideFinalizeMLSClientAfterE2EIEnrollmentUseCase(userScope: UserScope): FinalizeMLSClientAfterE2EIEnrollmentUseCase =
        userScope.finalizeMLSClientAfterE2EIEnrollment

    @Provides
    fun provideObserveTypingIndicatorEnabled(userScope: UserScope): ObserveTypingIndicatorEnabledUseCase =
        userScope.observeTypingIndicatorEnabled

    @Provides
    fun providePersistTypingIndicatorStatusConfig(userScope: UserScope): PersistTypingIndicatorStatusConfigUseCase =
        userScope.persistTypingIndicatorStatusConfig

    @Provides
    fun provideSelfServerConfig(
        userScope: UserScope
    ): SelfServerConfigUseCase = userScope.serverLinks

    @Provides
    fun provideObserveUserInfoUseCase(
        userScope: UserScope
    ): ObserveUserInfoUseCase = userScope.observeUserInfo

    @Provides
    fun provideIsPasswordRequiredUseCase(
        userScope: UserScope
    ): IsPasswordRequiredUseCase = userScope.isPasswordRequired

    @Provides
    fun provideIsPreventAdminlessGroupsEnabledUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): IsPreventAdminlessGroupsEnabledUseCase =
        coreLogic.getSessionScope(currentAccount).isPreventAdminlessGroupsEnabled

    @Provides
    fun provideIsReadOnlyAccountUseCase(
        userScope: UserScope
    ): IsReadOnlyAccountUseCase = userScope.isReadOnlyAccount

    @Provides
    fun provideGetAllContactsNotInTheConversationUseCase(
        userScope: UserScope
    ): GetAllContactsNotInConversationUseCase =
        userScope.getAllContactsNotInConversation

    @Provides
    fun provideGetUserInfoUseCase(userScope: UserScope): GetUserInfoUseCase =
        userScope.getUserInfo

    @Provides
    fun provideUpdateSelfAvailabilityStatusUseCase(userScope: UserScope): UpdateSelfAvailabilityStatusUseCase =
        userScope.updateSelfAvailabilityStatus

    @Provides
    fun provideGetAllContactsUseCase(
        userScope: UserScope
    ): GetAllContactsUseCase =
        userScope.getAllKnownUsers

    @Provides
    fun provideGetKnownUserUseCase(
        userScope: UserScope
    ): GetKnownUserUseCase =
        userScope.getKnownUser

    @Provides
    fun provideGetSelfUseCase(userScope: UserScope): GetSelfUserUseCase =
        userScope.getSelfUser

    @Provides
    fun provideGetSelfTeamIdUseCase(userScope: UserScope): GetSelfTeamIdUseCase =
        userScope.getSelfTeamId

    @Provides
    fun provideObserveSelfUseCase(userScope: UserScope): ObserveSelfUserUseCase =
        userScope.observeSelfUser

    @Provides
    fun provideObserveSelfUserWithTeamUseCase(userScope: UserScope): ObserveSelfUserWithTeamUseCase =
        userScope.observeSelfUserWithTeam

    @Provides
    fun provideGetAvatarAssetUseCase(userScope: UserScope): GetAvatarAssetUseCase =
        userScope.getPublicAsset

    @Provides
    fun provideDeleteAssetUseCase(userScope: UserScope): DeleteAssetUseCase =
        userScope.deleteAsset

    @Provides
    fun provideUploadUserAvatarUseCase(userScope: UserScope): UploadUserAvatarUseCase =
        userScope.uploadUserAvatar

    @Provides
    fun provideSetUserHandleUseCase(userScope: UserScope): SetUserHandleUseCase =
        userScope.setUserHandle

    @Provides
    fun provideGetE2EICertificateUseCase(userScope: UserScope): GetMLSClientIdentityUseCase =
        userScope.getE2EICertificate

    @Provides
    fun provideGetUserE2eiCertificateStatusUseCase(userScope: UserScope): IsOtherUserE2EIVerifiedUseCase =
        userScope.getUserE2eiCertificateStatus

    @Provides
    fun provideGetMembersE2EICertificateStatusesUseCase(userScope: UserScope): GetMembersE2EICertificateStatusesUseCase =
        userScope.getMembersE2EICertificateStatuses

    @Provides
    fun provideGetUserMlsClientIdentities(userScope: UserScope): GetUserMlsClientIdentitiesUseCase =
        userScope.getUserMlsClientIdentities

    @Provides
    fun provideIsPersonalToTeamAccountSupportedByBackendUseCase(userScope: UserScope): CanMigrateFromPersonalToTeamUseCase =
        userScope.isPersonalToTeamAccountSupportedByBackend

    @Provides
    fun provideForegroundActionsUseCase(userScope: UserScope): ForegroundActionsUseCase = userScope.foregroundActions

    @Provides
    fun provideCellsConfigUseCase(userScope: UserScope): IsWireCellsEnabledUseCase = userScope.isWireCellsEnabled

    @Provides
    fun provideIsWireCellsEnabledForConversationUseCase(userScope: UserScope): IsWireCellsEnabledForConversationUseCase =
        userScope.isWireCellsEnabledForConversation

    @Provides
    fun provideProfileQRCodeConfigUseCase(userScope: UserScope): IsProfileQRCodeEnabledUseCase =
        userScope.isProfileQRCodeEnabled
}
