package com.agentvoice.app.ui.presentation

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agentvoice.app.R

interface CompanionRenderer {
    val modelId: String

    @Composable
    fun Render(
        state: CompanionVisualState,
        size: Dp,
        modifier: Modifier
    )
}

object SolaraSpriteRenderer : CompanionRenderer {
    override val modelId: String = "solara_sprite_v1"

    @Composable
    override fun Render(
        state: CompanionVisualState,
        size: Dp,
        modifier: Modifier
    ) {
        val transition = rememberInfiniteTransition(label = "solaraMotion")
        val breath by transition.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = motionDuration(state.phase), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "solaraBreath"
        )
        val frameCycle by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = if (state.phase == CompanionPhase.Speaking) 360 else 3_600,
                    easing = LinearEasing
                )
            ),
            label = "solaraFrame"
        )
        val density = LocalDensity.current
        val translationY = with(density) { motionOffset(state.phase).toPx() } * breath
        val scale = when (state.phase) {
            CompanionPhase.Happy -> 1f + (breath * 0.028f)
            CompanionPhase.Listening -> 1.025f + (breath * 0.012f)
            else -> 1f + (breath * 0.009f)
        }
        val rotation = breath * when (state.phase) {
            CompanionPhase.Listening -> 1.8f
            CompanionPhase.Thinking -> -1.4f
            CompanionPhase.Speaking -> 0.8f
            CompanionPhase.Happy -> 1.5f
            CompanionPhase.Concerned -> 0.55f
            CompanionPhase.Idle -> 0.35f
        }
        val moodColor = moodColor(state)
        val drawable = drawableFor(state.phase, frameCycle)

        Box(
            modifier = modifier
                .size(size)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationY = translationY,
                    rotationZ = rotation
                )
                .clip(CircleShape)
                .background(moodColor.copy(alpha = 0.24f + (state.intensity * 0.18f)))
                .border(3.dp, moodColor.copy(alpha = 0.92f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = drawable,
                animationSpec = tween(140),
                label = "solaraExpression"
            ) { frame ->
                Image(
                    painter = painterResource(frame),
                    contentDescription = "Solara ${state.phase.name.lowercase()}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }
        }
    }

    @DrawableRes
    private fun drawableFor(phase: CompanionPhase, cycle: Float): Int = when (phase) {
        CompanionPhase.Listening -> R.drawable.solara_listen
        CompanionPhase.Thinking -> R.drawable.solara_think
        CompanionPhase.Speaking -> if (cycle < 0.5f) {
            R.drawable.solara_speak_a
        } else {
            R.drawable.solara_speak_b
        }
        CompanionPhase.Happy -> R.drawable.solara_happy
        CompanionPhase.Concerned -> R.drawable.solara_concerned
        CompanionPhase.Idle -> if (cycle > 0.955f) R.drawable.solara_blink else R.drawable.solara_idle
    }

    private fun moodColor(state: CompanionVisualState): Color = when {
        state.phase == CompanionPhase.Concerned -> Color(0xFF86A8FF)
        state.phase == CompanionPhase.Thinking -> Color(0xFF48E0D0)
        state.phase == CompanionPhase.Listening -> Color(0xFFFF79B0)
        state.phase == CompanionPhase.Happy -> Color(0xFFFFD166)
        state.mood in setOf("happy", "excited", "playful") -> Color(0xFFFFD166)
        else -> Color(0xFFFF79B0)
    }

    private fun motionDuration(phase: CompanionPhase): Int = when (phase) {
        CompanionPhase.Speaking -> 420
        CompanionPhase.Happy -> 720
        CompanionPhase.Listening -> 900
        CompanionPhase.Thinking -> 1_450
        CompanionPhase.Concerned -> 1_800
        CompanionPhase.Idle -> 2_400
    }

    private fun motionOffset(phase: CompanionPhase): Dp = when (phase) {
        CompanionPhase.Happy -> 3.dp
        CompanionPhase.Speaking -> 1.5.dp
        CompanionPhase.Thinking -> 2.dp
        CompanionPhase.Listening -> 1.dp
        CompanionPhase.Concerned -> 1.dp
        CompanionPhase.Idle -> 1.5.dp
    }
}
