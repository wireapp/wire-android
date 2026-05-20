/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.ui.debug.cryptostats

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.kalium.logic.feature.debug.ConversationCryptoDetail
import com.wire.kalium.logic.feature.debug.ConversationCryptoProtocolType
import com.wire.kalium.logic.feature.debug.ConversationCryptoStats
import com.wire.kalium.logic.feature.debug.DetailGroupState
import com.wire.kalium.logic.feature.debug.GetConversationCryptoStatsResult
import com.wire.kalium.logic.feature.debug.GetConversationCryptoStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ProtocolFilter(val label: String) {
    ALL("All"),
    PROTEUS("Proteus"),
    MLS("MLS"),
    MIXED("Mixed"),
}

enum class EstablishmentFilter(val label: String) {
    ALL("All"),
    IN_SYNC("In sync"),
    DRIFT("Drift"),
    LEFT("Left"),
    LOOKUP_FAILED("Lookup failed"),
    NOT_APPLICABLE("N/A (Proteus)"),
}

@HiltViewModel
class ConversationCryptoStatsViewModel @Inject constructor(
    private val getConversationCryptoStats: GetConversationCryptoStatsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ConversationCryptoStatsViewState())
    val state = _state.asStateFlow()

    private val allDetails = mutableListOf<ConversationCryptoDetailUiModel>()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = getConversationCryptoStats()) {
                is GetConversationCryptoStatsResult.Success -> {
                    val uiModel = result.stats.toUiModel()
                    allDetails.clear()
                    allDetails.addAll(uiModel.details)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            stats = result.stats,
                            error = null,
                        )
                    }
                }
                is GetConversationCryptoStatsResult.Failure -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = result.coreFailure.toString(),
                        )
                    }
                }
            }
        }
    }

    fun setProtocolFilter(filter: ProtocolFilter) {
        _state.update { it.copy(protocolFilter = filter) }
    }

    fun setEstablishmentFilter(filter: EstablishmentFilter) {
        _state.update { it.copy(establishmentFilter = filter) }
    }

    fun filteredDetails(): List<ConversationCryptoDetailUiModel> {
        val current = _state.value
        val query = current.searchQuery.text.toString().trim().lowercase()
        return allDetails.filter { detail ->
            current.protocolFilter.matches(detail) &&
                current.establishmentFilter.matches(detail) &&
                detail.matches(query)
        }
    }
}

data class ConversationCryptoStatsViewState(
    val isLoading: Boolean = true,
    val stats: ConversationCryptoStats? = null,
    val error: String? = null,
    val searchQuery: TextFieldState = TextFieldState(),
    val protocolFilter: ProtocolFilter = ProtocolFilter.ALL,
    val establishmentFilter: EstablishmentFilter = EstablishmentFilter.ALL,
)

data class ConversationCryptoStatsUiModel(
    val totalConversations: Int,
    val proteusCount: Int,
    val mlsCount: Int,
    val mixedCount: Int,
    val mlsDriftCount: Int,
    val mixedDriftCount: Int,
    val mlsLeftCount: Int,
    val mixedLeftCount: Int,
    val ccLookupFailedCount: Int,
    val details: List<ConversationCryptoDetailUiModel>,
)

data class ConversationCryptoDetailUiModel(
    val conversationId: String,
    val conversationName: String,
    val protocolType: String,
    val groupId: String?,
    val dbGroupState: String?,
    val dbEpoch: String?,
    val ccEpoch: String?,
    val ccLookupFailed: Boolean,
    val selfIsMember: Boolean,
    val cryptoStatus: ConversationCryptoStatus,
)

enum class ConversationCryptoStatus(val label: String, val sortOrder: Int) {
    LOOKUP_FAILED("Lookup failed", SORT_ORDER_LOOKUP_FAILED),
    DRIFT("Drift", SORT_ORDER_DRIFT),
    LEFT("Left", SORT_ORDER_LEFT),
    IN_SYNC("In sync", SORT_ORDER_NORMAL),
    NOT_APPLICABLE("N/A", SORT_ORDER_NORMAL),
}

