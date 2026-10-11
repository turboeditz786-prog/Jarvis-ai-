package com.example.data

import java.util.UUID

enum class ScheduleCategory(val displayName: String, val iconName: String) {
  WORK("Work", "work"),
  CALL("Call", "call"),
  REMINDER("Reminder", "alarm"),
  PERSONAL("Personal", "person"),
  SYSTEM("System", "settings")
}

data class ScheduleItem(
  val id: String = UUID.randomUUID().toString(),
  val title: String,
  val time: String,
  val date: String,
  val category: ScheduleCategory = ScheduleCategory.REMINDER,
  val isCompleted: Boolean = false,
  val details: String = ""
)
