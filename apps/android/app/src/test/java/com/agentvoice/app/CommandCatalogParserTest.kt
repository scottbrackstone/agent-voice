package com.agentvoice.app

import com.agentvoice.app.commands.CommandCatalogParser
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandCatalogParserTest {
    @Test
    fun parsesSolaraCatalogFeaturesAndRendersSlashText() {
        val catalog = CommandCatalogParser.parse(
            """
            {
              "version": 1,
              "source": "Hermes Agent COMMAND_REGISTRY",
              "generated": "2026-07-03",
              "commands": [
                {
                  "name": "/stop",
                  "label": "Stop",
                  "description": "Kill all running background processes",
                  "template": "/stop",
                  "sendMode": "send_immediately",
                  "voiceSafe": true,
                  "aliases": [],
                  "allowTypedArgs": false
                },
                {
                  "name": "/new",
                  "label": "New Session",
                  "description": "Start a new session",
                  "template": "/new {name}",
                  "sendMode": "send_immediately",
                  "voiceSafe": true,
                  "aliases": ["/reset"],
                  "allowTypedArgs": true,
                  "steps": [
                    {
                      "id": "name",
                      "label": "Session name (optional)",
                      "type": "text",
                      "required": false,
                      "placeholder": "e.g. debugging-auth"
                    }
                  ]
                },
                {
                  "name": "/status",
                  "label": "Status",
                  "description": "Show status",
                  "template": "/status",
                  "sendMode": "fill_composer",
                  "voiceSafe": true,
                  "aliases": [],
                  "allowTypedArgs": false
                },
                {
                  "name": "/model",
                  "label": "Model",
                  "description": "Switch Solara model",
                  "template": "/model {provider} {model}",
                  "sendMode": "fill_composer",
                  "steps": [
                    {
                      "id": "provider",
                      "label": "Select a provider",
                      "type": "single_select",
                      "options": [
                        { "label": "DeepSeek (4)", "value": "--provider deepseek", "selected": true }
                      ]
                    },
                    {
                      "id": "model",
                      "label": "Select a model",
                      "type": "single_select",
                      "optionsBy": "provider",
                      "optionGroups": {
                        "--provider deepseek": [
                          { "label": "deepseek-v4-pro", "value": "deepseek-v4-pro", "selected": true },
                          { "label": "deepseek-v4-flash", "value": "deepseek-v4-flash" },
                          { "label": "deepseek-chat", "value": "deepseek-chat" },
                          { "label": "deepseek-reasoner", "value": "deepseek-reasoner" }
                        ]
                      }
                    }
                  ]
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals(1, catalog.version)
        assertEquals("Hermes Agent COMMAND_REGISTRY", catalog.source)
        assertEquals("2026-07-03", catalog.generated)
        assertEquals(listOf("/stop", "/new", "/status", "/model"), catalog.commands.map { it.name })

        val stop = catalog.commands.first { it.name == "/stop" }
        assertEquals("send_immediately", stop.sendMode)
        assertTrue(stop.voiceSafe)
        assertFalse(stop.allowTypedArgs)
        assertTrue(stop.steps.isEmpty())
        assertEquals("/stop", stop.render(emptyMap()))

        val newSession = catalog.commands.first { it.name == "/new" }
        assertEquals(listOf("/reset"), newSession.aliases)
        assertTrue(newSession.allowTypedArgs)
        assertEquals("text", newSession.steps.single().type)
        assertFalse(newSession.steps.single().required)
        assertEquals("e.g. debugging-auth", newSession.steps.single().placeholder)
        assertEquals("/new debugging-auth", newSession.render(mapOf("name" to "debugging-auth")))
        assertEquals("/new", newSession.render(emptyMap()))

        val status = catalog.commands.first { it.name == "/status" }
        assertEquals("/status", status.render(emptyMap()))

        val model = catalog.commands.first { it.name == "/model" }
        assertEquals(listOf("/model"), catalog.matchingCommands("/mo").map { it.name })
        assertEquals(listOf("/new"), catalog.matchingCommands("/reset").map { it.name })
        val providers = model.optionsForStep(0, emptyMap())
        assertEquals(listOf("--provider deepseek"), providers.map { it.value })
        assertTrue(providers.single().selected)

        val deepSeekModels = model.optionsForStep(1, mapOf("provider" to "--provider deepseek"))
        assertEquals(
            listOf("deepseek-v4-pro", "deepseek-v4-flash", "deepseek-chat", "deepseek-reasoner"),
            deepSeekModels.map { it.value }
        )
        assertTrue(deepSeekModels.first().selected)
        assertEquals(
            "/model --provider deepseek deepseek-v4-pro",
            model.render(mapOf("provider" to "--provider deepseek", "model" to "deepseek-v4-pro"))
        )
    }
}