fun ConversationCryptoStats.toUiModel(): ConversationCryptoStatsUiModel = ConversationCryptoStatsUiModel(
    totalConversations = totalConversations,
    proteusCount = proteusCount,
    mlsCount = mlsCount,
    mixedCount = mixedCount,
    mlsDriftCount = mlsDriftCount,
    mixedDriftCount = mixedDriftCount,
    mlsLeftCount = mlsLeftCount,
    mixedLeftCount = mixedLeftCount,
    ccLookupFailedCount = ccLookupFailedCount,
    details = conversationDetails
        .sortedWith(
            compareBy<ConversationCryptoDetail> { it.cryptoStatus().sortOrder }
                .thenBy { it.conversationName }
        )
        .map { it.toUiModel() },
)

private fun ConversationCryptoDetail.toUiModel(): ConversationCryptoDetailUiModel {
    val name = conversationName ?: conversationId.value
    return ConversationCryptoDetailUiModel(
        conversationId = "${conversationId.domain}/${conversationId.value}",
        conversationName = name,
        protocolType = when (protocolType) {
            ConversationCryptoProtocolType.PROTEUS -> "Proteus"
            ConversationCryptoProtocolType.MLS -> "MLS"
            ConversationCryptoProtocolType.MIXED -> "Mixed"
        },
        groupId = groupId,
        dbGroupState = dbGroupState?.name,
        dbEpoch = dbEpoch?.toString(),
        ccEpoch = ccEpoch?.toString(),
        ccLookupFailed = ccLookupFailed,
        selfIsMember = selfIsMember,
        cryptoStatus = cryptoStatus(),
    )
}

private fun ConversationCryptoDetail.cryptoStatus(): ConversationCryptoStatus = when {
    protocolType == ConversationCryptoProtocolType.PROTEUS -> ConversationCryptoStatus.NOT_APPLICABLE
    ccLookupFailed -> ConversationCryptoStatus.LOOKUP_FAILED
    !selfIsMember -> ConversationCryptoStatus.LEFT
    dbGroupState == DetailGroupState.ESTABLISHED && ccEpoch == null -> ConversationCryptoStatus.DRIFT
    else -> ConversationCryptoStatus.IN_SYNC
}

private fun ProtocolFilter.matches(detail: ConversationCryptoDetailUiModel): Boolean = when (this) {
    ProtocolFilter.ALL -> true
    ProtocolFilter.PROTEUS -> detail.protocolType == "Proteus"
    ProtocolFilter.MLS -> detail.protocolType == "MLS"
    ProtocolFilter.MIXED -> detail.protocolType == "Mixed"
}

private fun EstablishmentFilter.matches(detail: ConversationCryptoDetailUiModel): Boolean = when (this) {
    EstablishmentFilter.ALL -> true
    EstablishmentFilter.IN_SYNC -> detail.cryptoStatus == ConversationCryptoStatus.IN_SYNC
    EstablishmentFilter.DRIFT -> detail.cryptoStatus == ConversationCryptoStatus.DRIFT
    EstablishmentFilter.LEFT -> detail.cryptoStatus == ConversationCryptoStatus.LEFT
    EstablishmentFilter.LOOKUP_FAILED -> detail.cryptoStatus == ConversationCryptoStatus.LOOKUP_FAILED
    EstablishmentFilter.NOT_APPLICABLE -> detail.cryptoStatus == ConversationCryptoStatus.NOT_APPLICABLE
}

private fun ConversationCryptoDetailUiModel.matches(query: String): Boolean =
    query.isEmpty() ||
        conversationName.lowercase().contains(query) ||
        conversationId.lowercase().contains(query) ||
        groupId?.lowercase()?.contains(query) == true

private const val SORT_ORDER_LOOKUP_FAILED = 0
private const val SORT_ORDER_DRIFT = 1
private const val SORT_ORDER_LEFT = 2
private const val SORT_ORDER_NORMAL = 3
