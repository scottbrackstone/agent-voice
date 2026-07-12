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
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agentvoice.app.R
import com.agentvoice.app.commands.CommandOption
import com.agentvoice.app.commands.HermesCommand
import com.agentvoice.app.model.AgentMode
import com.agentvoice.app.model.AgentPresentation
import com.agentvoice.app.model.ChatMessage
import com.agentvoice.app.model.ChatMessageRole
import com.agentvoice.app.model.ChatBackground
import com.agentvoice.app.model.ChatThread
import com.agentvoice.app.model.ConversationRecord
import com.agentvoice.app.model.QueuedAction
import com.agentvoice.app.viewmodel.MainUiState
import com.agentvoice.app.ui.commands.CommandPanel
import com.agentvoice.app.ui.commands.CommandSuggestionPanel
import com.agentvoice.app.ui.components.MessageActionSheet
import com.agentvoice.app.ui.components.ThreadActionSheet
import com.agentvoice.app.ui.presentation.SolaraSpriteRenderer
import com.agentvoice.app.ui.presentation.resolveCompanionVisualState
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    uiState: MainUiState,
    onModeSelected: (AgentMode) -> Unit,
    onBackgroundSelected: (String) -> Unit,
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
    onStartMessageSelection: (ChatMessage) -> Unit,
    onToggleMessageSelection: (ChatMessage) -> Unit,
    onClearMessageSelection: () -> Unit,
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
            onBackgroundSelected = onBackgroundSelected,
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
            onMessageClick = onToggleMessageSelection,
            onClearMessageSelection = onClearMessageSelection,
            onCopySelectedMessages = {
                copyToClipboard(
                    context = context,
                    clipboardManager = clipboardManager,
                    label = "Messages",
                    text = copyableMessagesText(uiState.selectedMessages)
                )
                onClearMessageSelection()
            },
            onCommandSelected = onCommandSelected,
            onCommandOptionSelected = onCommandOptionSelected,
            onCommandTextChange = onCommandTextChange,
            onSubmitCommandText = onSubmitCommandText,
            onSkipCommandText = onSkipCommandText,
            onCommandBack = onCommandBack,
            onCancelCommandPicker = onCancelCommandPicker,
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
            previewText = visibleMessageText(message),
            onCopyMessage = {
                copyToClipboard(
                    context = context,
                    clipboardManager = clipboardManager,
                    label = "Message",
                    text = visibleMessageText(message)
                )
                onCloseMessageActions()
            },
            onSelectMessages = { onStartMessageSelection(message) },
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
                    isCreatingThread = uiState.isCreatingThread,
                    isRetry = uiState.pendingThreadCreateId != null && !uiState.isCreatingThread,
                    onNewThreadClick = onNewThreadClick
                )
            }

            if (uiState.pendingThreadCreateId != null && uiState.errorMessage != null) {
                item {
                    ErrorStrip(
                        body = uiState.errorMessage,
                        canRetry = !uiState.isCreatingThread,
                        onRetry = onNewThreadClick
                    )
                }
            }

            item {
                SectionLabel("Threads")
            }

            if (uiState.threads.isEmpty()) {
                item {
                    EmptyThreadList(
                        isCreatingThread = uiState.isCreatingThread,
                        onNewThreadClick = onNewThreadClick
                    )
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
    onBackgroundSelected: (String) -> Unit,
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
    onMessageClick: (ChatMessage) -> Unit,
    onClearMessageSelection: () -> Unit,
    onCopySelectedMessages: () -> Unit,
    onCommandSelected: (HermesCommand) -> Unit,
    onCommandOptionSelected: (CommandOption) -> Unit,
    onCommandTextChange: (String) -> Unit,
    onSubmitCommandText: () -> Unit,
    onSkipCommandText: () -> Unit,
    onCommandBack: () -> Unit,
    onCancelCommandPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedThread = uiState.selectedThread
    var showBackgroundPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (uiState.isMessageSelectionMode) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onClearMessageSelection) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel message selection")
                        }
                    },
                    title = {
                        Text("${uiState.selectedMessageIds.size} selected", fontWeight = FontWeight.Bold)
                    },
                    actions = {
                        IconButton(onClick = onCopySelectedMessages) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy selected messages")
                        }
                    }
                )
            } else TopAppBar(
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
                    IconButton(
                        onClick = { showBackgroundPicker = true },
                        enabled = selectedThread != null
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = "Chat background")
                    }
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
            Column {
                val command = uiState.activeCommand
                val step = uiState.activeCommandStep
                if (command != null && step != null) {
                    CommandPanel(
                        command = command,
                        step = step,
                        stepIndex = uiState.activeCommandStepIndex,
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
                ComposerBar(
                    uiState = uiState,
                    onTalkClick = onTalkClick,
                    onTypedMessageChange = onTypedMessageChange,
                    onSendTypedMessage = onSendTypedMessage,
                    onCommandSelected = onCommandSelected
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            ChatBackdrop(selectedThread?.backgroundId ?: ChatBackground.DEFAULT_ID)
            Column(modifier = Modifier.fillMaxSize()) {
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
                    selectedMessageIds = uiState.selectedMessageIds,
                    selectionMode = uiState.isMessageSelectionMode,
                    onMessageClick = onMessageClick,
                    onMessageLongPress = onMessageLongPress
                )
            }
        }
    }

    if (showBackgroundPicker) {
        BackgroundPickerSheet(
            selectedId = selectedThread?.backgroundId ?: ChatBackground.DEFAULT_ID,
            onSelected = { background ->
                onBackgroundSelected(background.id)
                showBackgroundPicker = false
            },
            onDismiss = { showBackgroundPicker = false }
        )
    }
}

