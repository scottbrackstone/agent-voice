package com.agentvoice.app.commands

import org.json.JSONObject

data class CommandCatalog(
    val version: Int,
    val source: String?,
    val generated: String?,
    val commands: List<HermesCommand>
) {
    fun matchingCommands(query: String): List<HermesCommand> {
        val normalized = query.trim().lowercase()
        if (!normalized.startsWith("/")) {
            return emptyList()
        }

        return commands.filter { command ->
            command.name.lowercase().startsWith(normalized) ||
                command.label.lowercase().contains(normalized.removePrefix("/")) ||
                command.aliases.any { alias -> alias.lowercase().startsWith(normalized) }
        }
    }

    companion object {
        val Empty = CommandCatalog(
            version = 1,
            source = null,
            generated = null,
            commands = emptyList()
        )
    }
}

data class HermesCommand(
    val name: String,
    val label: String,
    val description: String,
    val template: String,
    val sendMode: String,
    val voiceSafe: Boolean,
    val aliases: List<String>,
    val allowTypedArgs: Boolean,
    val steps: List<CommandStep>
) {
    fun optionsForStep(index: Int, selections: Map<String, String>): List<CommandOption> {
        val step = steps.getOrNull(index) ?: return emptyList()
        val parentStepId = step.optionsBy
        if (parentStepId.isNullOrBlank()) {
            return step.options
        }

        val parentValue = selections[parentStepId] ?: return emptyList()
        return step.optionGroups[parentValue].orEmpty()
    }

    fun render(selections: Map<String, String>): String {
        val rendered = selections.entries.fold(template) { current, selection ->
            current.replace("{${selection.key}}", selection.value.trim())
        }

        return rendered
            .replace(Regex("\\{[^}]+\\}"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

data class CommandStep(
    val id: String,
    val label: String,
    val type: String,
    val required: Boolean,
    val placeholder: String?,
    val options: List<CommandOption>,
    val optionsBy: String?,
    val optionGroups: Map<String, List<CommandOption>>
)

data class CommandOption(
    val label: String,
    val value: String,
    val description: String?,
    val selected: Boolean
)

object CommandCatalogParser {
    fun parse(json: String): CommandCatalog {
        val root = JSONObject(json)
        val commandArray = root.optJSONArray("commands")
        val commands = (0 until (commandArray?.length() ?: 0)).map { index ->
            parseCommand(commandArray!!.getJSONObject(index))
        }

        return CommandCatalog(
            version = root.optInt("version", 1),
            source = root.optString("source").ifBlank { null },
            generated = root.optString("generated").ifBlank { null },
            commands = commands
        )
    }

    private fun parseCommand(json: JSONObject): HermesCommand {
        val stepArray = json.optJSONArray("steps")
        val steps = (0 until (stepArray?.length() ?: 0)).map { index ->
            parseStep(stepArray!!.getJSONObject(index))
        }

        return HermesCommand(
            name = json.getString("name"),
            label = json.optString("label", json.getString("name")),
            description = json.optString("description", ""),
            template = json.optString("template", json.getString("name")),
            sendMode = json.optString("sendMode", "fill_composer"),
            voiceSafe = json.optBoolean("voiceSafe", false),
            aliases = parseStringArray(json.optJSONArray("aliases")),
            allowTypedArgs = json.optBoolean("allowTypedArgs", false),
            steps = steps
        )
    }

    private fun parseStep(json: JSONObject): CommandStep {
        return CommandStep(
            id = json.getString("id"),
            label = json.optString("label", json.getString("id")),
            type = json.optString("type", "single_select"),
            required = json.optBoolean("required", true),
            placeholder = json.optString("placeholder").ifBlank { null },
            options = parseOptions(json.optJSONArray("options")),
            optionsBy = json.optString("optionsBy").ifBlank { null },
            optionGroups = parseOptionGroups(json.optJSONObject("optionGroups"))
        )
    }

    private fun parseStringArray(array: org.json.JSONArray?): List<String> {
        if (array == null) {
            return emptyList()
        }

        return (0 until array.length()).map { index -> array.getString(index) }
    }

    private fun parseOptions(array: org.json.JSONArray?): List<CommandOption> {
        if (array == null) {
            return emptyList()
        }

        return (0 until array.length()).map { index ->
            parseOption(array.getJSONObject(index))
        }
    }

    private fun parseOptionGroups(json: JSONObject?): Map<String, List<CommandOption>> {
        if (json == null) {
            return emptyMap()
        }

        return json.keys().asSequence().associateWith { key ->
            parseOptions(json.getJSONArray(key))
        }
    }

    private fun parseOption(json: JSONObject): CommandOption =
        CommandOption(
            label = json.optString("label", json.getString("value")),
            value = json.getString("value"),
            description = json.optString("description").ifBlank { null },
            selected = json.optBoolean("selected", false)
        )
}
