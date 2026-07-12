package com.agentvoice.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agentvoice.app.model.ChatMessage
import com.agentvoice.app.model.ChatThread

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadActionSheet(
    thread: ChatThread,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AgentVoiceActionSheet(
        title = thread.title,
        onDismiss = onDismiss
    ) {
        ActionRow(Icons.Default.Edit, "Rename chat", AgentVoiceColors.NeonTeal, onRename)
        ActionRow(Icons.Default.Delete, "Delete from this app", AgentVoiceColors.Sakura, onDelete)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageActionSheet(
    message: ChatMessage,
    previewText: String,
    onCopyMessage: () -> Unit,
    onSelectMessages: () -> Unit,
    onCopyFromHere: () -> Unit,
    onCopyThread: () -> Unit,
    onDismiss: () -> Unit
) {
    AgentVoiceActionSheet(
        title = message.role.label,
        subtitle = previewText,
        onDismiss = onDismiss
    ) {
        ActionRow(Icons.Default.ContentCopy, "Copy message", AgentVoiceColors.NeonTeal, onCopyMessage)
        ActionRow(Icons.Default.SelectAll, "Select messages", AgentVoiceColors.SignalGold, onSelectMessages)
        Divider(color = Color.White.copy(alpha = 0.08f))
        ActionRow(Icons.Default.ContentCopy, "Copy from here", AgentVoiceColors.MutedLavender, onCopyFromHere)
        ActionRow(Icons.Default.ContentCopy, "Copy whole thread", AgentVoiceColors.MutedLavender, onCopyThread)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgentVoiceActionSheet(
    title: String,
    subtitle: String? = null,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AgentVoiceColors.Ink,
        contentColor = AgentVoiceColors.SoftWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            subtitle?.let {
                Text(
                    it,
                    color = AgentVoiceColors.MutedLavender,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            content()
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, color = AgentVoiceColors.SoftWhite, fontWeight = FontWeight.SemiBold)
    }
}