private data class BackdropPalette(
    val base: Color,
    val secondary: Color,
    val accent: Color,
    val line: Color
)

@Composable
private fun ChatBackdrop(backgroundId: String) {
    val palette = when (ChatBackground.fromId(backgroundId)) {
        ChatBackground.SakuraSignal -> BackdropPalette(
            base = Color(0xFFFFE9F2),
            secondary = Color(0xFFD9FFF7),
            accent = Color(0xFFFF6FA9),
            line = Color(0xFF28A9A1)
        )
        ChatBackground.NeonBloom -> BackdropPalette(
            base = Color(0xFFD8FFF6),
            secondary = Color(0xFFFFE16B),
            accent = Color(0xFFFF4F9A),
            line = Color(0xFF087F8C)
        )
        ChatBackground.ElectricDawn -> BackdropPalette(
            base = Color(0xFFFFF1A8),
            secondary = Color(0xFFB8F5E5),
            accent = Color(0xFFFF5D73),
            line = Color(0xFF247BA0)
        )
        ChatBackground.MidnightArcade -> BackdropPalette(
            base = Color(0xFF171331),
            secondary = Color(0xFF2B1F55),
            accent = Color(0xFFFF5DA2),
            line = Color(0xFF4CE7D8)
        )
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(palette.base)
        drawRect(
            color = palette.secondary.copy(alpha = 0.32f),
            topLeft = Offset(0f, size.height * 0.48f),
            size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.52f)
        )

        val grid = 44.dp.toPx()
        var x = -size.height
        while (x < size.width) {
            drawLine(
                color = palette.line.copy(alpha = 0.13f),
                start = Offset(x, 0f),
                end = Offset(x + size.height, size.height),
                strokeWidth = 1.dp.toPx()
            )
            x += grid
        }

        var y = grid
        while (y < size.height) {
            drawLine(
                color = palette.accent.copy(alpha = 0.10f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
            y += grid * 1.7f
        }

        repeat(7) { index ->
            val left = (index * 83.dp.toPx()) % size.width
            val top = (index * 137.dp.toPx()) % size.height
            val tile = 9.dp.toPx()
            drawRect(
                color = palette.accent.copy(alpha = 0.20f),
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(tile, tile),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackgroundPickerSheet(
    selectedId: String,
    onSelected: (ChatBackground) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF151326)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Chat atmosphere",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF8F4FF)
            )
            ChatBackground.entries.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { background ->
                        val selected = background.id == selectedId
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(118.dp)
                                .then(
                                    if (selected) {
                                        Modifier.border(
                                            2.dp,
                                            Color(0xFFFFD166),
                                            RoundedCornerShape(8.dp)
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { onSelected(background) },
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Transparent
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                ChatBackdrop(background.id)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(Color(0xDD151326))
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        background.label,
                                        color = Color(0xFFF8F4FF),
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (selected) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp),
                                        shape = CircleShape,
                                        color = Color(0xFFFFD166)
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color(0xFF171331),
                                            modifier = Modifier.padding(4.dp).size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun CompanionHeader(
    status: String,
    isActive: Boolean,
    isCreatingThread: Boolean,
    isRetry: Boolean,
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
            CompanionAvatar(isActive = isActive, size = 104.dp)
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
                enabled = !isCreatingThread,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (isCreatingThread) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        if (isRetry) Icons.Default.Refresh else Icons.Default.Add,
                        contentDescription = null
                    )
                }
                Spacer(Modifier.size(8.dp))
                Text(
                    when {
                        isCreatingThread -> "Creating"
                        isRetry -> "Retry"
                        else -> "New"
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyThreadList(
    isCreatingThread: Boolean,
    onNewThreadClick: () -> Unit
) {
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
            OutlinedButton(
                onClick = onNewThreadClick,
                enabled = !isCreatingThread,
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isCreatingThread) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
                Spacer(Modifier.size(8.dp))
                Text(if (isCreatingThread) "Creating" else "Create thread")
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                CompanionAvatar(
                    isActive = uiState.isLoading || uiState.isListening || uiState.isSpeaking,
                    size = 96.dp,
                    presentation = uiState.presentation,
                    isListening = uiState.isListening,
                    isSpeaking = uiState.isSpeaking || uiState.streamingReply.isNotBlank()
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "Solara",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        uiState.connectionStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        companionStatus(uiState),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onSpeakAgainClick,
                        enabled = uiState.canSpeakAgain
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Speak again")
                    }
                    Switch(checked = uiState.ttsEnabled, onCheckedChange = onTtsToggle)
                }
            }

            ModeSelector(selectedMode = uiState.mode, onModeSelected = onModeSelected)

            uiState.toolProgressMessage?.let { progress ->
                Text(
                    progress,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (uiState.mode.showsCaptureNotice) {
                SafetyNotice()
            }

            if (uiState.queuedActions.isNotEmpty()) {
                QueuedActionPanel(uiState.queuedActions)
            }
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
    selectedMessageIds: Set<String>,
    selectionMode: Boolean,
    onMessageClick: (ChatMessage) -> Unit,
    onMessageLongPress: (ChatMessage) -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val latestMessageId = messages.lastOrNull()?.id
    var previousMessageCount by remember { mutableIntStateOf(0) }
    var unreadBelow by remember { mutableIntStateOf(0) }
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
            val previousCount = previousMessageCount
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val wasNearBottom = previousCount == 0 || lastVisibleIndex >= previousCount - 2
            val sentByUser = messages.lastOrNull()?.role == ChatMessageRole.User
            val addedCount = (messages.size - previousCount).coerceAtLeast(0)
            if (wasNearBottom || sentByUser) {
                listState.animateScrollToItem(messages.lastIndex)
                unreadBelow = 0
            } else {
                unreadBelow += addedCount
            }
            previousMessageCount = messages.size
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
                            selected = message.id in selectedMessageIds,
                            selectionMode = selectionMode,
                            onClick = { onMessageClick(message) },
                            onLongClick = {
                                if (selectionMode) {
                                    onMessageClick(message)
                                } else {
                                    onMessageLongPress(message)
                                }
                            }
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
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 4.dp
            ) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(messages.size)
                            unreadBelow = 0
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = if (unreadBelow > 0) {
                            "$unreadBelow new messages. Scroll to latest"
                        } else {
                            "Scroll to latest"
                        }
                    )
                }
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
            CompanionAvatar(isActive = isBusy, size = 112.dp)
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
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
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
                .then(
                    if (selected) {
                        Modifier.border(
                            width = 2.dp,
                            color = SoftGold,
                            shape = RoundedCornerShape(
                                topStart = 8.dp,
                                topEnd = 8.dp,
                                bottomStart = if (isUser) 8.dp else 2.dp,
                                bottomEnd = if (isUser) 2.dp else 8.dp
                            )
                        )
                    } else {
                        Modifier
                    }
                )
                .combinedClickable(
                    onClick = {
                        if (selectionMode) onClick()
                    },
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
                if (selected) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
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
    val focusManager = LocalFocusManager.current

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
                CommandSuggestionPanel(
                    commands = uiState.commandSuggestions,
                    onCommandSelected = { command ->
                        focusManager.clearFocus(force = true)
                        onCommandSelected(command)
                    }
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
                    placeholder = { Text("Message ${uiState.selectedAgent.label}") },
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
    size: androidx.compose.ui.unit.Dp,
    presentation: AgentPresentation = AgentPresentation(),
    isListening: Boolean = false,
    isSpeaking: Boolean = false
) {
    SolaraSpriteRenderer.Render(
        state = resolveCompanionVisualState(
            presentation = presentation,
            isListening = isListening,
            isSpeaking = isSpeaking,
            isActive = isActive
        ),
        size = size,
        modifier = Modifier
    )
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

private fun companionStatus(uiState: MainUiState): String {
    val activity = when {
        uiState.isListening -> "listening"
        uiState.isSpeaking -> "speaking"
        uiState.isLoading -> uiState.presentation.state.ifBlank { "thinking" }
        else -> uiState.presentation.state.ifBlank { "ready" }
    }
    val mood = uiState.presentation.mood
        .takeUnless { it.isBlank() || it.equals("neutral", ignoreCase = true) }
    return listOfNotNull(mood, activity, uiState.selectedAgent.label)
        .joinToString(separator = "  |  ")
}

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
