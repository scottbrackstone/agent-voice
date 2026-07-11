package com.agentvoice.app.model

enum class ChatBackground(val id: String, val label: String) {
    SakuraSignal("sakura_signal", "Sakura signal"),
    NeonBloom("neon_bloom", "Neon bloom"),
    ElectricDawn("electric_dawn", "Electric dawn"),
    MidnightArcade("midnight_arcade", "Midnight arcade");

    companion object {
        const val DEFAULT_ID = "sakura_signal"

        fun fromId(id: String): ChatBackground =
            entries.firstOrNull { it.id == id } ?: SakuraSignal
    }
}

object AvatarModels {
    const val SOLARA_STATIC = "solara_static"
}
