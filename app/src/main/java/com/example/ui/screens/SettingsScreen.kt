package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.JarvisUiState
import com.example.ui.JarvisViewModel
import com.example.ui.theme.ArcBlue
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ArcGreen
import com.example.ui.theme.ColorCardGlass
import com.example.ui.theme.HudBorder
import com.example.ui.theme.HudSurfaceVariant
import com.example.ui.theme.StarkGold
import com.example.ui.theme.TextCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
  viewModel: JarvisViewModel,
  uiState: JarvisUiState,
  modifier: Modifier = Modifier
) {
  var speedSlider by remember { mutableFloatStateOf(uiState.voiceSpeed) }
  var pitchSlider by remember { mutableFloatStateOf(uiState.voicePitch) }
  val scrollState = rememberScrollState()

  val isGeminiConfigured = try {
    BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
  } catch (e: Exception) {
    false
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    // Title
    Text(
      text = "SYSTEM PROTOCOLS // CONFIGURATION",
      color = ArcCyan,
      fontSize = 14.sp,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.sp
    )
    Text(
      text = "Jarvis synthetic voice and neural matrix parameters",
      color = TextSecondary,
      fontSize = 11.sp
    )

    Spacer(modifier = Modifier.height(16.dp))

    // 1. Gemini Cognition Core Status Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(ColorCardGlass)
        .border(1.dp, if (isGeminiConfigured) ArcGreen else StarkGold, RoundedCornerShape(14.dp))
        .padding(14.dp)
    ) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = null,
              tint = if (isGeminiConfigured) ArcGreen else StarkGold,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "GEMINI 3.5 FLASH NEURAL CORE",
              color = TextPrimary,
              fontSize = 12.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(if (isGeminiConfigured) ArcGreen.copy(alpha = 0.2f) else StarkGold.copy(alpha = 0.2f))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = if (isGeminiConfigured) "ACTIVE" else "HYBRID MODE",
              color = if (isGeminiConfigured) ArcGreen else StarkGold,
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = if (isGeminiConfigured) {
            "Cloud intelligence linked. High-level reasoning, Hinglish contextual actions, and conversational cognition active."
          } else {
            "Jarvis is running with high-speed on-device NLP intent parser for instant phone calls, apps, messages, and settings. Add your GEMINI_API_KEY in AI Studio Secrets panel for advanced AI thinking."
          },
          color = TextSecondary,
          fontSize = 11.sp,
          lineHeight = 16.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 2. Voice Modulation Controls
    Text(
      text = "VOICE MODULATION",
      color = TextSecondary,
      fontSize = 11.sp,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.sp
    )

    Spacer(modifier = Modifier.height(8.dp))

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(ColorCardGlass)
        .border(1.dp, HudBorder, RoundedCornerShape(14.dp))
        .padding(14.dp)
    ) {
      Column {
        // Speed slider
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Transmission Rate (Speed):", color = TextPrimary, fontSize = 12.sp)
          Text("${String.format("%.2f", speedSlider)}x", color = ArcCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        Slider(
          value = speedSlider,
          onValueChange = {
            speedSlider = it
            viewModel.updateVoiceSettings(speedSlider, pitchSlider)
          },
          valueRange = 0.7f..1.5f,
          colors = SliderDefaults.colors(
            thumbColor = ArcCyan,
            activeTrackColor = ArcCyan,
            inactiveTrackColor = HudBorder
          )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Pitch slider
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Synthesizer Frequency (Pitch):", color = TextPrimary, fontSize = 12.sp)
          Text("${String.format("%.2f", pitchSlider)}x", color = ArcCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        Slider(
          value = pitchSlider,
          onValueChange = {
            pitchSlider = it
            viewModel.updateVoiceSettings(speedSlider, pitchSlider)
          },
          valueRange = 0.7f..1.4f,
          colors = SliderDefaults.colors(
            thumbColor = ArcCyan,
            activeTrackColor = ArcCyan,
            inactiveTrackColor = HudBorder
          )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Test voice button
        Button(
          onClick = {
            viewModel.executeCommand("Test speech synthesis")
          },
          colors = ButtonDefaults.buttonColors(containerColor = HudSurfaceVariant),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = ArcCyan, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Test Jarvis Audio Response", color = ArcCyan, fontSize = 12.sp)
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 3. Jarvis Persona Matrix
    Text(
      text = "PERSONA PROFILE",
      color = TextSecondary,
      fontSize = 11.sp,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.sp
    )

    Spacer(modifier = Modifier.height(8.dp))

    val personas = listOf(
      "Tony Stark AI (Boss)" to "Charismatic, loyal, addresses you as Boss",
      "Tactical Commander (Sir)" to "Crisp military efficiency and system telemetry",
      "Hinglish Assistant" to "Friendly native Indian conversational tone"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      personas.forEach { (name, desc) ->
        val isSelected = uiState.personaName == name
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) ArcCyan.copy(alpha = 0.12f) else ColorCardGlass)
            .border(1.dp, if (isSelected) ArcCyan else HudBorder, RoundedCornerShape(12.dp))
            .clickable { viewModel.setPersona(name) }
            .padding(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = name,
                color = if (isSelected) ArcCyan else TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = desc,
                color = TextSecondary,
                fontSize = 11.sp
              )
            }
            if (isSelected) {
              Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = ArcCyan,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}
