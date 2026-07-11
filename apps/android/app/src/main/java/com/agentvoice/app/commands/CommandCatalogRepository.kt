package com.agentvoice.app.commands

import android.content.Context

class CommandCatalogRepository(private val context: Context) {
    fun load(): CommandCatalog {
        return runCatching {
            context.assets.open(COMMAND_CATALOG_ASSET).bufferedReader().use { reader ->
                parse(reader.readText())
            }
        }.getOrDefault(CommandCatalog.Empty)
    }

    fun parse(json: String): CommandCatalog = CommandCatalogParser.parse(json)

    companion object {
        private const val COMMAND_CATALOG_ASSET = "hermes_commands.json"
    }
}
