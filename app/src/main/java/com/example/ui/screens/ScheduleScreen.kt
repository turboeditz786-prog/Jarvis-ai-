package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ScheduleCategory
import com.example.data.ScheduleItem
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScheduleScreen(
  viewModel: JarvisViewModel,
  modifier: Modifier = Modifier
) {
  val items by viewModel.scheduleItems.collectAsState()
  var selectedCategory by remember { mutableStateOf<ScheduleCategory?>(null) }
  var showAddDialog by remember { mutableStateOf(false) }

  // Add dialog fields
  var newTitle by remember { mutableStateOf("") }
  var newTime by remember { mutableStateOf("10:00 AM") }
  var newCategory by remember { mutableStateOf(ScheduleCategory.REMINDER) }
  var newDetails by remember { mutableStateOf("") }

  val filteredItems = items.filter { item ->
    selectedCategory == null || item.category == selectedCategory
  }

  val todayDateStr = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())

  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "JARVIS PROTOCOL // TIMELINE",
            color = ArcCyan,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
          Text(
            text = todayDateStr,
            color = TextSecondary,
            fontSize = 11.sp
          )
        }

        // Quick Alarm Button
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(HudSurfaceVariant)
            .border(1.dp, HudBorder, RoundedCornerShape(12.dp))
            .clickable {
              viewModel.executeCommand("Set alarm for 7:00 AM Morning alarm")
            }
            .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Alarm,
              contentDescription = "Set Alarm",
              tint = StarkGold,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "+ ALARM",
              color = StarkGold,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Category filter chips
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        CategoryFilterChip(
          title = "ALL (${items.size})",
          isSelected = selectedCategory == null,
          onClick = { selectedCategory = null }
        )
        ScheduleCategory.entries.forEach { cat ->
          val count = items.count { it.category == cat }
          CategoryFilterChip(
            title = "${cat.displayName.uppercase()} ($count)",
            isSelected = selectedCategory == cat,
            onClick = { selectedCategory = cat }
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Schedule Item List
      if (filteredItems.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.AlarmOn,
              contentDescription = null,
              tint = TextSecondary,
              modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "No scheduled items in this protocol.",
              color = TextSecondary,
              fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Say: \"Jarvis reminder lagao kal 10 baje\"",
              color = TextCyan,
              fontSize = 11.sp,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(filteredItems, key = { it.id }) { item ->
            ScheduleCard(
              item = item,
              onToggle = { viewModel.toggleScheduleItem(item.id) },
              onDelete = { viewModel.deleteScheduleItem(item.id) }
            )
          }
          item {
            Spacer(modifier = Modifier.height(80.dp))
          }
        }
      }
    }

    // Floating Action Button to add new schedule/reminder
    FloatingActionButton(
      onClick = { showAddDialog = true },
      containerColor = ArcCyan,
      contentColor = Color(0xFF070B14),
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(24.dp)
        .testTag("add_schedule_fab")
    ) {
      Icon(imageVector = Icons.Default.Add, contentDescription = "Add Schedule Task")
    }
  }

  // Dialog: Add Schedule Item
  if (showAddDialog) {
    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      containerColor = ColorCardGlass,
      title = {
        Text("LOG SCHEDULE PROTOCOL", color = ArcCyan, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
      },
      text = {
        Column {
          OutlinedTextField(
            value = newTitle,
            onValueChange = { newTitle = it },
            placeholder = { Text("Task or Reminder Title", color = TextSecondary) },
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = ArcCyan,
              unfocusedBorderColor = HudBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = newTime,
            onValueChange = { newTime = it },
            placeholder = { Text("Time (e.g. 03:30 PM)", color = TextSecondary) },
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = ArcCyan,
              unfocusedBorderColor = HudBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = newDetails,
            onValueChange = { newDetails = it },
            placeholder = { Text("Additional notes (optional)", color = TextSecondary) },
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = ArcCyan,
              unfocusedBorderColor = HudBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(10.dp))

          Text("Category:", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            ScheduleCategory.entries.forEach { cat ->
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (newCategory == cat) ArcCyan else HudSurfaceVariant)
                  .clickable { newCategory = cat }
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Text(
                  text = cat.displayName,
                  color = if (newCategory == cat) Color(0xFF070B14) else TextPrimary,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newTitle.isNotBlank()) {
              val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
              viewModel.addSchedule(newTitle, newTime, today, newCategory)
              showAddDialog = false
              newTitle = ""
              newDetails = ""
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = ArcCyan)
        ) {
          Text("Commit Task", color = Color(0xFF070B14), fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }
}

@Composable
private fun CategoryFilterChip(
  title: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .background(if (isSelected) ArcCyan else HudSurfaceVariant)
      .border(1.dp, if (isSelected) ArcCyan else HudBorder, RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 6.dp)
  ) {
    Text(
      text = title,
      color = if (isSelected) Color(0xFF070B14) else TextSecondary,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace
    )
  }
}

@Composable
private fun ScheduleCard(
  item: ScheduleItem,
  onToggle: () -> Unit,
  onDelete: () -> Unit
) {
  val icon: ImageVector = when (item.category) {
    ScheduleCategory.WORK -> Icons.Default.Work
    ScheduleCategory.CALL -> Icons.Default.Call
    ScheduleCategory.REMINDER -> Icons.Default.Alarm
    ScheduleCategory.PERSONAL -> Icons.Default.Person
    ScheduleCategory.SYSTEM -> Icons.Default.Settings
  }

  val categoryColor = when (item.category) {
    ScheduleCategory.WORK -> ArcBlue
    ScheduleCategory.CALL -> ArcGreen
    ScheduleCategory.REMINDER -> StarkGold
    ScheduleCategory.PERSONAL -> ArcCyan
    ScheduleCategory.SYSTEM -> TextSecondary
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(ColorCardGlass)
      .border(
        1.dp,
        if (item.isCompleted) HudBorder.copy(alpha = 0.3f) else HudBorder,
        RoundedCornerShape(14.dp)
      )
      .padding(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Toggle Checkbox
      IconButton(
        onClick = onToggle,
        modifier = Modifier.size(28.dp)
      ) {
        Icon(
          imageVector = if (item.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
          contentDescription = "Toggle Complete",
          tint = if (item.isCompleted) ArcGreen else TextSecondary,
          modifier = Modifier.size(22.dp)
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      // Content
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Category pill
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(categoryColor.copy(alpha = 0.15f))
              .border(1.dp, categoryColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = icon,
                contentDescription = null,
                tint = categoryColor,
                modifier = Modifier.size(10.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = item.category.displayName.uppercase(),
                color = categoryColor,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
              )
            }
          }

          // Time pill
          Text(
            text = "• ${item.time}",
            color = TextCyan,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = item.title,
          color = if (item.isCompleted) TextSecondary else TextPrimary,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
        )

        if (item.details.isNotBlank()) {
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = item.details,
            color = TextSecondary,
            fontSize = 11.sp,
            lineHeight = 15.sp
          )
        }
      }

      // Delete Button
      IconButton(
        onClick = onDelete,
        modifier = Modifier.size(28.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Delete,
          contentDescription = "Delete Item",
          tint = TextSecondary.copy(alpha = 0.6f),
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}
