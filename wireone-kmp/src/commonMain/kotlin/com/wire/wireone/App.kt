package com.wire.wireone

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.coroutines.launch

@Composable
fun App() {
    val kaliumProvider = rememberKaliumProvider()
    val uiState by kaliumProvider.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var draftMessage by remember { mutableStateOf("") }
    var isSendingMessage by remember { mutableStateOf(false) }

    val filteredConversations = remember(uiState.conversations, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            uiState.conversations
        } else {
            uiState.conversations.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.subtitle.contains(query, ignoreCase = true) ||
                    it.protocolLabel.contains(query, ignoreCase = true)
            }
        }
    }

    WireTheme {
        Surface(color = MaterialTheme.wireColorScheme.background) {
            if (uiState.activeUserId == null) {
                LoginScreen(
                    uiState = uiState,
                    login = login,
                    password = password,
                    onLoginChange = { login = it },
                    onPasswordChange = { password = it },
                    onSubmit = {
                        coroutineScope.launch {
                            kaliumProvider.login(login, password)
                        }
                    }
                )
            } else {
                WorkspaceScreen(
                    uiState = uiState,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    conversations = filteredConversations,
                    onConversationSelected = kaliumProvider::selectConversation,
                    draftMessage = draftMessage,
                    isSendingMessage = isSendingMessage,
                    onDraftMessageChange = { draftMessage = it },
                    onSendMessage = {
                        if (!isSendingMessage) {
                            coroutineScope.launch {
                                isSendingMessage = true
                                val sent = kaliumProvider.sendMessage(draftMessage)
                                if (sent) {
                                    draftMessage = ""
                                }
                                isSendingMessage = false
                            }
                        }
                    },
                    onJoinCall = {
                        coroutineScope.launch { kaliumProvider.joinCall() }
                    },
                    onAnswerCall = {
                        coroutineScope.launch { kaliumProvider.answerCall() }
                    },
                    onRejectCall = {
                        coroutineScope.launch { kaliumProvider.rejectCall() }
                    },
                    onEndCall = {
                        coroutineScope.launch { kaliumProvider.endCall() }
                    },
                )
            }
        }
    }
}

@Composable
private fun LoginScreen(
    uiState: KaliumUiState,
    login: String,
    password: String,
    onLoginChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val colors = MaterialTheme.wireColorScheme
    val spacing = dimensions()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(spacing.spacing24x),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.width(spacing.spacing64x * 6),
            shape = RoundedCornerShape(spacing.spacing24x),
            color = colors.surface,
            tonalElevation = spacing.spacing4x,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier.padding(spacing.spacing24x),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing16x)
            ) {
                Box(
                    modifier = Modifier
                        .size(spacing.spacing48x)
                        .clip(CircleShape)
                        .background(colors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "W",
                        style = MaterialTheme.wireTypography.title02,
                        color = colors.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Wire One",
                    style = MaterialTheme.wireTypography.title01,
                    color = colors.onSurface
                )
                Text(
                    text = "Login flow plus persisted iOS session storage backport.",
                    style = MaterialTheme.wireTypography.body02,
                    color = colors.onSurfaceVariant
                )

                OutlinedTextField(
                    value = login,
                    onValueChange = onLoginChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email or handle") },
                    singleLine = true,
                    enabled = !uiState.isLoggingIn,
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !uiState.isLoggingIn,
                )

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoggingIn,
                    onClick = onSubmit,
                ) {
                    Text(if (uiState.isLoggingIn) "Logging in..." else "Open workspace")
                }

                StatusCard(
                    runtimeLine = uiState.runtimeLine,
                    sessionLine = uiState.sessionLine,
                    syncLine = uiState.syncLine,
                    errorLine = uiState.errorLine,
                )
            }
        }
    }
}

