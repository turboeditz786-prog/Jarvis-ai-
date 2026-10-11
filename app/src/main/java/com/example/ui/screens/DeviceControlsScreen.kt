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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private data class AppLauncherItem(
  val name: String,
  val icon: ImageVector,
  val color: Color
)

@Composable
fun DeviceControlsScreen(
  viewModel: JarvisViewModel,
  uiState: JarvisUiState,
  modifier: Modifier = Modifier
) {
  var appSearchQuery by remember { mutableStateOf("") }
  val scrollState = rememberScrollState()

  val coreApps = listOf(
    AppLauncherItem("YouTube", Icons.Default.PlayArrow, Color(0xFFFF5252)),
    AppLauncherItem("WhatsApp", Icons.AutoMirrored.Filled.Chat, Color(0xFF25D366)),
    AppLauncherItem("Camera", Icons.Default.CameraAlt, ArcCyan),
    AppLauncherItem("Chrome", Icons.Default.Language, StarkGold),
    AppLauncherItem("Calculator", Icons.Default.Calculate, ArcBlue),
    AppLauncherItem("Clock", Icons.Default.Schedule, ArcCyan),
    AppLauncherItem("Maps", Icons.Default.Map, ArcGreen),
    AppLauncherItem("Spotify", Icons.Default.MusicNote, Color(0xFF1DB954)),
    AppLauncherItem("Gmail", Icons.Default.Email, Color(0xFFFF5252)),
    AppLauncherItem("Photos", Icons.Default.Photo, ArcBlue),
    AppLauncherItem("Settings", Icons.Default.Settings, TextSecondary),
    AppLauncherItem("Play Store", Icons.Default.ShoppingBag, ArcCyan)
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    // Screen Title
    Text(
      text = "SYSTEM HARDWARE // PHONE OVERRIDE",
      color = ArcCyan,
      fontSize = 14.sp,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.sp
    )
    Text(
      text = "Direct kernel and hardware control deck",
      color = TextSecondary,
      fontSize = 11.sp
    )

    Spacer(modifier = Modifier.height(14.dp))

    // 1. Hardware Toggles Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Torch Toggle
      HardwareControlTile(
        title = "Illumination",
        status = if (uiState.isTorchActive) "FLASHLIGHT: ON" else "FLASHLIGHT: OFF",
        icon = Icons.Default.FlashlightOn,
        isActive = uiState.isTorchActive,
        activeColor = StarkGold,
        modifier = Modifier.weight(1f)
      ) {
        viewModel.quickToggleTorch()
      }

      // Wi-Fi Shortcut
      HardwareControlTile(
        title = "Wi-Fi Array",
        status = if (uiState.systemStats.isWifiConnected) "CONNECTED" else "DISCONNECTED",
        icon = Icons.Default.Wifi,
        isActive = uiState.systemStats.isWifiConnected,
        activeColor = ArcCyan,
        modifier = Modifier.weight(1f)
      ) {
        viewModel.quickOpenSettings("wifi")
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Bluetooth
      HardwareControlTile(
        title = "Bluetooth",
        status = "DEVICE LINK",
        icon = Icons.Default.Bluetooth,
        isActive = false,
        activeColor = ArcBlue,
        modifier = Modifier.weight(1f)
      ) {
        viewModel.quickOpenSettings("bluetooth")
      }

      // Audio & Volume
      HardwareControlTile(
        title = "Audio Matrix",
        status = "VOLUME PANEL",
        icon = Icons.AutoMirrored.Filled.VolumeUp,
        isActive = false,
        activeColor = ArcGreen,
        modifier = Modifier.weight(1f)
      ) {
        viewModel.quickOpenSettings("sound")
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Display / Brightness
      HardwareControlTile(
        title = "Display HUD",
        status = "BRIGHTNESS",
        icon = Icons.Default.BrightnessHigh,
        isActive = false,
        activeColor = StarkGold,
        modifier = Modifier.weight(1f)
      ) {
        viewModel.quickOpenSettings("display")
      }

      // General Settings
      HardwareControlTile(
        title = "Master Settings",
        status = "SYSTEM PREFS",
        icon = Icons.Default.Settings,
        isActive = false,
        activeColor = TextSecondary,
        modifier = Modifier.weight(1f)
      ) {
        viewModel.quickOpenSettings("general")
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 2. Telemetry Diagnostics Card
    Text(
      text = "SYSTEM TELEMETRY",
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
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
      ) {
        // Battery
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.BatteryFull,
            contentDescription = null,
            tint = if (uiState.systemStats.isCharging) ArcGreen else ArcCyan,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "${uiState.systemStats.batteryPercentage}%",
            color = TextPrimary,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = if (uiState.systemStats.isCharging) "CHARGING" else "POWER CELL",
            color = TextSecondary,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
          )
        }

        // Storage
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.SdStorage,
            contentDescription = null,
            tint = ArcBlue,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "${uiState.systemStats.availableStorageGb} GB",
            color = TextPrimary,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "FREE STORAGE",
            color = TextSecondary,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
          )
        }

        // Network
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.Wifi,
            contentDescription = null,
            tint = if (uiState.systemStats.isWifiConnected) ArcGreen else TextSecondary,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = if (uiState.systemStats.isWifiConnected) "ONLINE" else "STANDBY",
            color = if (uiState.systemStats.isWifiConnected) ArcGreen else TextSecondary,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "NETWORK LINK",
            color = TextSecondary,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 3. Application Launcher Matrix
    Text(
      text = "APPLICATION LAUNCH ARRAY",
      color = TextSecondary,
      fontSize = 11.sp,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.sp
    )

    Spacer(modifier = Modifier.height(8.dp))

    // App search box
    OutlinedTextField(
      value = appSearchQuery,
      onValueChange = { appSearchQuery = it },
      placeholder = { Text("Search or launch custom app (e.g. Netflix, Telegram)", color = TextSecondary, fontSize = 12.sp) },
      leadingIcon = {
        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = ArcCyan)
      },
      trailingIcon = {
        if (appSearchQuery.isNotBlank()) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(ArcCyan)
              .clickable {
                viewModel.quickOpenApp(appSearchQuery)
                appSearchQuery = ""
              }
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text("LAUNCH", color = Color(0xFF070B14), fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }
        }
      },
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = ArcCyan,
        unfocusedBorderColor = HudBorder,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedContainerColor = HudSurfaceVariant,
        unfocusedContainerColor = HudSurfaceVariant
      ),
      shape = RoundedCornerShape(12.dp),
      singleLine = true,
      modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Grid of standard apps
    val displayedApps = if (appSearchQuery.isBlank()) {
      coreApps
    } else {
      coreApps.filter { it.name.contains(appSearchQuery, ignoreCase = true) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      for (row in displayedApps.chunked(3)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          for (app in row) {
            AppTile(
              app = app,
              modifier = Modifier.weight(1f)
            ) {
              viewModel.quickOpenApp(app.name)
            }
          }
          // Fill empty slots if row has less than 3
          for (i in 0 until (3 - row.size)) {
            Spacer(modifier = Modifier.weight(1f))
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(30.dp))
  }
}

@Composable
private fun HardwareControlTile(
  title: String,
  status: String,
  icon: ImageVector,
  isActive: Boolean,
  activeColor: Color,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .background(if (isActive) activeColor.copy(alpha = 0.12f) else ColorCardGlass)
      .border(1.dp, if (isActive) activeColor else HudBorder, RoundedCornerShape(14.dp))
      .clickable(onClick = onClick)
      .padding(12.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = if (isActive) activeColor else TextSecondary,
        modifier = Modifier.size(24.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Column {
        Text(
          text = title,
          color = TextPrimary,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = status,
          color = if (isActive) activeColor else TextSecondary,
          fontSize = 9.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.SemiBold
        )
      }
    }
  }
}

@Composable
private fun AppTile(
  app: AppLauncherItem,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(ColorCardGlass)
      .border(1.dp, HudBorder, RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .padding(vertical = 12.dp, horizontal = 6.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        imageVector = app.icon,
        contentDescription = app.name,
        tint = app.color,
        modifier = Modifier.size(26.dp)
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = app.name,
        color = TextPrimary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
      )
    }
  }
}
