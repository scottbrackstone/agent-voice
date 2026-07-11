package com.agentvoice.app.ui.presentation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agentvoice.app.R
import com.agentvoice.app.model.AgentPresentation

interface CompanionRenderer {
    val modelId: String

    @Composable
    fun Render(
        presentation: AgentPresentation,
        active: Boolean,
        size: Dp,
        modifier: Modifier
    )
}

object SolaraStaticRenderer : CompanionRenderer {
    override val modelId: String = "solara_static"

    @Composable
    override fun Render(
        presentation: AgentPresentation,
        active: Boolean,
        size: Dp,
        modifier: Modifier
    ) {
        val transition = rememberInfiniteTransition(label = "solaraPresence")
        val scale by transition.animateFloat(
            initialValue = if (active) 0.97f else 0.995f,
            targetValue = if (active) 1.045f else 1.005f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "solaraPresenceScale"
        )

        val moodColor = when (presentation.mood.lowercase()) {
            "happy", "excited", "playful" -> Color(0xFFFFD166)
            "focused", "thinking" -> Color(0xFF4DE3D2)
            "concerned", "sad" -> Color(0xFF7FA8FF)
            "alert", "urgent" -> Color(0xFFFF5F88)
            else -> Color(0xFFFF79B0)
        }

        Box(
            modifier = modifier
                .size(size)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .clip(CircleShape)
                .background(Color(0xFF7EE6DB).copy(alpha = 0.28f))
                .border(3.dp, moodColor.copy(alpha = 0.88f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.agent_companion),
                contentDescription = "Solara",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
    }
}
