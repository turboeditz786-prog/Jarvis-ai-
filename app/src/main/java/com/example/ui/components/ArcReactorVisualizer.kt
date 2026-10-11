package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ArcBlue
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ArcCyanGlow
import com.example.ui.theme.StarkGold
import com.example.voice.AssistantListeningState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorVisualizer(
  listeningState: AssistantListeningState,
  audioRms: Float,
  modifier: Modifier = Modifier,
  onReactorClick: () -> Unit
) {
  val infiniteTransition = rememberInfiniteTransition(label = "arc_reactor")

  // Outer rotation
  val outerRotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 14000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "outer_rot"
  )

  // Inner counter-rotation
  val innerRotation by infiniteTransition.animateFloat(
    initialValue = 360f,
    targetValue = 0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 9000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "inner_rot"
  )

  // Idle breath pulse
  val idlePulse by infiniteTransition.animateFloat(
    initialValue = 0.92f,
    targetValue = 1.08f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "idle_pulse"
  )

  // Dynamic scale calculation based on state and microphone RMS audio level
  val dynamicScale = when (listeningState) {
    AssistantListeningState.LISTENING -> (1f + (audioRms * 0.35f)).coerceIn(1f, 1.4f)
    AssistantListeningState.PROCESSING -> 1.15f
    AssistantListeningState.SPEAKING -> 1.1f + (audioRms * 0.2f)
    else -> idlePulse
  }

  val coreColor = when (listeningState) {
    AssistantListeningState.LISTENING -> ArcCyanGlow
    AssistantListeningState.PROCESSING -> StarkGold
    AssistantListeningState.SPEAKING -> ArcBlue
    else -> ArcCyan
  }

  Box(
    modifier = modifier
      .size(230.dp)
      .testTag("arc_reactor_button")
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(bounded = false, radius = 120.dp),
        onClick = onReactorClick
      ),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val radius = size.minDimension / 2f

      // 1. Outermost glow ring
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            coreColor.copy(alpha = 0.25f),
            coreColor.copy(alpha = 0.05f),
            Color.Transparent
          ),
          center = center,
          radius = radius * dynamicScale
        ),
        radius = radius * dynamicScale,
        center = center
      )

      // 2. Outer segmented tech ring
      rotate(outerRotation, pivot = center) {
        val outerRadius = radius * 0.88f
        // Draw 6 segmented arcs
        for (i in 0 until 6) {
          val startAngle = i * 60f + 5f
          drawArc(
            color = ArcCyan.copy(alpha = 0.6f),
            startAngle = startAngle,
            sweepAngle = 48f,
            useCenter = false,
            topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
            size = Size(outerRadius * 2f, outerRadius * 2f),
            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
          )
        }

        // Tick marks around outer ring
        for (angle in 0 until 360 step 15) {
          val rad = Math.toRadians(angle.toDouble())
          val innerX = center.x + (radius * 0.94f) * cos(rad).toFloat()
          val innerY = center.y + (radius * 0.94f) * sin(rad).toFloat()
          val outerX = center.x + (radius * 0.99f) * cos(rad).toFloat()
          val outerY = center.y + (radius * 0.99f) * sin(rad).toFloat()
          drawLine(
            color = ArcCyan.copy(alpha = 0.4f),
            start = Offset(innerX, innerY),
            end = Offset(outerX, outerY),
            strokeWidth = 1.5.dp.toPx()
          )
        }
      }

      // 3. Middle counter-rotating power track
      rotate(innerRotation, pivot = center) {
        val midRadius = radius * 0.66f
        for (j in 0 until 4) {
          val startAngle = j * 90f + 10f
          drawArc(
            color = coreColor.copy(alpha = 0.85f),
            startAngle = startAngle,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = Offset(center.x - midRadius, center.y - midRadius),
            size = Size(midRadius * 2f, midRadius * 2f),
            style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
          )
        }

        // Inner triangular power struts (Stark Mark 85 Arc Reactor style)
        for (t in 0 until 3) {
          val rad = Math.toRadians((t * 120.0))
          val strutEndX = center.x + (midRadius * 0.95f) * cos(rad).toFloat()
          val strutEndY = center.y + (midRadius * 0.95f) * sin(rad).toFloat()
          drawLine(
            color = coreColor.copy(alpha = 0.75f),
            start = center,
            end = Offset(strutEndX, strutEndY),
            strokeWidth = 2.5.dp.toPx()
          )
        }
      }

      // 4. Central glowing core
      val coreRadius = radius * 0.38f * dynamicScale
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            Color.White,
            coreColor,
            coreColor.copy(alpha = 0.3f),
            Color.Transparent
          ),
          center = center,
          radius = coreRadius
        ),
        radius = coreRadius,
        center = center
      )

      // Inner sharp boundary ring
      drawCircle(
        color = Color.White.copy(alpha = 0.9f),
        radius = coreRadius * 0.55f,
        center = center,
        style = Stroke(width = 2.dp.toPx())
      )
    }

    // Text in the very center of the Arc Reactor
    Text(
      text = when (listeningState) {
        AssistantListeningState.LISTENING -> "LIVE"
        AssistantListeningState.PROCESSING -> "COMP"
        AssistantListeningState.SPEAKING -> "VOX"
        else -> "J.A.R.V.I.S."
      },
      color = Color.White,
      fontSize = 11.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.Monospace,
      letterSpacing = 1.5.sp
    )
  }
}
