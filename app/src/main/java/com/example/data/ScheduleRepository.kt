package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScheduleRepository(private val context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("jarvis_schedule_prefs", Context.MODE_PRIVATE)

  private val _scheduleItems = MutableStateFlow<List<ScheduleItem>>(emptyList())
  val scheduleItems: StateFlow<List<ScheduleItem>> = _scheduleItems.asStateFlow()

  init {
    loadItems()
  }

  private fun loadItems() {
    val rawJson = prefs.getString("items_json", null)
    if (rawJson.isNullOrBlank()) {
      // Seed default schedule for Jarvis
      val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
      val defaults = listOf(
        ScheduleItem(
          title = "Morning Diagnostics & Daily Briefing",
          time = "09:00 AM",
          date = today,
          category = ScheduleCategory.SYSTEM,
          details = "Jarvis system readiness check and schedule alignment."
        ),
        ScheduleItem(
          title = "Review Project Deliverables",
          time = "11:30 AM",
          date = today,
          category = ScheduleCategory.WORK,
          details = "Coordinate tasks and check pending messages."
        ),
        ScheduleItem(
          title = "Call Team / Manager",
          time = "02:00 PM",
          date = today,
          category = ScheduleCategory.CALL,
          details = "Daily sync call and status update."
        ),
        ScheduleItem(
          title = "System Maintenance & Backup",
          time = "06:00 PM",
          date = today,
          category = ScheduleCategory.SYSTEM,
          details = "Check phone battery health, storage, and clean temporary logs."
        ),
        ScheduleItem(
          title = "Workout & Evening Refresh",
          time = "07:30 PM",
          date = today,
          category = ScheduleCategory.PERSONAL,
          details = "30-minute cardio and hydration reminder."
        )
      )
      _scheduleItems.value = defaults
      saveItems(defaults)
    } else {
      try {
        val array = JSONArray(rawJson)
        val list = mutableListOf<ScheduleItem>()
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          list.add(
            ScheduleItem(
              id = obj.optString("id"),
              title = obj.optString("title"),
              time = obj.optString("time"),
              date = obj.optString("date"),
              category = ScheduleCategory.valueOf(obj.optString("category", ScheduleCategory.REMINDER.name)),
              isCompleted = obj.optBoolean("isCompleted", false),
              details = obj.optString("details", "")
            )
          )
        }
        _scheduleItems.value = list
      } catch (e: Exception) {
        _scheduleItems.value = emptyList()
      }
    }
  }

  private fun saveItems(items: List<ScheduleItem>) {
    val array = JSONArray()
    for (item in items) {
      val obj = JSONObject()
      obj.put("id", item.id)
      obj.put("title", item.title)
      obj.put("time", item.time)
      obj.put("date", item.date)
      obj.put("category", item.category.name)
      obj.put("isCompleted", item.isCompleted)
      obj.put("details", item.details)
      array.put(obj)
    }
    prefs.edit().putString("items_json", array.toString()).apply()
  }

  fun addItem(title: String, time: String, date: String, category: ScheduleCategory = ScheduleCategory.REMINDER, details: String = ""): ScheduleItem {
    val newItem = ScheduleItem(
      title = title,
      time = time,
      date = date,
      category = category,
      details = details
    )
    val updated = _scheduleItems.value + newItem
    _scheduleItems.value = updated
    saveItems(updated)
    return newItem
  }

  fun toggleItem(id: String) {
    val updated = _scheduleItems.value.map {
      if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it
    }
    _scheduleItems.value = updated
    saveItems(updated)
  }

  fun deleteItem(id: String) {
    val updated = _scheduleItems.value.filter { it.id != id }
    _scheduleItems.value = updated
    saveItems(updated)
  }
}
