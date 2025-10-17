/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.debug.featureflags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import com.wire.kalium.logic.data.featureConfig.ChannelFeatureConfiguration
import com.wire.kalium.logic.data.featureConfig.Status
import com.wire.kalium.logic.feature.debug.GetFeatureConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class DebugFeatureFlagsViewModel @Inject constructor(
    private val getFeatureConfig: GetFeatureConfigUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(DebugFeatureFlagsViewState())
    val state = _state.asStateFlow()

    val json = Json { prettyPrint = true }

    init {
        viewModelScope.launch {
            getFeatureConfig()
                .onSuccess { model ->
                    val features = buildList {
                        with(model) {
                            addFeature("App Lock", appLockModel.status, appLockModel)
                            addFeature("Classified Domains", classifiedDomainsModel.status)
                            addFeature("Conference Calling", conferenceCallingModel.status)
                            addFeature("Conversation Guest Links", conversationGuestLinksModel.status)
                            addFeature("Digital Signatures", digitalSignaturesModel.status)
                            addFeature("File Sharing", fileSharingModel.status)
                            addFeature("Guest Room Link", guestRoomLinkModel.status)
                            addFeature("Legal Hold", legalHoldModel.status)
                            addFeature("Search Visibility", searchVisibilityModel.status)
                            addFeature("Self-Deleting Messages", selfDeletingMessagesModel.status, selfDeletingMessagesModel.config)
                            addFeature("Second Factor Password Challenge", secondFactorPasswordChallengeModel.status)
                            addFeature("SSO", ssoModel.status)
                            addFeature("Validate SAML Emails", validateSAMLEmailsModel.status)
                            addFeature("MLS", mlsModel.status, mlsModel)
                            addFeature("E2EI", e2EIModel.status, e2EIModel.config)
                            addFeature("MLS Migration", mlsMigrationModel?.status, mlsMigrationModel)
                            addFeature("Consumable Notifications", consumableNotificationsModel?.status)
                            addFeature("Allowed Global Operations", allowedGlobalOperationsModel?.status, allowedGlobalOperationsModel)
                            addFeature("Wire Cells", cellsModel?.status)
                            addFeature("User Profile QR code", enableUserProfileQRCodeConfigModel?.status)
                            addFeature("Message bubbles", chatBubblesModel?.status)
                            add(
                                Feature(
                                    name = "Channels",
                                    status = when (channelsModel) {
                                        is ChannelFeatureConfiguration.Enabled -> FeatureStatus.ENABLED
                                        ChannelFeatureConfiguration.Disabled -> FeatureStatus.DISABLED
                                    },
                                    configJson = channelsModel.prettyPrint()
                                )
                            )
                        }
                    }.sortedWith(compareBy(Feature::status, Feature::name))

                    _state.update { current -> current.copy(features = features) }
                }
                .onFailure {
                    appLogger.e("Failed to load feature flags")
                }
        }
    }

    private fun MutableList<Feature>.addFeature(name: String, status: Status?) {
        addFeature<Any?>(name, status, null)
    }

    private inline fun <reified T> MutableList<Feature>.addFeature(name: String, status: Status?, config: T?) {
        add(
            Feature(
                name = name,
                status = status.asFeatureStatus(),
                configJson = config.prettyPrint()
            )
        )
    }

    private inline fun <reified T> T?.prettyPrint(): String? =
        this?.let {
            val s = json.encodeToString(it)
            s.substring(1, s.length - 1)
        }
}

data class Feature(
    val name: String,
    val status: FeatureStatus,
    val configJson: String? = null,
)

enum class FeatureStatus {
    ENABLED, DISABLED, NOT_CONFIGURED
}

data class DebugFeatureFlagsViewState(
    val features: List<Feature>? = null,
)

private fun Status?.asFeatureStatus(): FeatureStatus =
    when (this) {
        Status.ENABLED -> FeatureStatus.ENABLED
        Status.DISABLED -> FeatureStatus.DISABLED
        null -> FeatureStatus.NOT_CONFIGURED
    }
