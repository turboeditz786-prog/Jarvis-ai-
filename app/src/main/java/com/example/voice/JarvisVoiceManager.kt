package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class AssistantListeningState {
  IDLE,
  LISTENING,
  PROCESSING,
  SPEAKING,
  ERROR
}

class JarvisVoiceManager(
  private val context: Context,
  private val onSpeechRecognized: (String) -> Unit
) : TextToSpeech.OnInitListener {

  private val tag = "JarvisVoiceManager"
  private var speechRecognizer: SpeechRecognizer? = null
  private var tts: TextToSpeech? = null

  private val _state = MutableStateFlow(AssistantListeningState.IDLE)
  val state: StateFlow<AssistantListeningState> = _state.asStateFlow()

  private val _rmsLevel = MutableStateFlow(0f)
  val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

  private val _partialTranscript = MutableStateFlow("")
  val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

  var pitch: Float = 0.95f
    set(value) {
      field = value
      tts?.setPitch(value)
    }

  var speechRate: Float = 1.05f
    set(value) {
      field = value
      tts?.setSpeechRate(value)
    }

  init {
    initTts()
    initSpeechRecognizer()
  }

  private fun initTts() {
    tts = TextToSpeech(context, this)
  }

  override fun onInit(status: Int) {
    if (status == TextToSpeech.SUCCESS) {
      val result = tts?.setLanguage(Locale.ENGLISH)
      if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
        tts?.setLanguage(Locale.getDefault())
      }
      tts?.setPitch(pitch)
      tts?.setSpeechRate(speechRate)
      tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
          _state.value = AssistantListeningState.SPEAKING
        }

        override fun onDone(utteranceId: String?) {
          _state.value = AssistantListeningState.IDLE
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
          _state.value = AssistantListeningState.IDLE
        }
      })
    }
  }

  private fun initSpeechRecognizer() {
    if (SpeechRecognizer.isRecognitionAvailable(context)) {
      try {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
          setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
              _state.value = AssistantListeningState.LISTENING
              _partialTranscript.value = ""
            }

            override fun onBeginningOfSpeech() {
              _state.value = AssistantListeningState.LISTENING
            }

            override fun onRmsChanged(rmsdB: Float) {
              // Normalize RMS for UI visualization (0.0 to 1.0)
              val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
              _rmsLevel.value = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
              _state.value = AssistantListeningState.PROCESSING
              _rmsLevel.value = 0f
            }

            override fun onError(error: Int) {
              _state.value = AssistantListeningState.IDLE
              _rmsLevel.value = 0f
              Log.e(tag, "Speech recognition error code: $error")
            }

            override fun onResults(results: Bundle?) {
              _state.value = AssistantListeningState.PROCESSING
              val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
              val text = matches?.firstOrNull() ?: ""
              _partialTranscript.value = text
              if (text.isNotBlank()) {
                onSpeechRecognized(text)
              } else {
                _state.value = AssistantListeningState.IDLE
              }
            }

            override fun onPartialResults(partialResults: Bundle?) {
              val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
              val text = matches?.firstOrNull() ?: ""
              if (text.isNotBlank()) {
                _partialTranscript.value = text
              }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
          })
        }
      } catch (e: Exception) {
        Log.e(tag, "Failed to initialize SpeechRecognizer: ${e.message}")
      }
    }
  }

  fun startListening() {
    // If speaking, stop TTS first
    stopSpeaking()

    if (speechRecognizer == null) {
      initSpeechRecognizer()
    }

    try {
      val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
      }
      speechRecognizer?.startListening(intent)
      _state.value = AssistantListeningState.LISTENING
    } catch (e: Exception) {
      Log.e(tag, "startListening error: ${e.message}")
      _state.value = AssistantListeningState.IDLE
    }
  }

  fun stopListening() {
    try {
      speechRecognizer?.stopListening()
    } catch (e: Exception) {
      Log.e(tag, "stopListening error: ${e.message}")
    }
    _state.value = AssistantListeningState.IDLE
  }

  fun speak(text: String) {
    if (text.isBlank()) return
    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_response_${System.currentTimeMillis()}")
  }

  fun stopSpeaking() {
    tts?.stop()
    if (_state.value == AssistantListeningState.SPEAKING) {
      _state.value = AssistantListeningState.IDLE
    }
  }

  fun destroy() {
    try {
      speechRecognizer?.destroy()
      tts?.stop()
      tts?.shutdown()
    } catch (e: Exception) {
      Log.e(tag, "Destroy error: ${e.message}")
    }
  }
}