@Composable
private fun WorkspaceScreen(
    uiState: KaliumUiState,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    conversations: List<ConversationListItemUiState>,
    onConversationSelected: (ConversationId) -> Unit,
    draftMessage: String,
    isSendingMessage: Boolean,
    onDraftMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onJoinCall: () -> Unit,
    onAnswerCall: () -> Unit,
    onRejectCall: () -> Unit,
    onEndCall: () -> Unit,
) {
    val colors = MaterialTheme.wireColorScheme
    val spacing = dimensions()
    var isCompactConversationListVisible by rememberSaveable { mutableStateOf(true) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(WindowInsets.statusBars.asPaddingValues())
    ) {
        val isCompactLayout = maxWidth < 700.dp

        if (!isCompactLayout) {
            Row(modifier = Modifier.fillMaxSize()) {
                ConversationListPane(
                    searchQuery = searchQuery,
                    onSearchChange = onSearchChange,
                    conversations = conversations,
                    selectedConversationId = uiState.selectedConversationId,
                    hasPassedSlowSync = uiState.hasPassedSlowSync,
                    syncLine = uiState.syncLine,
                    onConversationSelected = onConversationSelected,
                    onCompactConversationSelected = { },
                    modifier = Modifier.width(spacing.spacing64x * 5),
                )

                WorkspaceDetailPane(
                    uiState = uiState,
                    draftMessage = draftMessage,
                    isSendingMessage = isSendingMessage,
                    onDraftMessageChange = onDraftMessageChange,
                    onSendMessage = onSendMessage,
                    onJoinCall = onJoinCall,
                    onAnswerCall = onAnswerCall,
                    onRejectCall = onRejectCall,
                    onEndCall = onEndCall,
                )
            }
        } else if (isCompactConversationListVisible) {
            ConversationListPane(
                searchQuery = searchQuery,
                onSearchChange = onSearchChange,
                conversations = conversations,
                selectedConversationId = uiState.selectedConversationId,
                hasPassedSlowSync = uiState.hasPassedSlowSync,
                syncLine = uiState.syncLine,
                onConversationSelected = onConversationSelected,
                onCompactConversationSelected = {
                    isCompactConversationListVisible = false
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            WorkspaceDetailPane(
                uiState = uiState,
                draftMessage = draftMessage,
                isSendingMessage = isSendingMessage,
                onDraftMessageChange = onDraftMessageChange,
                onSendMessage = onSendMessage,
                onJoinCall = onJoinCall,
                onAnswerCall = onAnswerCall,
                onRejectCall = onRejectCall,
                onEndCall = onEndCall,
                modifier = Modifier.fillMaxSize(),
                navigationAction = {
                    TextButton(onClick = { isCompactConversationListVisible = true }) {
                        Text("Back")
                    }
                }
            )
        }
    }
}

@Composable
private fun ConversationListPane(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    conversations: List<ConversationListItemUiState>,
    selectedConversationId: ConversationId?,
    hasPassedSlowSync: Boolean,
    syncLine: String,
    onConversationSelected: (ConversationId) -> Unit,
    onCompactConversationSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.wireColorScheme
    val spacing = dimensions()

    Surface(
        modifier = modifier.fillMaxHeight(),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Conversations",
                style = MaterialTheme.wireTypography.title03,
                color = colors.onSurface,
                modifier = Modifier.padding(spacing.spacing16x)
            )
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.spacing16x),
                label = { Text("Search") },
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(spacing.spacing12x))
            if (!hasPassedSlowSync) {
                SyncPlaceholder(syncLine = syncLine)
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(conversations, key = { it.id.value }) { conversation ->
                        ConversationRow(
                            conversation = conversation,
                            isSelected = conversation.id == selectedConversationId,
                            onClick = {
                                onConversationSelected(conversation.id)
                                onCompactConversationSelected()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkspaceDetailPane(
    uiState: KaliumUiState,
    draftMessage: String,
    isSendingMessage: Boolean,
    onDraftMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onJoinCall: () -> Unit,
    onAnswerCall: () -> Unit,
    onRejectCall: () -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier,
    navigationAction: @Composable (() -> Unit)? = null,
) {
    val spacing = dimensions()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.spacing20x),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing16x)
    ) {
        if (navigationAction != null) {
            navigationAction()
        }
        StatusCard(
            runtimeLine = uiState.runtimeLine,
            sessionLine = uiState.sessionLine,
            syncLine = uiState.syncLine,
            errorLine = uiState.errorLine,
        )
        if (!uiState.hasPassedSlowSync) {
            SyncPlaceholder(syncLine = uiState.syncLine)
        } else {
            MessagePane(
                title = uiState.selectedConversationTitle,
                selectedConversationId = uiState.selectedConversationId,
                hasOngoingCall = uiState.selectedConversationId?.let { selectedId ->
                    (uiState.conversations.firstOrNull { it.id == selectedId }?.hasOngoingCall == true) ||
                        (selectedId in uiState.ongoingCallConversationIds)
                } == true,
                isIncomingCall = uiState.selectedConversationId?.let { it in uiState.incomingCallConversationIds } == true,
                messages = uiState.messages,
                draftMessage = draftMessage,
                isSendingMessage = isSendingMessage,
                onDraftMessageChange = onDraftMessageChange,
                onSendMessage = onSendMessage,
                onJoinCall = onJoinCall,
                onAnswerCall = onAnswerCall,
                onRejectCall = onRejectCall,
                onEndCall = onEndCall,
            )
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: ConversationListItemUiState,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.wireColorScheme
    val spacing = dimensions()
    val background = if (isSelected) colors.surfaceVariant else colors.surface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.spacing16x, vertical = spacing.spacing12x)
    ) {
        Text(
            text = conversation.title,
            style = MaterialTheme.wireTypography.title03,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(spacing.spacing4x))
        Text(
            text = "${conversation.subtitle} · ${conversation.protocolLabel}",
            style = MaterialTheme.wireTypography.body03,
            color = colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (conversation.hasOngoingCall) {
            Spacer(modifier = Modifier.height(spacing.spacing4x))
            Text(
                text = "Call in progress",
                style = MaterialTheme.wireTypography.body03,
                color = colors.primary
            )
        }
    }
    HorizontalDivider(color = colors.divider.copy(alpha = 0.5f))
}

@Composable
private fun MessagePane(
    title: String?,
    selectedConversationId: ConversationId?,
    hasOngoingCall: Boolean,
    isIncomingCall: Boolean,
    messages: List<MessageItemUiState>,
    draftMessage: String,
    isSendingMessage: Boolean,
    onDraftMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onJoinCall: () -> Unit,
    onAnswerCall: () -> Unit,
    onRejectCall: () -> Unit,
    onEndCall: () -> Unit,
) {
    val colors = MaterialTheme.wireColorScheme
    val spacing = dimensions()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing12x)
    ) {
        Text(
            text = title ?: "No conversation selected",
            style = MaterialTheme.wireTypography.title02,
            color = colors.onBackground
        )
        if (selectedConversationId != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.spacing8x)) {
                if (isIncomingCall) {
                    Button(onClick = onAnswerCall) { Text("Answer") }
                    TextButton(onClick = onRejectCall) { Text("Reject") }
                } else if (hasOngoingCall) {
                    Button(onClick = onJoinCall) { Text("Join call") }
                    TextButton(onClick = onEndCall) { Text("End") }
                } else {
                    Button(onClick = onJoinCall) { Text("Call") }
                }
            }
        }
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(spacing.spacing16x),
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider.copy(alpha = 0.7f))
        ) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No messages yet",
                        style = MaterialTheme.wireTypography.body02,
                        color = colors.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(spacing.spacing16x),
                    verticalArrangement = Arrangement.spacedBy(spacing.spacing10x)
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageRow(message = message)
                    }
                }
            }
        }

        OutlinedTextField(
            value = draftMessage,
            onValueChange = onDraftMessageChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Message") },
            enabled = !isSendingMessage,
        )
        Button(
            onClick = onSendMessage,
            enabled = draftMessage.isNotBlank() && !isSendingMessage,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(if (isSendingMessage) "Sending..." else "Send")
        }
    }
}

