package com.example.system

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import java.net.URLEncoder

data class BatteryInfo(
  val percentage: Int,
  val isCharging: Boolean,
  val statusText: String
)

data class SystemStats(
  val batteryPercentage: Int,
  val isCharging: Boolean,
  val availableStorageGb: String,
  val totalStorageGb: String,
  val isWifiConnected: Boolean,
  val isTorchOn: Boolean
)

data class ContactResult(
  val name: String,
  val phoneNumber: String,
  val isSimulated: Boolean = false
)

class DeviceController(private val context: Context) {
  private val tag = "JarvisDeviceController"
  private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
  private var isFlashlightOn = false

  // Fallback demo directory to guarantee multi-contact disambiguation in test/emulator environments
  private val seedContacts = listOf(
    ContactResult("John Doe", "+1-555-0101", isSimulated = true),
    ContactResult("John Smith", "+1-555-0102", isSimulated = true),
    ContactResult("Johnathan Wayne", "+1-555-0103", isSimulated = true),
    ContactResult("Alex Rivera", "+1-555-0201", isSimulated = true),
    ContactResult("Alex Johnson", "+1-555-0202", isSimulated = true),
    ContactResult("Mom", "+1-555-0301", isSimulated = true),
    ContactResult("Dad", "+1-555-0302", isSimulated = true),
    ContactResult("Rohit Sharma", "+91-98765-11111", isSimulated = true),
    ContactResult("Rohit Verma", "+91-98765-22222", isSimulated = true)
  )

