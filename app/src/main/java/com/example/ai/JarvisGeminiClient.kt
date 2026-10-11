package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.ScheduleCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class JarvisGeminiClient {
  private val tag = "JarvisGeminiClient"

  private val client = OkHttpClient.Builder()
    .connectTimeout(60, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .build()

  private val systemInstruction = """
    You are J.A.R.V.I.S., the ultimate AI personal assistant engineered for the user's Android phone.
    You address the user with respect like Tony Stark's assistant: "Boss" or "Sir".
    You understand English, Hindi, and Hinglish natively (e.g., "Rohit ko call karo", "YouTube kholo", "Mom ko WhatsApp pe msg bhejo").
    
    When the user requests an action, analyze their request and output your reply strictly in JSON format with two keys:
    1. "action": An object describing the action, with field "type":
       - "call": { "type": "call", "target": "Name or phone number" }
       - "message": { "type": "message", "target": "Name or phone number", "text": "message body", "isWhatsApp": boolean }
       - "open_app": { "type": "open_app", "appName": "YouTube / Camera / Settings / etc." }
       - "torch": { "type": "torch", "enable": true/false/null }
       - "settings": { "type": "settings", "settingType": "wifi" / "bluetooth" / "sound" / "display" / "battery" / "general" }
       - "schedule": { "type": "schedule", "title": "meeting title", "time": "10:30 AM", "date": "today/tomorrow/YYYY-MM-DD", "category": "WORK"/"CALL"/"REMINDER"/"PERSONAL" }
       - "alarm": { "type": "alarm", "hour": 7, "minute": 30, "label": "Morning alarm" }
       - "status": { "type": "status" }
       - "chat": { "type": "chat" }
    2. "speech": The concise, stylish Jarvis voice response to speak back to the user (in the same language the user spoke, e.g. Hinglish/Hindi/English). Keep speech under 2 sentences.
    
    Example output JSON:
    {
      "action": { "type": "call", "target": "Rohit" },
      "speech": "Connecting you to Rohit immediately, Boss."
    }
  """.trimIndent()

  suspend fun askGemini(prompt: String): JarvisCommandResult? = withContext(Dispatchers.IO) {
    val apiKey = try {
      BuildConfig.GEMINI_API_KEY
    } catch (e: Exception) {
      ""
    }

    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
      Log.d(tag, "Gemini API key is blank or placeholder, skipping REST call.")
      return@withContext null
    }

    try {
      val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

      val requestBodyJson = JSONObject().apply {
        put("contents", JSONArray().apply {
          put(JSONObject().apply {
            put("parts", JSONArray().apply {
              put(JSONObject().apply {
                put("text", prompt)
              })
            })
          })
        })
        put("systemInstruction", JSONObject().apply {
          put("parts", JSONArray().apply {
            put(JSONObject().apply {
              put("text", systemInstruction)
            })
          })
        })
        put("generationConfig", JSONObject().apply {
          put("temperature", 0.7)
          put("responseMimeType", "application/json")
        })
      }

      val request = Request.Builder()
        .url(url)
        .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
        .build()

      val response = client.newCall(request).execute()
      val responseString = response.body?.string() ?: return@withContext null

      if (!response.isSuccessful) {
        Log.e(tag, "Gemini API failed code: ${response.code}, body: $responseString")
        return@withContext null
      }

      val rootJson = JSONObject(responseString)
      val candidates = rootJson.optJSONArray("candidates") ?: return@withContext null
      val firstCandidate = candidates.optJSONObject(0) ?: return@withContext null
      val content = firstCandidate.optJSONObject("content") ?: return@withContext null
      val parts = content.optJSONArray("parts") ?: return@withContext null
      val rawText = parts.optJSONObject(0)?.optString("text") ?: return@withContext null

      parseGeminiResponse(rawText)
    } catch (e: Exception) {
      Log.e(tag, "Gemini API call failed: ${e.message}")
      null
    }
  }

  private fun parseGeminiResponse(rawJsonText: String): JarvisCommandResult? {
    return try {
      val cleanJson = rawJsonText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
      val json = JSONObject(cleanJson)
      val speech = json.optString("speech", "At your service, Boss.")
      val actionObj = json.optJSONObject("action")

      val action: JarvisAction = if (actionObj == null) {
        JarvisAction.ConversationalAction(speech)
      } else {
        when (actionObj.optString("type")) {
          "call" -> JarvisAction.CallAction(actionObj.optString("target", ""))
          "message" -> JarvisAction.MessageAction(
            target = actionObj.optString("target", ""),
            message = actionObj.optString("text", "Hello"),
            isWhatsApp = actionObj.optBoolean("isWhatsApp", false)
          )
          "open_app" -> JarvisAction.OpenAppAction(actionObj.optString("appName", ""))
          "torch" -> {
            val enable = if (actionObj.has("enable") && !actionObj.isNull("enable")) {
              actionObj.optBoolean("enable")
            } else null
            JarvisAction.TorchAction(enable)
          }
          "settings" -> JarvisAction.SettingAction(actionObj.optString("settingType", "general"))
          "schedule" -> JarvisAction.ScheduleAction(
            title = actionObj.optString("title", "New Task"),
            time = actionObj.optString("time", "12:00 PM"),
            date = actionObj.optString("date", "today"),
            category = ScheduleCategory.valueOf(actionObj.optString("category", ScheduleCategory.REMINDER.name))
          )
          "alarm" -> JarvisAction.AlarmAction(
            hour = actionObj.optInt("hour", 8),
            minute = actionObj.optInt("minute", 0),
            label = actionObj.optString("label", "Jarvis Alarm")
          )
          "status" -> JarvisAction.SystemStatusAction
          else -> JarvisAction.ConversationalAction(speech)
        }
      }

      JarvisCommandResult(
        action = action,
        speechResponse = speech,
        executionLog = "[GEMINI COGNITION] Parsed intent: ${action::class.simpleName}"
      )
    } catch (e: Exception) {
      Log.e(tag, "Failed to parse Gemini JSON: ${e.message}")
      null
    }
  }
}
