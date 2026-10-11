package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SpeechToTextManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _currentInterim = MutableStateFlow("")
    val currentInterim: StateFlow<String> = _currentInterim.asStateFlow()

    var onFinalTranscription: ((String) -> Unit)? = null
    var onErrorOccurred: ((String) -> Unit)? = null

    private val timeoutRunnable = Runnable {
        if (_isListening.value) {
            stopListening()
            onErrorOccurred?.invoke("Tiempo de espera agotado. Puedes reintentar o usar entrada rápida.")
        }
    }

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(languageCode: String) {
        mainHandler.post {
            // Clean up any existing recognizer first to prevent System resource leak
            cleanupRecognizer()

            if (!isAvailable()) {
                _isListening.value = false
                onErrorOccurred?.invoke("El servicio de voz no está habilitado en este emulador.")
                return@post
            }

            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _isListening.value = true
                            _currentInterim.value = ""
                            // Schedule 8-second safety timeout
                            mainHandler.removeCallbacks(timeoutRunnable)
                            mainHandler.postDelayed(timeoutRunnable, 8000)
                        }

                        override fun onBeginningOfSpeech() {
                            _isListening.value = true
                            mainHandler.removeCallbacks(timeoutRunnable)
                            mainHandler.postDelayed(timeoutRunnable, 6000)
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            _rmsLevel.value = rmsdB.coerceIn(0f, 10f)
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            _isListening.value = false
                            _rmsLevel.value = 0f
                            mainHandler.removeCallbacks(timeoutRunnable)
                        }

                        override fun onError(error: Int) {
                            mainHandler.removeCallbacks(timeoutRunnable)
                            _isListening.value = false
                            _rmsLevel.value = 0f
                            cleanupRecognizer()

                            val message = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> "No se detectó voz clara. Prueba hablando más cerca o usa entrada rápida."
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tiempo agotado sin detectar audio."
                                SpeechRecognizer.ERROR_NETWORK -> "Error de conexión en reconocimiento de voz."
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permiso de micrófono no otorgado."
                                SpeechRecognizer.ERROR_CLIENT -> "El servicio de audio del sistema se reinició."
                                else -> "Reconocimiento finalizado (código $error)."
                            }
                            onErrorOccurred?.invoke(message)
                        }

                        override fun onResults(results: Bundle?) {
                            mainHandler.removeCallbacks(timeoutRunnable)
                            _isListening.value = false
                            _rmsLevel.value = 0f
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull()?.trim() ?: ""
                            cleanupRecognizer()
                            if (text.isNotEmpty()) {
                                _currentInterim.value = text
                                onFinalTranscription?.invoke(text)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull()?.trim() ?: ""
                            if (text.isNotEmpty()) {
                                _currentInterim.value = text
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

                speechRecognizer?.startListening(intent)
                _isListening.value = true
                mainHandler.postDelayed(timeoutRunnable, 8000)
            } catch (e: Exception) {
                _isListening.value = false
                cleanupRecognizer()
                onErrorOccurred?.invoke(e.localizedMessage ?: "No se pudo iniciar el micrófono")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            mainHandler.removeCallbacks(timeoutRunnable)
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {}
            _isListening.value = false
            _rmsLevel.value = 0f
            cleanupRecognizer()
        }
    }

    private fun cleanupRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    fun destroy() {
        mainHandler.removeCallbacks(timeoutRunnable)
        cleanupRecognizer()
        _isListening.value = false
        _rmsLevel.value = 0f
    }
}