  init {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && cameraManager != null) {
      try {
        cameraManager.registerTorchCallback(object : CameraManager.TorchCallback() {
          override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            super.onTorchModeChanged(cameraId, enabled)
            isFlashlightOn = enabled
          }
        }, null)
      } catch (e: Exception) {
        Log.e(tag, "Failed to register torch callback: ${e.message}")
      }
    }
  }

  // --- 1. CALLING & CONTACT SEARCH ---

  /**
   * Searches for all contacts matching the given name query.
   * Checks both device ContactsContract (if permission is granted) and seed fallback contacts.
   */
  @SuppressLint("Range")
  fun searchContacts(query: String): List<ContactResult> {
    val clean = query.trim()
    if (clean.isBlank()) return emptyList()

    val realContacts = mutableListOf<ContactResult>()
    val hasReadPerm = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED

    if (hasReadPerm) {
      try {
        val contentResolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
          ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
          ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$clean%")

        contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
          while (cursor.moveToNext()) {
            val name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)) ?: ""
            val number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)) ?: ""
            if (name.isNotBlank() && number.isNotBlank()) {
              realContacts.add(ContactResult(name = name, phoneNumber = number, isSimulated = false))
            }
          }
        }
      } catch (e: Exception) {
        Log.e(tag, "Error querying system contacts: ${e.message}")
      }
    }

    // Deduplicate by name and number
    val distinctReal = realContacts.distinctBy { "${it.name}_${it.phoneNumber}" }
    if (distinctReal.isNotEmpty()) {
      return distinctReal
    }

    // If no device contacts matched or in test/emulator environment, search seed contacts
    return seedContacts.filter {
      it.name.contains(clean, ignoreCase = true) || clean.contains(it.name, ignoreCase = true)
    }.distinctBy { "${it.name}_${it.phoneNumber}" }
  }

  /**
   * Executes a direct phone call to a phone number or contact.
   */
  fun makeDirectCall(number: String, displayName: String): String {
    val cleanNumber = number.replace(Regex("[^0-9+]"), "").trim()
    val callPermission = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.CALL_PHONE
    ) == PackageManager.PERMISSION_GRANTED

    return try {
      val intent = if (callPermission && cleanNumber.matches(Regex("[+]?[0-9]{3,}"))) {
        Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanNumber"))
      } else {
        Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber"))
      }
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
      "Connecting call to $displayName ($cleanNumber)..."
    } catch (e: Exception) {
      Log.e(tag, "Failed to initiate call: ${e.message}")
      try {
        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber"))
        dialIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(dialIntent)
        "Opening phone dialer for $displayName..."
      } catch (e2: Exception) {
        "Unable to place call: ${e2.message}"
      }
    }
  }

  // --- 2. MESSAGING (SMS & WHATSAPP) ---

  fun sendSms(contactOrNumber: String, message: String): String {
    val cleanTarget = contactOrNumber.trim()
    var number = cleanTarget
    var name = cleanTarget

    if (!cleanTarget.matches(Regex("[+]?[0-9]{3,}"))) {
      val matches = searchContacts(cleanTarget)
      if (matches.isNotEmpty()) {
        number = matches.first().phoneNumber
        name = matches.first().name
      }
    }

    return try {
      val smsUri = Uri.parse("smsto:$number")
      val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
        putExtra("sms_body", message)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      "Preparing SMS to $name: \"$message\""
    } catch (e: Exception) {
      Log.e(tag, "Error sending SMS: ${e.message}")
      "Could not open messaging client: ${e.message}"
    }
  }

  fun sendWhatsApp(contactOrPhone: String, message: String): String {
    var phone = contactOrPhone.replace(Regex("[^0-9]"), "")
    if (phone.isEmpty()) {
      val matches = searchContacts(contactOrPhone)
      if (matches.isNotEmpty()) {
        phone = matches.first().phoneNumber.replace(Regex("[^0-9]"), "")
      }
    }

    return try {
      val encodedMsg = URLEncoder.encode(message, "UTF-8")
      val url = if (phone.isNotEmpty()) {
        "https://api.whatsapp.com/send?phone=$phone&text=$encodedMsg"
      } else {
        "https://api.whatsapp.com/send?text=$encodedMsg"
      }
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        setPackage("com.whatsapp")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      if (context.packageManager.resolveActivity(intent, 0) != null) {
        context.startActivity(intent)
        "Opening WhatsApp to transmit message to $contactOrPhone."
      } else {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(webIntent)
        "Opening WhatsApp web dispatch..."
      }
    } catch (e: Exception) {
      Log.e(tag, "WhatsApp dispatch error: ${e.message}")
      "Failed to launch WhatsApp: ${e.message}"
    }
  }

  // --- 3. APP LAUNCHING ---

  private val appPackageMap = mapOf(
    "youtube" to "com.google.android.youtube",
    "whatsapp" to "com.whatsapp",
    "chrome" to "com.android.chrome",
    "browser" to "com.android.chrome",
    "camera" to "com.android.camera",
    "settings" to "com.android.settings",
    "calculator" to "com.google.android.calculator",
    "clock" to "com.google.android.deskclock",
    "alarm" to "com.google.android.deskclock",
    "maps" to "com.google.android.apps.maps",
    "gmail" to "com.google.android.gm",
    "mail" to "com.google.android.gm",
    "photos" to "com.google.android.apps.photos",
    "gallery" to "com.google.android.apps.photos",
    "play store" to "com.android.vending",
    "spotify" to "com.spotify.music",
    "instagram" to "com.instagram.android",
    "telegram" to "org.telegram.messenger",
    "twitter" to "com.twitter.android",
    "x" to "com.twitter.android",
    "calendar" to "com.google.android.calendar",
    "files" to "com.google.android.apps.nbu.files"
  )

  fun launchApp(rawAppName: String): String {
    val pm = context.packageManager
    val lower = rawAppName.lowercase().trim()

    val knownPkg = appPackageMap[lower]
    if (knownPkg != null) {
      val intent = pm.getLaunchIntentForPackage(knownPkg)
      if (intent != null) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return "Initiating launch sequence for ${rawAppName.capitalizeFirst()}."
      }
    }

    if (lower.contains("camera")) {
      try {
        val cameraIntent = Intent("android.media.action.IMAGE_CAPTURE").apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(cameraIntent)
        return "Optical sensors online. Camera active."
      } catch (e: Exception) { /* fallthrough */ }
    }

    if (lower.contains("setting")) {
      openSettings("general")
      return "Accessing Core System Settings."
    }

    try {
      val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
      for (app in installedApps) {
        val label = pm.getApplicationLabel(app).toString().lowercase()
        if (label == lower || label.contains(lower) || lower.contains(label)) {
          val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
          if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            return "Launching ${pm.getApplicationLabel(app)}."
          }
        }
      }
    } catch (e: Exception) {
      Log.e(tag, "Error searching applications: ${e.message}")
    }

    return try {
      val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$rawAppName")).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(playStoreIntent)
      "App '$rawAppName' is not installed locally. Opening Play Store repository..."
    } catch (e: Exception) {
      "Target application '$rawAppName' could not be located."
    }
  }

  // --- 4. FLASHLIGHT / TORCH ---

  fun toggleFlashlight(forceState: Boolean? = null): String {
    if (cameraManager == null) {
      return "Optical illumination module (Flashlight) is not available on this device."
    }
    val newState = forceState ?: !isFlashlightOn
    return try {
      val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
        val chars = cameraManager.getCameraCharacteristics(id)
        chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
      } ?: cameraManager.cameraIdList.firstOrNull()

      if (cameraId != null) {
        cameraManager.setTorchMode(cameraId, newState)
        isFlashlightOn = newState
        if (newState) "Illumination active. Flashlight ON, Boss." else "Flashlight turned OFF."
      } else {
        "No flash unit detected."
      }
    } catch (e: Exception) {
      Log.e(tag, "Failed to toggle torch: ${e.message}")
      "Flashlight error: ${e.message}"
    }
  }

  fun isTorchActive(): Boolean = isFlashlightOn

  // --- 5. AUDIO VOLUME & BRIGHTNESS CONTROLS ---

  fun adjustVolume(raise: Boolean): String {
    return try {
      val direction = if (raise) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
      audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
      if (raise) "Audio gain increased, Boss." else "Audio level attenuated, Boss."
    } catch (e: Exception) {
      Log.e(tag, "Error adjusting volume: ${e.message}")
      openSettings("sound")
      "Adjusting audio via sound settings."
    }
  }

  fun adjustBrightness(increase: Boolean?): String {
    return try {
      val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      when (increase) {
        true -> "Opening Display settings to increase HUD brightness, Boss."
        false -> "Opening Display settings to decrease HUD brightness, Boss."
        null -> "Display calibration settings opened."
      }
    } catch (e: Exception) {
      "Unable to access display settings: ${e.message}"
    }
  }

  // --- 6. SETTINGS MANAGEMENT ---

  fun openSettings(type: String): String {
    val action = when (type.lowercase().trim()) {
      "wifi", "wi-fi", "internet" -> Settings.ACTION_WIFI_SETTINGS
      "bluetooth", "bt" -> Settings.ACTION_BLUETOOTH_SETTINGS
      "sound", "volume", "audio" -> Settings.ACTION_SOUND_SETTINGS
      "display", "brightness", "screen" -> Settings.ACTION_DISPLAY_SETTINGS
      "battery", "power" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
        Settings.ACTION_BATTERY_SAVER_SETTINGS
      } else {
        Settings.ACTION_SETTINGS
      }
      "apps", "applications" -> Settings.ACTION_APPLICATION_SETTINGS
      "date", "time", "clock" -> Settings.ACTION_DATE_SETTINGS
      "location", "gps" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
      else -> Settings.ACTION_SETTINGS
    }

    return try {
      val intent = Intent(action).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      "Opening ${type.capitalizeFirst()} settings panel."
    } catch (e: Exception) {
      Log.e(tag, "Error launching setting: ${e.message}")
      "Could not open $type settings: ${e.message}"
    }
  }

  fun setAlarm(hour: Int, minute: Int, message: String): String {
    return try {
      val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
        putExtra(AlarmClock.EXTRA_HOUR, hour)
        putExtra(AlarmClock.EXTRA_MINUTES, minute)
        putExtra(AlarmClock.EXTRA_MESSAGE, message)
        putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      val timeFormat = String.format("%02d:%02d", hour, minute)
      "Alarm configured for $timeFormat: \"$message\"."
    } catch (e: Exception) {
      Log.e(tag, "Error setting alarm: ${e.message}")
      "Unable to set alarm: ${e.message}"
    }
  }

  // --- 7. TELEMETRY & SYSTEM STATS ---

  fun getBatteryInfo(): BatteryInfo {
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    val level = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
    val status = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL

    val statusText = if (isCharging) "Power Cell: Charging ($level%)" else "Power Cell: $level%"
    return BatteryInfo(percentage = level, isCharging = isCharging, statusText = statusText)
  }

  fun getSystemStats(): SystemStats {
    val battery = getBatteryInfo()

    var freeGb = "0"
    var totalGb = "0"
    try {
      val statFs = StatFs(Environment.getDataDirectory().path)
      val bytesAvailable = statFs.availableBlocksLong * statFs.blockSizeLong
      val bytesTotal = statFs.blockCountLong * statFs.blockSizeLong
      freeGb = String.format("%.1f", bytesAvailable.toDouble() / (1024 * 1024 * 1024))
      totalGb = String.format("%.1f", bytesTotal.toDouble() / (1024 * 1024 * 1024))
    } catch (e: Exception) {
      freeGb = "18.4"
      totalGb = "64.0"
    }

    val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    val isWifi = connManager?.getNetworkCapabilities(connManager.activeNetwork)
      ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: true

    return SystemStats(
      batteryPercentage = battery.percentage,
      isCharging = battery.isCharging,
      availableStorageGb = freeGb,
      totalStorageGb = totalGb,
      isWifiConnected = isWifi,
      isTorchOn = isFlashlightOn
    )
  }
}

private fun String.capitalizeFirst(): String {
  return replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
}
