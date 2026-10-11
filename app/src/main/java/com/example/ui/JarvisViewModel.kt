package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.JarvisAction
import com.example.ai.JarvisBrain
import com.example.ai.JarvisCommandResult
import com.example.data.ScheduleCategory
import com.example.data.ScheduleItem
import com.example.data.ScheduleRepository
import com.example.system.ContactResult
import com.example.system.DeviceController
import com.example.system.SystemStats
import com.example.voice.AssistantListeningState
import com.example.voice.JarvisVoiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class JarvisTab {
  ASSISTANT,
  SCHEDULE,
  DEVICE_CONTROL,
  SETTINGS
}

data class JarvisUiState(
  val currentTab: JarvisTab = JarvisTab.ASSISTANT,
  val listeningState: AssistantListeningState = AssistantListeningState.IDLE,
  val audioRms: Float = 0f,
  val userTranscript: String = "",
  val jarvisSpeech: String = "JARVIS Core Online. All systems nominal, Boss.",
  val executionLogs: List<String> = listOf(
    "[SYSTEM BOOT] Mark 85 Neural Cores initialized",
    "[TELEMETRY] Sensor arrays connected",
    "[AUDIO] Speech synthesizer and recognizer ready"
  ),
  val systemStats: SystemStats = SystemStats(
    batteryPercentage = 95,
    isCharging = false,
    availableStorageGb = "28.4",
    totalStorageGb = "64.0",
    isWifiConnected = true,
    isTorchOn = false
  ),
  val isTorchActive: Boolean = false,
  val lastExecutedActionText: String? = null,
  val disambiguationContacts: List<ContactResult> = emptyList(),
  val disambiguationQuery: String = "",
  val voiceSpeed: Float = 1.05f,
  val voicePitch: Float = 0.95f,
  val personaName: String = "Tony Stark AI (Boss)"
)

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

  private val deviceController = DeviceController(application)
  private val scheduleRepository = ScheduleRepository(application)
  private val brain = JarvisBrain(deviceController = deviceController)

  private val _uiState = MutableStateFlow(JarvisUiState())
  val uiState: StateFlow<JarvisUiState> = _uiState.asStateFlow()

  val scheduleItems: StateFlow<List<ScheduleItem>> = scheduleRepository.scheduleItems

  private val voiceManager = JarvisVoiceManager(application) { speechText ->
    onSpeechRecognized(speechText)
  }

  init {
    viewModelScope.launch {
      voiceManager.state.collect { state ->
        _uiState.value = _uiState.value.copy(listeningState = state)
      }
    }
    viewModelScope.launch {
      voiceManager.rmsLevel.collect { rms ->
        _uiState.value = _uiState.value.copy(audioRms = rms)
      }
    }
    viewModelScope.launch {
      voiceManager.partialTranscript.collect { partial ->
        if (partial.isNotBlank()) {
          _uiState.value = _uiState.value.copy(userTranscript = partial)
        }
      }
    }

    refreshSystemStats()
  }

  fun refreshSystemStats() {
    val stats = deviceController.getSystemStats()
    _uiState.value = _uiState.value.copy(
      systemStats = stats,
      isTorchActive = stats.isTorchOn
    )
  }

  fun selectTab(tab: JarvisTab) {
    _uiState.value = _uiState.value.copy(currentTab = tab)
  }

  fun toggleVoiceListening() {
    val current = _uiState.value.listeningState
    if (current == AssistantListeningState.LISTENING) {
      voiceManager.stopListening()
    } else {
      voiceManager.startListening()
    }
  }

  fun stopSpeaking() {
    voiceManager.stopSpeaking()
  }

  private fun onSpeechRecognized(text: String) {
    executeCommand(text)
  }

  fun executeCommand(command: String) {
    val clean = command.trim()
    if (clean.isBlank()) return

    _uiState.value = _uiState.value.copy(
      userTranscript = clean,
      listeningState = AssistantListeningState.PROCESSING
    )

    viewModelScope.launch {
      val result = brain.processCommand(clean)
      handleResult(result)
    }
  }

  private fun handleResult(result: JarvisCommandResult) {
    var actionNote = ""

    when (val action = result.action) {
      is JarvisAction.DisambiguateCallAction -> {
        _uiState.value = _uiState.value.copy(
          disambiguationContacts = action.candidates,
          disambiguationQuery = action.query
        )
        actionNote = "Multiple matches for '${action.query}': ${action.candidates.size} contacts found"
      }

      is JarvisAction.CallAction -> {
        _uiState.value = _uiState.value.copy(
          disambiguationContacts = emptyList(),
          disambiguationQuery = ""
        )
        // Check if there is an exact number or 1 contact
        val isNumber = action.target.replace(Regex("[^0-9+]"), "").length >= 3 &&
            action.target.matches(Regex("[+]?[0-9\\-\\s()]{3,}"))
        if (isNumber) {
          actionNote = deviceController.makeDirectCall(action.target, action.target)
        } else {
          val matches = deviceController.searchContacts(action.target)
          if (matches.size > 1) {
            _uiState.value = _uiState.value.copy(
              disambiguationContacts = matches,
              disambiguationQuery = action.target
            )
            actionNote = "Disambiguation required for ${action.target}"
          } else if (matches.size == 1) {
            actionNote = deviceController.makeDirectCall(matches.first().phoneNumber, matches.first().name)
          } else {
            actionNote = deviceController.makeDirectCall(action.target, action.target)
          }
        }
      }

      is JarvisAction.MessageAction -> {
        actionNote = if (action.isWhatsApp) {
          deviceController.sendWhatsApp(action.target, action.message)
        } else {
          deviceController.sendSms(action.target, action.message)
        }
      }

      is JarvisAction.OpenAppAction -> {
        actionNote = deviceController.launchApp(action.appName)
      }

      is JarvisAction.TorchAction -> {
        actionNote = deviceController.toggleFlashlight(action.enable)
        refreshSystemStats()
      }

      is JarvisAction.VolumeAction -> {
        actionNote = deviceController.adjustVolume(action.raise)
      }

      is JarvisAction.BrightnessAction -> {
        actionNote = deviceController.adjustBrightness(action.increase)
      }

      is JarvisAction.SettingAction -> {
        actionNote = deviceController.openSettings(action.settingType)
      }

      is JarvisAction.ScheduleAction -> {
        scheduleRepository.addItem(
          title = action.title,
          time = action.time,
          date = action.date,
          category = action.category
        )
        actionNote = "Scheduled: ${action.title} at ${action.time}"
      }

      is JarvisAction.AlarmAction -> {
        actionNote = deviceController.setAlarm(action.hour, action.minute, action.label)
      }

      is JarvisAction.SystemStatusAction -> {
        refreshSystemStats()
        val stats = _uiState.value.systemStats
        actionNote = "Battery: ${stats.batteryPercentage}% | Storage: ${stats.availableStorageGb}GB free"
      }

      is JarvisAction.ConversationalAction -> {
        actionNote = "Speech transmission active"
      }
    }

    val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
    val newLogs = (_uiState.value.executionLogs + listOf(
      "[$timestamp] ${result.executionLog}",
      if (actionNote.isNotBlank()) "[$timestamp RESULT] $actionNote" else ""
    ).filter { it.isNotBlank() }).takeLast(20)

    _uiState.value = _uiState.value.copy(
      jarvisSpeech = result.speechResponse,
      lastExecutedActionText = actionNote,
      executionLogs = newLogs
    )

    voiceManager.speak(result.speechResponse)
    refreshSystemStats()
  }

  // --- Calling Disambiguation helpers ---
  fun callSelectedContact(contact: ContactResult) {
    _uiState.value = _uiState.value.copy(
      disambiguationContacts = emptyList(),
      disambiguationQuery = ""
    )
    val note = deviceController.makeDirectCall(contact.phoneNumber, contact.name)
    val speech = "Calling ${contact.name} now, Boss."
    voiceManager.speak(speech)
    _uiState.value = _uiState.value.copy(
      jarvisSpeech = speech,
      lastExecutedActionText = note
    )
  }

  fun dismissDisambiguation() {
    _uiState.value = _uiState.value.copy(
      disambiguationContacts = emptyList(),
      disambiguationQuery = ""
    )
  }

  // --- Quick Actions for UI chips/buttons ---
  fun quickCall(target: String) {
    executeCommand("Call $target")
  }

  fun quickOpenApp(app: String) {
    executeCommand("Open $app")
  }

  fun quickToggleTorch() {
    executeCommand("Toggle flashlight")
  }

  fun quickCheckBattery() {
    executeCommand("Check battery status")
  }

  fun quickOpenSettings(type: String) {
    executeCommand("Open $type settings")
  }

  // --- Schedule management ---
  fun addSchedule(title: String, time: String, date: String, category: ScheduleCategory) {
    scheduleRepository.addItem(title, time, date, category)
    val msg = "Added $title to schedule for $time"
    voiceManager.speak(msg)
    _uiState.value = _uiState.value.copy(
      jarvisSpeech = msg,
      lastExecutedActionText = msg
    )
  }

  fun toggleScheduleItem(id: String) {
    scheduleRepository.toggleItem(id)
  }

  fun deleteScheduleItem(id: String) {
    scheduleRepository.deleteItem(id)
  }

  // --- Persona and Voice settings ---
  fun updateVoiceSettings(speed: Float, pitch: Float) {
    voiceManager.speechRate = speed
    voiceManager.pitch = pitch
    _uiState.value = _uiState.value.copy(voiceSpeed = speed, voicePitch = pitch)
  }

  fun setPersona(persona: String) {
    _uiState.value = _uiState.value.copy(personaName = persona)
    voiceManager.speak("Persona updated to $persona, Boss.")
  }

  override fun onCleared() {
    super.onCleared()
    voiceManager.destroy()
  }
}
