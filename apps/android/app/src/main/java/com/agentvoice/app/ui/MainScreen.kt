package com.agentvoice.app.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agentvoice.app.R
import com.agentvoice.app.commands.CommandOption
import com.agentvoice.app.commands.HermesCommand
import com.agentvoice.app.model.AgentMode
import com.agentvoice.app.model.ChatMessage
import com.agentvoice.app.model.ChatMessageRole
import com.agentvoice.app.model.ChatThread
import com.agentvoice.app.model.ConversationRecord
import com.agentvoice.app.model.QueuedAction
import com.agentvoice.app.viewmodel.MainUiState
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    uiState: MainUiState,
    onModeSelected: (AgentMode) -> Unit,
    onTalkClick: () -> Unit,
    onTypedMessageChange: (String) -> Unit,
    onSendTypedMessage: () -> Unit,
    onRetryClick: () -> Unit,
    onSpeakAgainClick: () -> Unit,
    onDrivingModeClick: () -> Unit,
    onThreadSelected: (ChatThread) -> Unit,
    onThreadLongPress: (ChatThread) -> Unit,
    onNewThreadClick: () -> Unit,
    onBackToThreads: () -> Unit,
    onRenameThreadClick: () -> Unit,
    onRenameThreadFromActions: () -> Unit,
    onRequestDeleteThread: () -> Unit,
    onConfirmDeleteThread: () -> Unit,
    onCancelDeleteThread: () -> Unit,
    onCloseThreadActions: () -> Unit,
    onThreadTitleDraftChange: (String) -> Unit,
    onSaveThreadTitle: () -> Unit,
    onCancelRenameThread: () -> Unit,
    onMessageLongPress: (ChatMessage) -> Unit,
    onCloseMessageActions: () -> Unit,
    onCommandSelected: (HermesCommand) -> Unit,
    onCommandOptionSelected: (CommandOption) -> Unit,
    onCommandTextChange: (String) -> Unit,
    onSubmitCommandText: () -> Unit,
    onSkipCommandText: () -> Unit,
    onCommandBack: () -> Unit,
    onCancelCommandPicker: () -> Unit,
    onHistorySelected: (ConversationRecord) -> Unit,
    onDismissHistory: () -> Unit,
    onTtsToggle: (Boolean) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    if (uiState.isThreadListVisible) {
        ThreadListScreen(
            uiState = uiState,
            onThreadSelected = onThreadSelected,
            onThreadLongPress = onThreadLongPress,
            onNewThreadClick = onNewThreadClick,
            onDrivingModeClick = onDrivingModeClick,
            onHistorySelected = onHistorySelected,
            onDismissHistory = onDismissHistory,
            onSettingsClick = onSettingsClick,
            modifier = modifier
        )
    } else {
        ChatScreen(
            uiState = uiState,
            onModeSelected = onModeSelected,
            onTalkClick = onTalkClick,
            onTypedMessageChange = onTypedMessageChange,
            onSendTypedMessage = onSendTypedMessage,
            onRetryClick = onRetryClick,
            onSpeakAgainClick = onSpeakAgainClick,
            onDrivingModeClick = onDrivingModeClick,
            onBackToThreads = onBackToThreads,
            onRenameThreadClick = onRenameThreadClick,
            onTtsToggle = onTtsToggle,
            onSettingsClick = onSettingsClick,
            onMessageLongPress = onMessageLongPress,
            onCommandSelected = onCommandSelected,
            modifier = modifier
        )
    }

    if (uiState.showRenameThread) {
        RenameThreadDialog(
            title = uiState.threadTitleDraft,
            onTitleChange = onThreadTitleDraftChange,
            onSave = onSaveThreadTitle,
            onCancel = onCancelRenameThread
        )
    }

    if (uiState.showThreadActions) {
        uiState.actionThread?.let { thread ->
            ThreadActionSheet(
                thread = thread,
                onRename = onRenameThreadFromActions,
                onDelete = onRequestDeleteThread,
                onDismiss = onCloseThreadActions
            )
        }
    }

    if (uiState.showDeleteThreadConfirmation) {
        uiState.actionThread?.let { thread ->
            DeleteThreadDialog(
                thread = thread,
                onConfirm = onConfirmDeleteThread,
                onCancel = onCancelDeleteThread
            )
        }
    }

    uiState.activeMessageAction?.let { message ->
        MessageActionSheet(
            message = message,
            onCopyMessage = {
                copyToClipboard(
                    context = context,
                    clipboardManager = clipboardManager,
                    label = "Message",
                    text = visibleMessageText(message)
                )
                onCloseMessageActions()
            },
            onCopyFromHere = {
                val selectedIndex = uiState.activeThreadMessages.indexOfFirst {
                    it.id == message.id
                }
                if (selectedIndex >= 0) {
                    copyToClipboard(
                        context = context,
                        clipboardManager = clipboardManager,
                        label = "Messages",
                        text = copyableMessagesText(uiState.activeThreadMessages.drop(selectedIndex))
                    )
                }
                onCloseMessageActions()
            },
            onCopyThread = {
                copyToClipboard(
                    context = context,
                    clipboardManager = clipboardManager,
                    label = "Thread",
                    text = copyableMessagesText(uiState.activeThreadMessages)
                )
                onCloseMessageActions()
            },
            onDismiss = onCloseMessageActions
        )
    }

    uiState.activeCommand?.let { command ->
        CommandPickerSheet(
            command = command,
            step = uiState.activeCommandStep,
            options = uiState.activeCommandOptions,
            textDraft = uiState.commandTextDraft,
            pickerError = uiState.commandPickerError,
            onOptionSelected = onCommandOptionSelected,
            onTextChange = onCommandTextChange,
            onSubmitText = onSubmitCommandText,
            onSkipText = onSkipCommandText,
            onBack = onCommandBack,
            onDismiss = onCancelCommandPicker
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadListScreen(
    uiState: MainUiState,
    onThreadSelected: (ChatThread) -> Unit,
    onThreadLongPress: (ChatThread) -> Unit,
    onNewThreadClick: () -> Unit,
    onDrivingModeClick: () -> Unit,
    onHistorySelected: (ConversationRecord) -> Unit,
    onDismissHistory: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AgentVoice", fontWeight = FontWeight.SemiBold)
                        Text(
                            uiState.connectionStatus,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onDrivingModeClick) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = "Driving mode")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                CompanionHeader(
                    status = uiState.lastConnector?.let { "Last reply via ${it.label}" }
                        ?: "Ready",
                    isActive = uiState.isListening || uiState.isLoading,
                    onNewThreadClick = onNewThreadClick
                )
            }

            item {
                SectionLabel("Threads")
            }

            if (uiState.threads.isEmpty()) {
                item {
                    EmptyThreadList(onNewThreadClick = onNewThreadClick)
                }
            } else {
                items(uiState.threads, key = { it.id }) { thread ->
                    ThreadRow(
                        thread = thread,
                        isSelected = thread.id == uiState.selectedThreadId,
                        onClick = { onThreadSelected(thread) },
                        onLongClick = { onThreadLongPress(thread) }
                    )
                }
            }

            if (uiState.recentHistory.isNotEmpty()) {
                item {
                    RecentHistoryCard(
                        records = uiState.recentHistory,
                        onHistorySelected = onHistorySelected
                    )
                }
            }

            uiState.selectedHistory?.let { selected ->
                item {
                    HistoryDetailCard(
                        record = selected,
                        onDismiss = onDismissHistory
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: MainUiState,
    onModeSelected: (AgentMode) -> Unit,
    onTalkClick: () -> Unit,
    onTypedMessageChange: (String) -> Unit,
    onSendTypedMessage: () -> Unit,
    onRetryClick: () -> Unit,
    onSpeakAgainClick: () -> Unit,
    onDrivingModeClick: () -> Unit,
    onBackToThreads: () -> Unit,
    onRenameThreadClick: () -> Unit,
    onTtsToggle: (Boolean) -> Unit,
    onSettingsClick: () -> Unit,
    onMessageLongPress: (ChatMessage) -> Unit,
    onCommandSelected: (HermesCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedThread = uiState.selectedThread

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackToThreads) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Threads")
                    }
                },
                title = {
                    Column {
                        Text(
                            selectedThread?.title ?: "Chat",
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${uiState.selectedAgent.label} - ${uiState.mode.label}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRenameThreadClick, enabled = selectedThread != null) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename thread")
                    }
                    IconButton(onClick = onDrivingModeClick) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = "Driving mode")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            ComposerBar(
                uiState = uiState,
                onTalkClick = onTalkClick,
                onTypedMessageChange = onTypedMessageChange,
                onSendTypedMessage = onSendTypedMessage,
                onCommandSelected = onCommandSelected
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            ChatStatusHeader(
                uiState = uiState,
                onModeSelected = onModeSelected,
                onTtsToggle = onTtsToggle,
                onSpeakAgainClick = onSpeakAgainClick
            )

            if (uiState.isLoading || uiState.isListening || uiState.isTranscribingVoiceClip) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            uiState.errorMessage?.let { error ->
                ErrorStrip(
                    body = error,
                    canRetry = uiState.canRetry,
                    onRetry = onRetryClick
                )
            }

            MessageList(
                messages = uiState.activeThreadMessages,
                isBusy = uiState.isLoading || uiState.isListening || uiState.isTranscribingVoiceClip,
                onMessageLongPress = onMessageLongPress
            )
        }
    }
}