@Composable
private fun MessageRow(message: MessageItemUiState) {
    val colors = MaterialTheme.wireColorScheme
    val spacing = dimensions()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (message.isOwnMessage) colors.primary.copy(alpha = 0.08f) else colors.background,
                RoundedCornerShape(spacing.spacing12x)
            )
            .padding(spacing.spacing12x)
    ) {
        Text(
            text = message.senderLabel,
            style = MaterialTheme.wireTypography.body03,
            color = colors.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(spacing.spacing4x))
        Text(
            text = message.body,
            style = MaterialTheme.wireTypography.body01,
            color = colors.onSurface
        )
    }
}

@Composable
private fun StatusCard(
    runtimeLine: String,
    sessionLine: String,
    syncLine: String,
    errorLine: String?,
) {
    val colors = MaterialTheme.wireColorScheme
    val spacing = dimensions()

    Surface(
        shape = RoundedCornerShape(spacing.spacing16x),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(spacing.spacing16x),
            verticalArrangement = Arrangement.spacedBy(spacing.spacing8x)
        ) {
            Text(runtimeLine, style = MaterialTheme.wireTypography.body02, color = colors.onSurface)
            Text(sessionLine, style = MaterialTheme.wireTypography.body02, color = colors.onSurface)
            Text(syncLine, style = MaterialTheme.wireTypography.body02, color = colors.onSurface)
            if (errorLine != null) {
                Text(errorLine, style = MaterialTheme.wireTypography.body02, color = colors.error)
            }
        }
    }
}

@Composable
private fun SyncPlaceholder(syncLine: String) {
    val colors = MaterialTheme.wireColorScheme
    val spacing = dimensions()

    Box(
        modifier = Modifier.fillMaxSize().padding(spacing.spacing24x),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.spacing12x)
        ) {
            CircularProgressIndicator()
            Text(syncLine, style = MaterialTheme.wireTypography.body02, color = colors.onSurfaceVariant)
        }
    }
}
