package com.example.ai

import com.example.data.ScheduleCategory
import com.example.system.ContactResult
import com.example.system.DeviceController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JarvisBrain(
  private val deviceController: DeviceController? = null,
  private val geminiClient: JarvisGeminiClient = JarvisGeminiClient()
) {

  suspend fun processCommand(input: String): JarvisCommandResult {
    val clean = input.trim()
    if (clean.isBlank()) {
      return JarvisCommandResult(
        action = JarvisAction.ConversationalAction("I am ready for your instructions, Boss."),
        speechResponse = "Awaiting your command, Boss.",
        executionLog = "[SYSTEM] Empty input received."
      )
    }

    // Step 1: Rapid Local NLP Intent Parsing for zero latency on core phone controls
    val localResult = parseLocalIntent(clean)
    if (localResult != null) {
      return localResult
    }

    // Step 2: Cloud Gemini AI reasoning for conversational or complex instructions
    val geminiResult = geminiClient.askGemini(clean)
    if (geminiResult != null) {
      // If Gemini returned a CallAction, check for contact disambiguation
      if (geminiResult.action is JarvisAction.CallAction) {
        val disambiguation = checkCallDisambiguation(geminiResult.action.target)
        if (disambiguation != null) return disambiguation
      }
      return geminiResult
    }

    // Step 3: Fallback on-device conversational intelligence
    return generateFallbackResponse(clean)
  }

  private fun parseLocalIntent(raw: String): JarvisCommandResult? {
    val lower = raw.lowercase(Locale.ROOT).trim()

    // --- 1. CALL COMMANDS & CONTACT DISAMBIGUATION ---
    // Matches: "Call John", "Call John Doe", "Call +1-555-0101", "Dial 9876543210", "Rohit ko call karo"
    val callRegex = Regex("^(?:call|dial|phone)\\s+(.+?)(?:\\s+please|\\s+now)?$", RegexOption.IGNORE_CASE)
    val hindiCallRegex = Regex("^(.+?)(?:\\s+ko\\s+call\\s+(?:karo|lagao)|\\s+ko\\s+phone\\s+lagao)$", RegexOption.IGNORE_CASE)

    val callMatch = callRegex.find(raw) ?: hindiCallRegex.find(raw)
    if (callMatch != null) {
      val target = callMatch.groupValues[1].trim()
      if (target.isNotBlank()) {
        val disambiguation = checkCallDisambiguation(target)
        if (disambiguation != null) {
          return disambiguation
        }

        // Single contact or direct number
        val isNumber = target.replace(Regex("[^0-9+]"), "").length >= 3 &&
            target.matches(Regex("[+]?[0-9\\-\\s()]{3,}"))

        val responseText = if (isNumber) {
          "Connecting call to $target immediately, Boss."
        } else {
          "Initiating call to $target, Boss."
        }

        return JarvisCommandResult(
          action = JarvisAction.CallAction(target),
          speechResponse = responseText,
          executionLog = "[DIRECT CALL] Placing telephonic link to $target"
        )
      }
    }

    // --- 2. TEXT MESSAGING COMMANDS (SMS & WHATSAPP) ---
    // Matches: "Send a message to John saying I'll be late", "Text Mom saying I am on my way",
    // "Send message to John saying I will be late", "WhatsApp message to John saying I'll be late"
    val msgWithSayingRegex = Regex(
      "^(?:send\\s+(?:a\\s+)?(?:text\\s+)?message\\s+to|send\\s+(?:a\\s+)?text\\s+to|text|message)\\s+([\\w\\s+]+?)\\s+(?:saying|that)\\s+(.+)$",
      RegexOption.IGNORE_CASE
    )
    val whatsappWithSayingRegex = Regex(
      "^(?:whatsapp(?:\\s+message)?\\s+to|send\\s+whatsapp(?:\\s+message)?\\s+to)\\s+([\\w\\s+]+?)\\s+(?:saying|that)\\s+(.+)$",
      RegexOption.IGNORE_CASE
    )

    val msgSayingMatch = msgWithSayingRegex.find(raw)
    if (msgSayingMatch != null) {
      val target = msgSayingMatch.groupValues[1].trim()
      val message = msgSayingMatch.groupValues[2].trim()
      return JarvisCommandResult(
        action = JarvisAction.MessageAction(target, message, isWhatsApp = false),
        speechResponse = "Drafting SMS to $target: \"$message\", Boss.",
        executionLog = "[SMS DISPATCH] Target: $target | Message: \"$message\""
      )
    }

    val whatsappSayingMatch = whatsappWithSayingRegex.find(raw)
    if (whatsappSayingMatch != null) {
      val target = whatsappSayingMatch.groupValues[1].trim()
      val message = whatsappSayingMatch.groupValues[2].trim()
      return JarvisCommandResult(
        action = JarvisAction.MessageAction(target, message, isWhatsApp = true),
        speechResponse = "Opening WhatsApp to send message to $target: \"$message\", Boss.",
        executionLog = "[WHATSAPP DISPATCH] Target: $target | Message: \"$message\""
      )
    }

    // Hinglish / colloquial messaging patterns
    if (lower.contains("message") || lower.contains("msg") || lower.contains("sms") || lower.contains("whatsapp")) {
      if (lower.contains("bhejo") || lower.contains("send") || lower.contains("write")) {
        val isWhatsApp = lower.contains("whatsapp")
        var target = "Contact"
        var msgText = "Hello"

        if (lower.contains("to ")) {
          val idxTo = raw.indexOf("to ", ignoreCase = true)
          val remainder = raw.substring(idxTo + 3).trim()
          val parts = remainder.split(" ", limit = 2)
          target = parts.getOrNull(0) ?: "Contact"
          msgText = parts.getOrNull(1) ?: "Greetings from Jarvis."
        } else if (lower.contains("ko")) {
          val parts = raw.split("ko", limit = 2)
          target = parts[0].replace(Regex("(?i)whatsapp|pe|message|bhejo"), "").trim()
          val afterKo = parts[1].trim()
          msgText = afterKo.replace(Regex("(?i)message bhejo|msg bhejo|likho|send"), "").trim()
        }

        if (msgText.isBlank()) msgText = "Hey there!"
        return JarvisCommandResult(
          action = JarvisAction.MessageAction(target, msgText, isWhatsApp),
          speechResponse = if (isWhatsApp) "Opening WhatsApp for $target, Boss." else "Preparing message for $target, Boss.",
          executionLog = "[DISPATCH PROTOCOL] Dispatching to $target via ${if (isWhatsApp) "WhatsApp" else "SMS"}"
        )
      }
    }

    // --- 3. APPLICATION OPENING COMMANDS ---
    // Matches: "Open YouTube", "Open Camera", "Open WhatsApp", "Open Chrome", "Launch Spotify", "YouTube kholo"
    val openRegex = Regex("^(?:open|launch|start|run)\\s+([\\w\\s]+?)(?:\\s+app|\\s+application)?$", RegexOption.IGNORE_CASE)
    val hindiOpenRegex = Regex("^([\\w\\s]+?)\\s+(?:kholo|chalao|open\\s+karo)$", RegexOption.IGNORE_CASE)

    val openMatch = openRegex.find(raw) ?: hindiOpenRegex.find(raw)
    if (openMatch != null) {
      val appName = openMatch.groupValues[1].trim()
      if (appName.isNotBlank() && !appName.equals("settings", ignoreCase = true) && !appName.equals("setting", ignoreCase = true)) {
        return JarvisCommandResult(
          action = JarvisAction.OpenAppAction(appName),
          speechResponse = "Opening $appName immediately, Boss.",
          executionLog = "[APP PROTOCOL] Executing launch sequence for $appName"
        )
      }
    }

    // --- 4. HARDWARE & SETTINGS ADJUSTMENTS ---

    // Brightness adjustment: "increase brightness", "decrease brightness", "turn up brightness", "lower brightness"
    if (lower.contains("brightness") || lower.contains("roshni")) {
      val isIncrease = when {
        lower.contains("increase") || lower.contains("up") || lower.contains("raise") || lower.contains("high") || lower.contains("badhao") -> true
        lower.contains("decrease") || lower.contains("down") || lower.contains("lower") || lower.contains("dim") || lower.contains("kam") -> false
        else -> null
      }
      val speech = when (isIncrease) {
        true -> "Opening Display settings to increase brightness, Boss."
        false -> "Opening Display settings to decrease brightness, Boss."
        null -> "Opening brightness and display settings."
      }
      return JarvisCommandResult(
        action = JarvisAction.BrightnessAction(isIncrease),
        speechResponse = speech,
        executionLog = "[HARDWARE PROTOCOL] Brightness adjustment command: $isIncrease"
      )
    }

    // Volume adjustment: "increase volume", "decrease volume", "turn up volume", "lower volume", "volume up"
    if (lower.contains("volume") || lower.contains("awaz")) {
      val isRaise = lower.contains("increase") || lower.contains("up") || lower.contains("raise") || lower.contains("high") || lower.contains("badhao")
      val speech = if (isRaise) "Increasing audio volume, Boss." else "Lowering audio volume, Boss."
      return JarvisCommandResult(
        action = JarvisAction.VolumeAction(isRaise),
        speechResponse = speech,
        executionLog = "[AUDIO PROTOCOL] Volume adjustment: ${if (isRaise) "+1" else "-1"}"
      )
    }

    // Wi-Fi commands: "turn on wi-fi", "turn off wi-fi", "open wi-fi", "wi-fi settings"
    if (lower.contains("wi-fi") || lower.contains("wifi")) {
      return JarvisCommandResult(
        action = JarvisAction.SettingAction("wifi"),
        speechResponse = "Accessing Wi-Fi control panel, Boss.",
        executionLog = "[SETTINGS PROTOCOL] Wi-Fi settings requested"
      )
    }

    // Bluetooth commands: "turn on bluetooth", "turn off bluetooth", "bluetooth settings"
    if (lower.contains("bluetooth") || lower.contains("bt")) {
      return JarvisCommandResult(
        action = JarvisAction.SettingAction("bluetooth"),
        speechResponse = "Accessing Bluetooth control interface, Boss.",
        executionLog = "[SETTINGS PROTOCOL] Bluetooth settings requested"
      )
    }

    // Flashlight / Torch: "turn on flashlight", "turn off flashlight", "torch on", "torch jalao"
    if (lower.contains("torch") || lower.contains("flashlight")) {
      val enable = when {
        lower.contains("on") || lower.contains("chalu") || lower.contains("jalao") || lower.contains("start") -> true
        lower.contains("off") || lower.contains("band") || lower.contains("bujhao") || lower.contains("stop") -> false
        else -> null
      }
      val responseText = when (enable) {
        true -> "Illumination active. Flashlight is ON, Boss."
        false -> "Flashlight deactivated, Boss."
        null -> "Toggling illumination unit."
      }
      return JarvisCommandResult(
        action = JarvisAction.TorchAction(enable),
        speechResponse = responseText,
        executionLog = "[HARDWARE PROTOCOL] Flashlight state adjusted: ${enable ?: "TOGGLED"}"
      )
    }

    // General settings
    if (lower.contains("open setting") || lower.contains("settings kholo") || lower.contains("settings open")) {
      return JarvisCommandResult(
        action = JarvisAction.SettingAction("general"),
        speechResponse = "Accessing main device settings, Boss.",
        executionLog = "[SETTINGS PROTOCOL] Master settings launched"
      )
    }

    // Sound / Audio settings
    if (lower.contains("sound setting") || lower.contains("audio setting")) {
      return JarvisCommandResult(
        action = JarvisAction.SettingAction("sound"),
        speechResponse = "Opening sound configuration panel, Boss.",
        executionLog = "[SETTINGS PROTOCOL] Sound settings launched"
      )
    }

    // Telemetry / Battery
    if (lower.contains("battery") || lower.contains("charge") || lower.contains("power status") || lower.contains("phone status")) {
      return JarvisCommandResult(
        action = JarvisAction.SystemStatusAction,
        speechResponse = "Checking power core diagnostics, Boss. Systems nominal.",
        executionLog = "[TELEMETRY PROTOCOL] Battery and memory stats checked"
      )
    }

    // Schedule & Alarm
    if (lower.contains("alarm") && (lower.contains("lagao") || lower.contains("set") || lower.contains("karo"))) {
      val hourMatch = Regex("(\\d{1,2})(:(\\d{2}))?\\s*(am|pm)?").find(lower)
      var hour = 7
      var minute = 0
      if (hourMatch != null) {
        val rawH = hourMatch.groupValues[1].toIntOrNull() ?: 7
        val rawM = hourMatch.groupValues[3].toIntOrNull() ?: 0
        val amPm = hourMatch.groupValues[4]
        hour = if (amPm == "pm" && rawH < 12) rawH + 12 else if (amPm == "am" && rawH == 12) 0 else rawH
        minute = rawM
      }
      return JarvisCommandResult(
        action = JarvisAction.AlarmAction(hour, minute, "Jarvis Alarm"),
        speechResponse = "Alarm set for ${String.format("%02d:%02d", hour, minute)}, Boss.",
        executionLog = "[TIME PROTOCOL] Alarm configured for $hour:$minute"
      )
    }

    if (lower.contains("reminder") || lower.contains("schedule") || lower.contains("task")) {
      val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
      val title = raw.replace(Regex("(?i)reminder lagao|add to schedule|schedule|set reminder for|karo|bhi"), "").trim()
        .ifEmpty { "Important Task" }

      return JarvisCommandResult(
        action = JarvisAction.ScheduleAction(
          title = title,
          time = "02:00 PM",
          date = today,
          category = ScheduleCategory.REMINDER
        ),
        speechResponse = "I have logged '$title' into your daily schedule, Boss.",
        executionLog = "[SCHEDULE PROTOCOL] Task logged to database: $title"
      )
    }

    return null
  }

  /**
   * Checks whether the target matches multiple contacts and requires disambiguation.
   */
  private fun checkCallDisambiguation(target: String): JarvisCommandResult? {
    val clean = target.trim()
    // Don't disambiguate explicit phone numbers
    val isPhoneNumber = clean.replace(Regex("[^0-9+]"), "").length >= 3 &&
        clean.matches(Regex("[+]?[0-9\\-\\s()]{3,}"))

    if (isPhoneNumber) return null

    val matches = deviceController?.searchContacts(clean) ?: emptyList()
    if (matches.size > 1) {
      val namesList = matches.take(3).joinToString(" or ") { it.name }
      return JarvisCommandResult(
        action = JarvisAction.DisambiguateCallAction(clean, matches),
        speechResponse = "I found ${matches.size} contacts matching '$clean': $namesList. Which one would you like to call, Boss?",
        executionLog = "[DISAMBIGUATION REQUIRED] Multiple contacts found for '$clean': ${matches.size} candidates"
      )
    }

    return null
  }

  private fun generateFallbackResponse(input: String): JarvisCommandResult {
    val lower = input.lowercase(Locale.ROOT)
    val reply = when {
      lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ->
        "Jarvis systems fully operational, Boss. How may I assist your phone operations today?"
      lower.contains("kaise ho") || lower.contains("how are you") ->
        "All internal reactors and subsystems running at 100% capacity, Boss. Ready for your command."
      lower.contains("who are you") || lower.contains("kaun ho") || lower.contains("naam") ->
        "I am J.A.R.V.I.S., your dedicated AI phone controller. I handle calls, messages, apps, device settings, and your daily schedule."
      lower.contains("thank") || lower.contains("shukriya") ->
        "Always an honor to serve you, Boss."
      else ->
        "Protocol acknowledged, Boss. All phone control modules, apps, contacts, and schedules are at your disposal."
    }

    return JarvisCommandResult(
      action = JarvisAction.ConversationalAction(reply),
      speechResponse = reply,
      executionLog = "[NEURAL CORE] Conversational query processed"
    )
  }
}
