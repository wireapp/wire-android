package com.wire.wireone

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private object WasmKaliumProvider : KaliumProvider {
    private val internalState = MutableStateFlow(
        KaliumUiState(
            runtimeLine = "Kalium: unavailable on Wasm web",
            sessionLine = "Session: unsupported on Wasm",
            syncLine = "Sync: unsupported on Wasm"
        )
    )

    override val state: StateFlow<KaliumUiState> = internalState

    override suspend fun login(userIdentifier: String, password: String) {
        internalState.value = internalState.value.copy(
            errorLine = "Kalium login/sync smoke is currently wired only for JS web."
        )
    }

    override fun selectConversation(conversationId: ConversationId) = Unit

    override suspend fun sendMessage(text: String): Boolean = false
}

@Composable
internal actual fun rememberKaliumProvider(): KaliumProvider = remember { WasmKaliumProvider }
