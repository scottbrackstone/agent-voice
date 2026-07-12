package com.agentvoice.app.ui.commands

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agentvoice.app.commands.CommandOption
import com.agentvoice.app.commands.CommandStep
import com.agentvoice.app.commands.HermesCommand
import com.agentvoice.app.ui.components.AgentVoiceColors

@Composable
fun CommandPanel(
    command: HermesCommand,
    step: CommandStep,
    stepIndex: Int,
    options: List<CommandOption>,
    textDraft: String,
    pickerError: String?,
    onOptionSelected: (CommandOption) -> Unit,
    onTextChange: (String) -> Unit,
    onSubmitText: () -> Unit,
    onSkipText: () -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember(command.name, step.id) { mutableStateOf("") }
    val filteredOptions = remember(options, query) {
        val needle = query.trim()
        if (needle.isBlank()) {
            options
        } else {
            options.filter { option ->
                option.label.contains(needle, ignoreCase = true) ||
                    option.value.contains(needle, ignoreCase = true) ||
                    option.description?.contains(needle, ignoreCase = true) == true
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("command_panel"),
        color = AgentVoiceColors.Ink,
        contentColor = AgentVoiceColors.SoftWhite,
        tonalElevation = 5.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (stepIndex > 0) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Previous command step")
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(command.label, fontWeight = FontWeight.Bold)
                    Text(
                        "${step.label}  ${stepIndex + 1}/${command.steps.size}",
                        color = AgentVoiceColors.MutedLavender,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close command menu")
                }
            }

            Crossfade(
                targetState = step.id,
                animationSpec = tween(180),
                label = "commandStep"
            ) {
                if (step.type == "text") {
                    CommandTextStep(
                        step = step,
                        textDraft = textDraft,
                        pickerError = pickerError,
                        onTextChange = onTextChange,
                        onSubmitText = onSubmitText,
                        onSkipText = onSkipText
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (options.size > SEARCH_THRESHOLD) {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("command_search"),
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null)
                                },
                                placeholder = { Text("Search") },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = commandTextFieldColors()
                            )
                        }
                        if (filteredOptions.isEmpty()) {
                            Text("No matches", color = AgentVoiceColors.MutedLavender)
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 330.dp)
                                    .testTag("command_options"),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(filteredOptions, key = { it.value }) { option ->
                                    CommandOptionRow(option, onOptionSelected)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommandSuggestionPanel(
    commands: List<HermesCommand>,
    onCommandSelected: (HermesCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = AgentVoiceColors.InkRaised,
        contentColor = AgentVoiceColors.SoftWhite,
        tonalElevation = 4.dp
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp)
                .padding(vertical = 6.dp)
        ) {
            items(commands, key = { it.name }) { command ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCommandSelected(command) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(command.name, color = AgentVoiceColors.NeonTeal, fontWeight = FontWeight.Bold)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(command.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (command.description.isNotBlank()) {
                            Text(
                                command.description,
                                color = AgentVoiceColors.MutedLavender,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandOptionRow(
    option: CommandOption,
    onOptionSelected: (CommandOption) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOptionSelected(option) }
            .testTag("command_option_${option.value}"),
        shape = RoundedCornerShape(8.dp),
        color = if (option.selected) {
            AgentVoiceColors.NeonTeal.copy(alpha = 0.18f)
        } else {
            Color.White.copy(alpha = 0.055f)
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                if (option.selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (option.selected) AgentVoiceColors.SignalGold else AgentVoiceColors.MutedLavender
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(option.label, fontWeight = FontWeight.SemiBold)
                option.description?.let { description ->
                    Text(
                        description,
                        color = AgentVoiceColors.MutedLavender,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CommandTextStep(
    step: CommandStep,
    textDraft: String,
    pickerError: String?,
    onTextChange: (String) -> Unit,
    onSubmitText: () -> Unit,
    onSkipText: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = textDraft,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = step.placeholder?.let { placeholder -> { Text(placeholder) } },
            singleLine = true,
            isError = pickerError != null,
            shape = RoundedCornerShape(8.dp),
            colors = commandTextFieldColors()
        )
        pickerError?.let { Text(it, color = AgentVoiceColors.Sakura) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!step.required) {
                OutlinedButton(onClick = onSkipText, modifier = Modifier.weight(1f)) {
                    Text("Skip")
                }
            }
            Button(
                onClick = onSubmitText,
                enabled = !step.required || textDraft.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Continue")
            }
        }
        Spacer(Modifier.height(2.dp))
    }
}

@Composable
private fun commandTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = AgentVoiceColors.SoftWhite,
    unfocusedTextColor = AgentVoiceColors.SoftWhite,
    focusedBorderColor = AgentVoiceColors.NeonTeal,
    unfocusedBorderColor = AgentVoiceColors.MutedLavender,
    focusedPlaceholderColor = AgentVoiceColors.MutedLavender,
    unfocusedPlaceholderColor = AgentVoiceColors.MutedLavender,
    cursorColor = AgentVoiceColors.Sakura
)

private const val SEARCH_THRESHOLD = 8
