# Solara Command Catalog Prompt

Give Solara this prompt to produce the command catalog for `apps/android/app/src/main/assets/hermes_commands.json`.

```text
I am replacing Telegram with my Android app AgentVoice as the main client for talking to you.

I need a complete machine-readable slash command catalog so the app can reproduce Telegram-style command menus.

Please return JSON only, no markdown, using this schema:

{
  "version": 1,
  "commands": [
    {
      "name": "/model",
      "label": "Model",
      "description": "Short human description",
      "template": "/model {api} {model}",
      "sendMode": "fill_composer",
      "steps": [
        {
          "id": "api",
          "label": "API connection",
          "type": "single_select",
          "options": [
            {
              "label": "OpenAI",
              "value": "openai",
              "description": "Optional short description"
            }
          ]
        },
        {
          "id": "model",
          "label": "Model",
          "type": "single_select",
          "optionsBy": "api",
          "optionGroups": {
            "openai": [
              {
                "label": "GPT-5",
                "value": "gpt-5",
                "description": "Optional short description"
              }
            ]
          }
        }
      ]
    }
  ]
}

Please include every slash command I currently use in Telegram.

For each command include:
1. The exact slash command name.
2. The final text format that Telegram sends.
3. Every menu step Telegram shows.
4. Every option for each step.
5. Which options depend on earlier choices.
6. Examples of final command text.
7. Whether the command should fill the composer or send immediately. Default to fill_composer.
8. Any aliases or common variants.
9. Whether typed arguments are allowed.
10. Whether the command is safe to use from mobile voice.

Important:
- Do not invent commands.
- Preserve exact command spelling and option values.
- Return JSON only.
```