@Composable
private fun CompanionHeader(
    status: String,
    isActive: Boolean,
    onNewThreadClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CompanionAvatar(isActive = isActive, size = 82.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "Solara",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Button(
                onClick = onNewThreadClick,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("New")
            }
        }
    }
}

@Composable
private fun EmptyThreadList(onNewThreadClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text("No threads yet.", style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onNewThreadClick, shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Create thread")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ThreadRow(
    thread: ChatThread,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompanionAvatar(isActive = false, size = 44.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    thread.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${thread.connector.label} - ${thread.mode.label} - ${formatTimestamp(thread.updatedAtMillis)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatStatusHeader(
    uiState: MainUiState,
    onModeSelected: (AgentMode) -> Unit,
    onTtsToggle: (Boolean) -> Unit,
    onSpeakAgainClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompanionAvatar(
                isActive = uiState.isLoading || uiState.isListening || uiState.isSpeaking,
                size = 54.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    uiState.connectionStatus,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    uiState.selectedAgent.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onSpeakAgainClick,
                enabled = uiState.canSpeakAgain
            ) {
                Icon(Icons.Default.VolumeUp, contentDescription = "Speak again")
            }
            Switch(checked = uiState.ttsEnabled, onCheckedChange = onTtsToggle)
        }

        ModeSelector(selectedMode = uiState.mode, onModeSelected = onModeSelected)

        if (uiState.mode.showsCaptureNotice) {
            SafetyNotice()
        }

        if (uiState.queuedActions.isNotEmpty()) {
            QueuedActionPanel(uiState.queuedActions)
        }
    }
}

@Composable
private fun SafetyNotice() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = SoftGold.copy(alpha = 0.28f)
    ) {
        Text(
            "Capture now. Confirm later.",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF6C5616)
        )
    }
}

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    isBusy: Boolean,
    onMessageLongPress: (ChatMessage) -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val latestMessageId = messages.lastOrNull()?.id
    val showScrollToBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf false
            lastVisible.index < layoutInfo.totalItemsCount - 1 ||
                lastVisible.offset + lastVisible.size > layoutInfo.viewportEndOffset
        }
    }

    LaunchedEffect(latestMessageId) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    EmptyChatState(isBusy = isBusy)
                }
            } else {
                items(messages, key = { it.id }) { message ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(180)) +
                            slideInVertically(
                                animationSpec = tween(220),
                                initialOffsetY = { it / 4 }
                            )
                    ) {
                        MessageBubble(
                            message = message,
                            onLongClick = { onMessageLongPress(message) }
                        )
                    }
                }
                item(key = "chat-bottom") {
                    Spacer(Modifier.height(1.dp))
                }
            }
        }

        AnimatedVisibility(
            visible = showScrollToBottom && messages.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp)
        ) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(messages.size)
                    }
                },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Scroll to latest"
                )
            }
        }
    }
}
@Composable
private fun EmptyChatState(isBusy: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CompanionAvatar(isActive = isBusy, size = 92.dp)
            Text(
                if (isBusy) "Thinking..." else "Start a thread.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: ChatMessage,
    onLongClick: () -> Unit
) {
    val isUser = message.role == ChatMessageRole.User
    val bubbleColor = when {
        isUser -> MaterialTheme.colorScheme.primary
        message.error != null -> MaterialTheme.colorScheme.errorContainer
        message.status.wireValue == "queued" -> SoftGold.copy(alpha = 0.34f)
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when {
        isUser -> MaterialTheme.colorScheme.onPrimary
        message.error != null -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 312.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongClick
                ),
            shape = RoundedCornerShape(
                topStart = 8.dp,
                topEnd = 8.dp,
                bottomStart = if (isUser) 8.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 8.dp
            ),
            color = bubbleColor,
            tonalElevation = if (isUser) 0.dp else 1.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    visibleMessageText(message),
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor
                )
                if (message.queuedActionsSummary.isNotBlank()) {
                    Text(
                        message.queuedActionsSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.82f)
                    )
                }
                Text(
                    "${message.role.label} - ${message.status.label} - ${
                        formatTimestamp(message.timestampMillis)
                    }",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.72f)
                )
            }
        }
    }
}

