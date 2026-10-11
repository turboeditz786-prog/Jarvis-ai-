package com.example.ai

import com.example.data.ScheduleCategory
import com.example.system.ContactResult

sealed class JarvisAction {
  data class CallAction(val target: String) : JarvisAction()
  data class DisambiguateCallAction(val query: String, val candidates: List<ContactResult>) : JarvisAction()
  data class MessageAction(val target: String, val message: String, val isWhatsApp: Boolean) : JarvisAction()
  data class OpenAppAction(val appName: String) : JarvisAction()
  data class TorchAction(val enable: Boolean? = null) : JarvisAction()
  data class SettingAction(val settingType: String, val parameter: String? = null) : JarvisAction()
  data class VolumeAction(val raise: Boolean) : JarvisAction()
  data class BrightnessAction(val increase: Boolean?) : JarvisAction()
  data class ScheduleAction(
    val title: String,
    val time: String,
    val date: String,
    val category: ScheduleCategory = ScheduleCategory.REMINDER
  ) : JarvisAction()
  data class AlarmAction(val hour: Int, val minute: Int, val label: String) : JarvisAction()
  data object SystemStatusAction : JarvisAction()
  data class ConversationalAction(val response: String) : JarvisAction()
}

data class JarvisCommandResult(
  val action: JarvisAction,
  val speechResponse: String,
  val executionLog: String
)
