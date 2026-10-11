package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ArcBlue
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ColorCardGlass
import com.example.ui.theme.ColorCardHighlight
import com.example.ui.theme.HudBorder
import com.example.ui.theme.HudSurfaceVariant
import com.example.ui.theme.StarkGold
import com.example.ui.theme.TextCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.voice.AssistantListeningState

@Composable
fun JarvisConsoleTerminal(
  speech: String,
  userTranscript: String,
  lastAction: String?,
  listeningState: AssistantListeningState,
  logs: List<String>,
  modifier: Modifier = Modifier,
  onStopSpeech: () -> Unit
) {
  var showLogs by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(ColorCardGlass)
      .border(1.dp, HudBorder, RoundedCornerShape(16.dp))
      .padding(14.dp)
  ) {
    // Header row with terminal title and logs toggle
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.RecordVoiceOver,
          contentDescription = null,
          tint = ArcCyan,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "CORE TERMINAL // AUDIO FEED",
          color = ArcCyan,
          fontSize = 11.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        if (listeningState == AssistantListeningState.SPEAKING) {
          IconButton(
            onClick = onStopSpeech,
            modifier = Modifier.size(24.dp)
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.VolumeUp,
              contentDescription = "Stop Speech",
              tint = StarkGold,
              modifier = Modifier.size(16.dp)
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (showLogs) ArcCyan.copy(alpha = 0.2f) else HudSurfaceVariant)
            .border(1.dp, if (showLogs) ArcCyan else HudBorder, RoundedCornerShape(6.dp))
            .clickable { showLogs = !showLogs }
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Code,
              contentDescription = "Logs",
              tint = if (showLogs) ArcCyan else TextSecondary,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (showLogs) "HIDE LOGS" else "RAW LOGS",
              color = if (showLogs) ArcCyan else TextSecondary,
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // User Transcript bubble if user spoke or typed
    if (userTranscript.isNotBlank()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(ColorCardHighlight)
          .padding(8.dp)
      ) {
        Column {
          Text(
            text = "USER PROMPT:",
            color = TextSecondary,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "\"$userTranscript\"",
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
    }

    // Jarvis Response Text
    Text(
      text = speech,
      color = TextCyan,
      fontSize = 14.sp,
      lineHeight = 20.sp,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.testTag("jarvis_speech_text")
    )

    // Last executed action tag
    if (!lastAction.isNullOrBlank()) {
      Spacer(modifier = Modifier.height(8.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(6.dp))
          .background(ArcBlue.copy(alpha = 0.15f))
          .border(1.dp, ArcBlue.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
          .padding(horizontal = 8.dp, vertical = 5.dp)
      ) {
        Text(
          text = "▶ $lastAction",
          color = TextPrimary,
          fontSize = 11.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Medium
        )
      }
    }

    // Expandable logs
    AnimatedVisibility(visible = showLogs) {
      Column(modifier = Modifier.padding(top = 10.dp)) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(HudSurfaceVariant)
            .border(1.dp, HudBorder, RoundedCornerShape(8.dp))
            .padding(8.dp)
        ) {
          LazyColumn {
            items(logs.reversed()) { log ->
              Text(
                text = log,
                color = if (log.contains("RESULT")) ArcCyan else TextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 14.sp
              )
            }
          }
        }
      }
    }
  }
}