@Composable
private fun ComposerBar(
    uiState: MainUiState,
    onTalkClick: () -> Unit,
    onTypedMessageChange: (String) -> Unit,
    onSendTypedMessage: () -> Unit,
    onCommandSelected: (HermesCommand) -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.isRecordingVoiceClip || uiState.isTranscribingVoiceClip) {
                Text(
                    if (uiState.isTranscribingVoiceClip) "Transcribing..." else "Recording...",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (uiState.commandSuggestions.isNotEmpty()) {
                CommandSuggestionBar(
                    commands = uiState.commandSuggestions,
                    onCommandSelected = onCommandSelected
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MicPulseButton(
                    active = uiState.isListening || uiState.isRecordingVoiceClip,
                    enabled = !uiState.isLoading &&
                        !uiState.isListening &&
                        !uiState.isTranscribingVoiceClip,
                    onClick = onTalkClick
                )
                OutlinedTextField(
                    value = uiState.typedMessage,
                    onValueChange = onTypedMessageChange,
                    enabled = !uiState.isLoading && !uiState.isListening,
                    placeholder = { Text("Message Hermes") },
                    minLines = 1,
                    maxLines = 4,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
                IconButton(
                    onClick = onSendTypedMessage,
                    enabled = uiState.canSendTypedMessage,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@Composable
private fun MicPulseButton(
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "micPulse")
    val pulse by transition.animateFloat(
        initialValue = if (active) 0.92f else 1f,
        targetValue = if (active) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micPulseScale"
    )

    Box(
        modifier = Modifier.size(52.dp),
        contentAlignment = Alignment.Center
    ) {
        if (active) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .graphicsLayer(scaleX = pulse, scaleY = pulse)
                    .clip(CircleShape)
                    .background(CompanionPink.copy(alpha = 0.32f))
            )
        }
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = CompanionPink),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(Icons.Default.Mic, contentDescription = "Push to talk")
        }
    }
}

@Composable
private fun CompanionAvatar(
    isActive: Boolean,
    size: androidx.compose.ui.unit.Dp
) {
    val transition = rememberInfiniteTransition(label = "avatarBreath")
    val scale by transition.animateFloat(
        initialValue = if (isActive) 0.97f else 1f,
        targetValue = if (isActive) 1.04f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatarBreathScale"
    )

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(CircleShape)
            .background(SoftTeal.copy(alpha = 0.26f))
            .border(2.dp, CompanionPink.copy(alpha = 0.72f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.agent_companion),
            contentDescription = "AgentVoice companion",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ModeSelector(
    selectedMode: AgentMode,
    onModeSelected: (AgentMode) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AgentMode.entries.forEach { mode ->
            FilterChip(
                selected = mode == selectedMode,
                onClick = { onModeSelected(mode) },
                label = { Text(mode.label) }
            )
        }
    }
}

@Composable
private fun QueuedActionPanel(actions: List<QueuedAction>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Queued actions",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            actions.take(3).forEach { action ->
                Text(
                    "${action.type.label}: ${action.summary}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CommandSuggestionBar(
    commands: List<HermesCommand>,
    onCommandSelected: (HermesCommand) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        commands.forEach { command ->
            FilterChip(
                selected = false,
                onClick = { onCommandSelected(command) },
                label = {
                    Text(
                        command.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThreadActionSheet(
    thread: ChatThread,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                thread.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            TextButton(
                onClick = onRename,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Rename chat")
            }
            TextButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Delete from this app")
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun DeleteThreadDialog(
    thread: ChatThread,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Delete chat?") },
        text = {
            Text(
                "This removes \"${thread.title}\" from this app only. Solara and Hermes history are not deleted."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageActionSheet(
    message: ChatMessage,
    onCopyMessage: () -> Unit,
    onCopyFromHere: () -> Unit,
    onCopyThread: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Message",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                visibleMessageText(message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            TextButton(
                onClick = onCopyMessage,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Copy message")
            }
            TextButton(
                onClick = onCopyFromHere,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Copy from here")
            }
            TextButton(
                onClick = onCopyThread,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Copy whole thread")
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommandPickerSheet(
    command: HermesCommand,
    step: com.agentvoice.app.commands.CommandStep?,
    options: List<CommandOption>,
    textDraft: String,
    pickerError: String?,
    onOptionSelected: (CommandOption) -> Unit,
    onTextChange: (String) -> Unit,
    onSubmitText: () -> Unit,
    onSkipText: () -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                command.label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (command.description.isNotBlank()) {
                Text(
                    command.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                step?.label ?: "Choose",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (step?.type == "text") {
                OutlinedTextField(
                    value = textDraft,
                    onValueChange = onTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = step.placeholder?.let { placeholder ->
                        { Text(placeholder) }
                    },
                    singleLine = true,
                    isError = pickerError != null
                )
                pickerError?.let { error ->
                    Text(
                        error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (step.required.not()) {
                        OutlinedButton(
                            onClick = onSkipText,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Skip")
                        }
                    }
                    Button(
                        onClick = onSubmitText,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        enabled = step.required.not() || textDraft.isNotBlank()
                    ) {
                        Text("Continue")
                    }
                }
            } else {
                if (options.isEmpty()) {
                    Text(
                        "No options available for this step.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    options.forEach { option ->
                        Button(
                            onClick = { onOptionSelected(option) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (option.selected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(option.label)
                                    option.description?.let { description ->
                                        Text(
                                            description,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (command.steps.indexOf(step) > 0) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Back")
                    }
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Cancel")
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun ErrorStrip(
    body: String,
    canRetry: Boolean,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                body,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            if (canRetry) {
                IconButton(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, contentDescription = "Retry")
                }
            }
        }
    }
}

@Composable
private fun RenameThreadDialog(
    title: String,
    onTitleChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Rename thread") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RecentHistoryCard(
    records: List<ConversationRecord>,
    onHistorySelected: (ConversationRecord) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionLabel("Local activity")
            records.take(4).forEach { record ->
                TextButton(
                    onClick = { onHistorySelected(record) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            "${formatTimestamp(record.timestampMillis)} - ${record.mode.label}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            record.transcript,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryDetailCard(
    record: ConversationRecord,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "History detail",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            DetailLine("When", formatTimestamp(record.timestampMillis))
            DetailLine("Mode", record.mode.label)
            DetailLine("Connector", record.connector.label)
            DetailLine("Status", record.status.wireValue)
            DetailBlock("Transcript", record.transcript)
            DetailBlock("Reply", record.reply.ifBlank { "No reply saved." })
            if (record.queuedActionsSummary.isNotBlank()) {
                DetailBlock("Queued actions", record.queuedActionsSummary)
            }
            if (!record.error.isNullOrBlank()) {
                DetailBlock("Error", record.error)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        copyToClipboard(context, clipboardManager, "Transcript", record.transcript)
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Copy transcript")
                }
                OutlinedButton(
                    onClick = {
                        copyToClipboard(context, clipboardManager, "Reply", record.reply)
                    },
                    enabled = record.reply.isNotBlank()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Copy reply")
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DetailBlock(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun formatTimestamp(timestampMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        .format(Date(timestampMillis))

private fun copyToClipboard(
    context: Context,
    clipboardManager: ClipboardManager,
    label: String,
    text: String
) {
    if (text.isBlank()) {
        return
    }

    clipboardManager.setText(AnnotatedString(text))
    Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
}

private fun visibleMessageText(message: ChatMessage): String =
    message.text.ifBlank { message.error ?: "" }

private fun copyableMessagesText(messages: List<ChatMessage>): String =
    messages.joinToString(separator = "\n\n") { message ->
        buildString {
            append(message.role.label)
            append(" - ")
            append(message.status.label)
            append(" - ")
            append(formatTimestamp(message.timestampMillis))
            append("\n")
            append(visibleMessageText(message))
            if (message.queuedActionsSummary.isNotBlank()) {
                append("\n")
                append(message.queuedActionsSummary)
            }
        }
    }

private val SoftTeal = Color(0xFF8AD7D0)
private val CompanionPink = Color(0xFFFF89B7)
private val SoftGold = Color(0xFFFFD76A)
