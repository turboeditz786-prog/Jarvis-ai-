package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.JarvisHudTopBar
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.DeviceControlsScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.HudBackground
import com.example.ui.theme.HudBorder
import com.example.ui.theme.HudSurface
import com.example.ui.theme.TextSecondary

private data class NavItem(
  val tab: JarvisTab,
  val label: String,
  val icon: ImageVector,
  val testTag: String
)

@Composable
fun JarvisApp(
  viewModel: JarvisViewModel = viewModel()
) {
  val uiState by viewModel.uiState.collectAsState()

  // Handle back button on secondary screens
  BackHandler(enabled = uiState.currentTab != JarvisTab.ASSISTANT) {
    viewModel.selectTab(JarvisTab.ASSISTANT)
  }

  val navItems = listOf(
    NavItem(JarvisTab.ASSISTANT, "CORE", Icons.Default.GraphicEq, "nav_core"),
    NavItem(JarvisTab.SCHEDULE, "SCHEDULE", Icons.Default.CalendarMonth, "nav_schedule"),
    NavItem(JarvisTab.DEVICE_CONTROL, "CONTROLS", Icons.Default.Apps, "nav_controls"),
    NavItem(JarvisTab.SETTINGS, "PROTOCOLS", Icons.Default.Tune, "nav_settings")
  )

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .background(HudBackground),
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      JarvisHudTopBar(
        stats = uiState.systemStats,
        listeningState = uiState.listeningState
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = HudSurface,
        tonalElevation = 8.dp,
        modifier = Modifier
          .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
          .border(1.dp, HudBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
      ) {
        navItems.forEach { item ->
          val isSelected = uiState.currentTab == item.tab
          NavigationBarItem(
            selected = isSelected,
            onClick = { viewModel.selectTab(item.tab) },
            icon = {
              Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                modifier = Modifier.size(20.dp)
              )
            },
            label = {
              Text(
                text = item.label,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
              )
            },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = Color(0xFF070B14),
              selectedTextColor = ArcCyan,
              indicatorColor = ArcCyan,
              unselectedIconColor = TextSecondary,
              unselectedTextColor = TextSecondary
            ),
            modifier = Modifier.testTag(item.testTag)
          )
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(HudBackground)
    ) {
      when (uiState.currentTab) {
        JarvisTab.ASSISTANT -> AssistantScreen(viewModel = viewModel, uiState = uiState)
        JarvisTab.SCHEDULE -> ScheduleScreen(viewModel = viewModel)
        JarvisTab.DEVICE_CONTROL -> DeviceControlsScreen(viewModel = viewModel, uiState = uiState)
        JarvisTab.SETTINGS -> SettingsScreen(viewModel = viewModel, uiState = uiState)
      }
    }
  }
}
