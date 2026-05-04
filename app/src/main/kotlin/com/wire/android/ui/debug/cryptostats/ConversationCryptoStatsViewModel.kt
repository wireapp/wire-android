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
    ESTABLISHED("Established"),
    NOT_ESTABLISHED("Not established"),
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
            val matchesProtocol = when (current.protocolFilter) {
                ProtocolFilter.ALL -> true
                ProtocolFilter.PROTEUS -> detail.protocolType == "Proteus"
                ProtocolFilter.MLS -> detail.protocolType == "MLS"
                ProtocolFilter.MIXED -> detail.protocolType == "Mixed"
            }
            val matchesEstablishment = when (current.establishmentFilter) {
                EstablishmentFilter.ALL -> true
                EstablishmentFilter.ESTABLISHED -> detail.establishedInCrypto == "Yes"
                EstablishmentFilter.NOT_ESTABLISHED -> detail.establishedInCrypto == "No"
                EstablishmentFilter.NOT_APPLICABLE -> detail.establishedInCrypto == "N/A"
            }
            val matchesQuery = query.isEmpty() ||
                detail.conversationName.lowercase().contains(query) ||
                detail.conversationId.lowercase().contains(query) ||
                detail.groupId?.lowercase()?.contains(query) == true
            matchesProtocol && matchesEstablishment && matchesQuery
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
    val mlsNotEstablishedInCrypto: Int,
    val mixedNotEstablishedInCrypto: Int,
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
    val establishedInCrypto: String?,
)

fun ConversationCryptoStats.toUiModel(): ConversationCryptoStatsUiModel = ConversationCryptoStatsUiModel(
    totalConversations = totalConversations,
    proteusCount = proteusCount,
    mlsCount = mlsCount,
    mixedCount = mixedCount,
    mlsNotEstablishedInCrypto = mlsNotEstablishedInCrypto,
    mixedNotEstablishedInCrypto = mixedNotEstablishedInCrypto,
    details = conversationDetails
        .sortedWith(
            compareBy<ConversationCryptoDetail> {
                when {
                    it.protocolType != ConversationCryptoProtocolType.PROTEUS && it.establishedInCrypto == false -> 0
                    it.protocolType != ConversationCryptoProtocolType.PROTEUS -> 1
                    else -> 2
                }
            }.thenBy { it.conversationName }
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
        establishedInCrypto = when {
            establishedInCrypto == null && protocolType == ConversationCryptoProtocolType.PROTEUS -> "N/A"
            establishedInCrypto == true -> "Yes"
            establishedInCrypto == false -> "No"
            else -> "Unknown"
        },
    )
}
