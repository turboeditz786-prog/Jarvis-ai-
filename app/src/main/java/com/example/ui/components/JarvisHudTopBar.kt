package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.system.SystemStats
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ArcGreen
import com.example.ui.theme.HudBorder
import com.example.ui.theme.HudSurfaceVariant
import com.example.ui.theme.StarkGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.voice.AssistantListeningState

@Composable
fun JarvisHudTopBar(
  stats: SystemStats,
  listeningState: AssistantListeningState,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Left: Holographic Branding
    Column {
      Row(verticalAlignment = Alignment.CenterVertically) {
        // Glowing status beacon dot
        val beaconColor = when (listeningState) {
          AssistantListeningState.LISTENING -> ArcCyan
          AssistantListeningState.PROCESSING -> StarkGold
          AssistantListeningState.SPEAKING -> ArcCyan
          else -> ArcGreen
        }
        Box(
          modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(beaconColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "J.A.R.V.I.S.",
          color = ArcCyan,
          fontSize = 18.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 2.sp
        )
      }
      Text(
        text = "MARK 85 // PHONE LINK ACTIVE",
        color = TextSecondary,
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.sp
      )
    }

    // Right: Device Telemetry Pills
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      // Torch pill if active
      AnimatedVisibility(visible = stats.isTorchOn, enter = fadeIn(), exit = fadeOut()) {
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(StarkGold.copy(alpha = 0.2f))
            .border(1.dp, StarkGold, RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.FlashlightOn,
            contentDescription = "Torch Active",
            tint = StarkGold,
            modifier = Modifier.size(12.dp)
          )
        }
      }

      // Wi-Fi Status
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .background(HudSurfaceVariant)
          .border(1.dp, HudBorder, RoundedCornerShape(12.dp))
          .padding(horizontal = 6.dp, vertical = 3.dp)
      ) {
        Icon(
          imageVector = if (stats.isWifiConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
          contentDescription = "Network",
          tint = if (stats.isWifiConnected) ArcCyan else TextSecondary,
          modifier = Modifier.size(13.dp)
        )
      }

      // Battery Pill
      Row(
        modifier = Modifier
          .testTag("battery_status_pill")
          .clip(RoundedCornerShape(12.dp))
          .background(HudSurfaceVariant)
          .border(1.dp, HudBorder, RoundedCornerShape(12.dp))
          .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = if (stats.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
          contentDescription = "Battery",
          tint = if (stats.isCharging) ArcGreen else ArcCyan,
          modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "${stats.batteryPercentage}%",
          color = TextPrimary,
          fontSize = 11.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}
