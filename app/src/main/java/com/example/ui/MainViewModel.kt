package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.LanguageItem
import com.example.data.TranslationEntity
import com.example.data.UserProfile
import com.example.safety.SafetyManager
import com.example.speech.SpeechToTextManager
import com.example.speech.TextToSpeechManager
import com.example.translation.TranslationService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppNavTab {
    CONVERSATION, // Contains both Voice Mode and Text Chat mode with seamless switcher
    PAIRING,
    HISTORY,
    PROFILE
}

enum class ConversationMode {
    VOICE_MODE,
    TEXT_CHAT
}

enum class PairingStatus {
    UNPAIRED,
    SEARCHING,
    PAIRED
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val translationDao = db.translationDao()
    private val translationService = TranslationService()

    val ttsManager = TextToSpeechManager(application)
    val sttManager = SpeechToTextManager(application)

    // Current logged in user (defaults to User #2 - Diego Español)
    private val _currentUser = MutableStateFlow(UserProfile.PRESET_USERS[0])
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(true)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // Paired partner (e.g. User #8 - Hans Alemán)
    private val _pairedPartner = MutableStateFlow<UserProfile?>(UserProfile.PRESET_USERS[1])
    val pairedPartner: StateFlow<UserProfile?> = _pairedPartner.asStateFlow()

    private val _pairingStatus = MutableStateFlow(PairingStatus.PAIRED)
    val pairingStatus: StateFlow<PairingStatus> = _pairingStatus.asStateFlow()

    private val _activeTab = MutableStateFlow(AppNavTab.CONVERSATION)
    val activeTab: StateFlow<AppNavTab> = _activeTab.asStateFlow()

    // Conversation Mode: Voice Mode vs Text Chat mode
    private val _conversationMode = MutableStateFlow(ConversationMode.VOICE_MODE)
    val conversationMode: StateFlow<ConversationMode> = _conversationMode.asStateFlow()

    // Language configuration
    private val _sourceLanguage = MutableStateFlow(LanguageItem.findByCode("es"))
    val sourceLanguage: StateFlow<LanguageItem> = _sourceLanguage.asStateFlow()

    private val _targetLanguage = MutableStateFlow(LanguageItem.findByCode("de"))
    val targetLanguage: StateFlow<LanguageItem> = _targetLanguage.asStateFlow()

    // Real-time Voice Mode: Dual interlocutor live states
    // Interlocutor 1 (Local / Current User)
    private val _speaker1Transcription = MutableStateFlow("Toca el micrófono para hablar en tu idioma...")
    val speaker1Transcription: StateFlow<String> = _speaker1Transcription.asStateFlow()

    private val _speaker1Translation = MutableStateFlow("La traducción para tu compañero aparecerá aquí...")
    val speaker1Translation: StateFlow<String> = _speaker1Translation.asStateFlow()

    // Interlocutor 2 (Paired Partner)
    private val _speaker2Transcription = MutableStateFlow("Esperando intervención de tu compañero...")
    val speaker2Transcription: StateFlow<String> = _speaker2Transcription.asStateFlow()

    private val _speaker2Translation = MutableStateFlow("La traducción a tu idioma natal aparecerá aquí...")
    val speaker2Translation: StateFlow<String> = _speaker2Translation.asStateFlow()

    private val _isPartnerSpeaking = MutableStateFlow(false)
    val isPartnerSpeaking: StateFlow<Boolean> = _isPartnerSpeaking.asStateFlow()

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()

    private val _autoSpeak = MutableStateFlow(true)
    val autoSpeak: StateFlow<Boolean> = _autoSpeak.asStateFlow()

    private val _safetyNotification = MutableStateFlow<String?>(null)
    val safetyNotification: StateFlow<String?> = _safetyNotification.asStateFlow()

    // Text Chat input
    private val _chatInput = MutableStateFlow("")
    val chatInput: StateFlow<String> = _chatInput.asStateFlow()

    // Voice note recording in chat
    private val _isRecordingVoiceNote = MutableStateFlow(false)
    val isRecordingVoiceNote: StateFlow<Boolean> = _isRecordingVoiceNote.asStateFlow()

    private val _voiceNoteDuration = MutableStateFlow(0)
    val voiceNoteDuration: StateFlow<Int> = _voiceNoteDuration.asStateFlow()

    private var voiceTimerJob: Job? = null

    // Search query for history
    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    // Database Flows
    val chatMessages: StateFlow<List<TranslationEntity>> = combine(
        translationDao.getAllTranslations(),
        _currentUser,
        _pairedPartner
    ) { all, user, partner ->
        if (partner == null) emptyList()
        else {
            all.filter {
                (it.senderId == user.id && it.receiverId == partner.id) ||
                (it.senderId == partner.id && it.receiverId == user.id)
            }.sortedBy { it.timestamp }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val historyTranslations: StateFlow<List<TranslationEntity>> = combine(
        translationDao.getAllTranslations(),
        _historySearchQuery
    ) { list, query ->
        if (query.isBlank()) list
        else list.filter {
            it.originalText.contains(query, ignoreCase = true) ||
            it.translatedText.contains(query, ignoreCase = true) ||
            it.senderAlias.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        setupSpeechListeners()
        syncLanguagesWithPairing()
        viewModelScope.launch {
            insertInitialWelcomeMessages()
        }
    }

    private fun syncLanguagesWithPairing() {
        val user = _currentUser.value
        val partner = _pairedPartner.value
        _sourceLanguage.value = user.nativeLanguage
        if (partner != null) {
            _targetLanguage.value = partner.nativeLanguage
        }
    }

    private suspend fun insertInitialWelcomeMessages() {
        val user = _currentUser.value
        val partner = _pairedPartner.value ?: return
        val currentMsgs = chatMessages.value
        if (currentMsgs.isEmpty()) {
            translationDao.insert(
                TranslationEntity(
                    senderId = partner.id,
                    senderAlias = partner.alias,
                    receiverId = user.id,
                    receiverAlias = user.alias,
                    sourceLanguageCode = partner.nativeLanguageCode,
                    targetLanguageCode = user.nativeLanguageCode,
                    originalText = "Hallo Diego! Ich bin Hans. Willkommen bei VozLink.",
                    translatedText = "¡Hola Diego! Soy Hans. Bienvenido a VozLink.",
                    isVoiceNote = false,
                    durationSeconds = 0,
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 5
                )
            )
        }
    }

    private fun setupSpeechListeners() {
        sttManager.onFinalTranscription = { text ->
            viewModelScope.launch {
                handleLocalSpeakerSpeech(text)
            }
        }

        sttManager.onErrorOccurred = { errorMsg ->
            _speaker1Transcription.value = errorMsg
        }
    }

    fun setActiveTab(tab: AppNavTab) {
        _activeTab.value = tab
    }

    fun setConversationMode(mode: ConversationMode) {
        _conversationMode.value = mode
    }

    fun setSourceLanguage(lang: LanguageItem) {
        _sourceLanguage.value = lang
    }

    fun setTargetLanguage(lang: LanguageItem) {
        _targetLanguage.value = lang
    }

    fun toggleAutoSpeak() {
        _autoSpeak.value = !_autoSpeak.value
    }

    fun setChatInput(text: String) {
        _chatInput.value = text
    }

    fun setHistorySearchQuery(query: String) {
        _historySearchQuery.value = query
    }

    fun dismissSafetyNotification() {
        _safetyNotification.value = null
    }

    // Update Profile Function
    fun updateProfile(
        alias: String,
        bio: String,
        languageCode: String,
        photoUri: String?,
        avatarEmoji: String
    ) {
        val updated = _currentUser.value.copy(
            alias = alias.trim().ifEmpty { _currentUser.value.alias },
            bio = bio.trim(),
            nativeLanguageCode = languageCode,
            photoUri = photoUri,
            avatarEmoji = avatarEmoji
        )
        _currentUser.value = updated
        syncLanguagesWithPairing()
        _safetyNotification.value = "Perfil actualizado correctamente."
    }

    // Voice Mode: Speaker 1 (User / Local) Voice Recognition
    fun startLocalSpeakerListening() {
        if (_pairingStatus.value != PairingStatus.PAIRED) {
            _speaker1Transcription.value = "Debes emparejarte con un usuario antes de iniciar el modo de voz."
            return
        }
        _speaker1Transcription.value = "Escuchando en vivo (${_sourceLanguage.value.displayName})..."
        sttManager.startListening(_sourceLanguage.value.localeTag)
    }

    fun stopLocalSpeakerListening() {
        sttManager.stopListening()
    }

    fun processLocalSpeakerText(text: String) {
        if (text.isBlank()) return
        sttManager.stopListening()
        viewModelScope.launch {
            handleLocalSpeakerSpeech(text)
        }
    }

    private suspend fun handleLocalSpeakerSpeech(rawText: String) {
        val (safeText, hadViolation) = SafetyManager.sanitizeText(rawText)
        if (hadViolation) {
            _safetyNotification.value = "Transcripción filtrada por normas de convivencia segura."
        }

        _speaker1Transcription.value = safeText
        _isTranslating.value = true

        val result = translationService.translate(
            text = safeText,
            sourceLangCode = _sourceLanguage.value.code,
            targetLangCode = _targetLanguage.value.code
        )

        _isTranslating.value = false
        val translated = result.getOrDefault(safeText)
        _speaker1Translation.value = translated

        // Auto speak translation to partner in their target language
        if (_autoSpeak.value && translated.isNotBlank()) {
            ttsManager.speak(translated, _targetLanguage.value.code)
        }

        // Save translation to Database
        val partner = _pairedPartner.value
        val user = _currentUser.value
        if (partner != null) {
            translationDao.insert(
                TranslationEntity(
                    senderId = user.id,
                    senderAlias = user.alias,
                    receiverId = partner.id,
                    receiverAlias = partner.alias,
                    sourceLanguageCode = _sourceLanguage.value.code,
                    targetLanguageCode = _targetLanguage.value.code,
                    originalText = safeText,
                    translatedText = translated,
                    isVoiceNote = false,
                    durationSeconds = 0,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    // Voice Mode: Interlocutor 2 (Paired Partner) Live Speaking
    fun simulatePartnerVoiceSpeech(customTextInPartnerLang: String? = null) {
        val partner = _pairedPartner.value ?: return
        val user = _currentUser.value

        val samplePhrases = when (partner.nativeLanguageCode) {
            "de" -> listOf(
                "Hallo! Ich kann dich sehr deutlich hören.",
                "Wie läuft dein Arbeitstag heute?",
                "Das ist wirklich fantastisch, wir verstehen uns perfekt!",
                "Können wir das Projekt morgen um 10 Uhr besprechen?",
                "Die Echtzeit-Übersetzung funktioniert wunderbar."
            )
            "fr" -> listOf(
                "Bonjour ! Je t'entends très bien.",
                "Comment se passe ta journée ?",
                "C'est magnifique de converser ainsi en temps réel !",
                "À quelle heure nous retrouvons-nous demain ?"
            )
            "ja" -> listOf(
                "こんにちは！とてもクリアに聞こえていますよ。",
                "今日の調子はいかがですか？",
                "リアルタイムで翻訳されて素晴らしいですね。"
            )
            else -> listOf(
                "Hello! I can hear you very clearly.",
                "How is your project going today?",
                "This real-time voice translation is incredibly smooth!",
                "Should we schedule our call for tomorrow morning?"
            )
        }

        val originalPartnerSpeech = customTextInPartnerLang ?: samplePhrases.random()
        _isPartnerSpeaking.value = true
        _speaker2Transcription.value = originalPartnerSpeech

        viewModelScope.launch {
            _isTranslating.value = true
            val transRes = translationService.translate(
                text = originalPartnerSpeech,
                sourceLangCode = partner.nativeLanguageCode,
                targetLangCode = user.nativeLanguageCode
            )
            _isTranslating.value = false
            val translatedToUser = transRes.getOrDefault(originalPartnerSpeech)
            _speaker2Translation.value = translatedToUser

            // Play voice in user's native language so user immediately hears partner's translated voice!
            if (_autoSpeak.value) {
                ttsManager.speak(translatedToUser, user.nativeLanguageCode)
            }

            _isPartnerSpeaking.value = false

            // Save to database
            translationDao.insert(
                TranslationEntity(
                    senderId = partner.id,
                    senderAlias = partner.alias,
                    receiverId = user.id,
                    receiverAlias = user.alias,
                    sourceLanguageCode = partner.nativeLanguageCode,
                    targetLanguageCode = user.nativeLanguageCode,
                    originalText = originalPartnerSpeech,
                    translatedText = translatedToUser,
                    isVoiceNote = false,
                    durationSeconds = 0,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    // Text Chat Message Sending
    fun sendChatMessage() {
        val text = _chatInput.value.trim()
        if (text.isEmpty() || _pairingStatus.value != PairingStatus.PAIRED) return

        val partner = _pairedPartner.value ?: return
        val user = _currentUser.value

        val (safeText, hadViolation) = SafetyManager.sanitizeText(text)
        if (hadViolation) {
            _safetyNotification.value = "Texto filtrado para mantener un chat seguro y respetuoso."
        }
        _chatInput.value = ""

        viewModelScope.launch {
            _isTranslating.value = true
            val translationRes = translationService.translate(
                text = safeText,
                sourceLangCode = user.nativeLanguageCode,
                targetLangCode = partner.nativeLanguageCode
            )
            _isTranslating.value = false
            val translated = translationRes.getOrDefault(safeText)

            val entity = TranslationEntity(
                senderId = user.id,
                senderAlias = user.alias,
                receiverId = partner.id,
                receiverAlias = partner.alias,
                sourceLanguageCode = user.nativeLanguageCode,
                targetLanguageCode = partner.nativeLanguageCode,
                originalText = safeText,
                translatedText = translated,
                isVoiceNote = false,
                durationSeconds = 0,
                timestamp = System.currentTimeMillis()
            )
            translationDao.insert(entity)

            if (_autoSpeak.value) {
                ttsManager.speak(translated, partner.nativeLanguageCode)
            }
        }
    }

    // Voice Note in Chat (Record & Transcribe automatically)
    fun startVoiceNoteRecording() {
        if (_pairingStatus.value != PairingStatus.PAIRED) return
        _isRecordingVoiceNote.value = true
        _voiceNoteDuration.value = 0

        voiceTimerJob?.cancel()
        voiceTimerJob = viewModelScope.launch {
            while (_isRecordingVoiceNote.value) {
                delay(1000)
                _voiceNoteDuration.value += 1
            }
        }
        sttManager.startListening(_sourceLanguage.value.localeTag)
    }

    fun stopVoiceNoteRecording() {
        if (!_isRecordingVoiceNote.value) return
        val duration = _voiceNoteDuration.value
        _isRecordingVoiceNote.value = false
        voiceTimerJob?.cancel()
        sttManager.stopListening()

        viewModelScope.launch {
            delay(400)
            val recognized = sttManager.currentInterim.value.ifBlank {
                "Nota de voz grabada (${duration}s)"
            }
            sendVoiceNoteMessage(recognized, duration)
        }
    }

    private suspend fun sendVoiceNoteMessage(originalText: String, durationSec: Int) {
        val partner = _pairedPartner.value ?: return
        val user = _currentUser.value

        val (safeText, hadViolation) = SafetyManager.sanitizeText(originalText)
        if (hadViolation) {
            _safetyNotification.value = "Transcripción filtrada por seguridad."
        }

        val translationRes = translationService.translate(
            text = safeText,
            sourceLangCode = user.nativeLanguageCode,
            targetLangCode = partner.nativeLanguageCode
        )
        val translated = translationRes.getOrDefault(safeText)

        translationDao.insert(
            TranslationEntity(
                senderId = user.id,
                senderAlias = user.alias,
                receiverId = partner.id,
                receiverAlias = partner.alias,
                sourceLanguageCode = user.nativeLanguageCode,
                targetLanguageCode = partner.nativeLanguageCode,
                originalText = safeText,
                translatedText = translated,
                isVoiceNote = true,
                durationSeconds = durationSec.coerceAtLeast(1),
                timestamp = System.currentTimeMillis()
            )
        )

        if (_autoSpeak.value) {
            ttsManager.speak(translated, partner.nativeLanguageCode)
        }
    }

    fun speakCustom(text: String, langCode: String) {
        ttsManager.speak(text, langCode)
    }

    // Pairing controls
    fun pairWithUser(partnerIdOrCode: String) {
        val clean = partnerIdOrCode.trim()
        if (clean.isBlank()) return

        val matched = UserProfile.PRESET_USERS.firstOrNull { it.id == clean || it.alias.equals(clean, ignoreCase = true) }
            ?: UserProfile(
                id = clean,
                alias = "Usuario #$clean",
                email = "user$clean@vozlink.app",
                nativeLanguageCode = if (_currentUser.value.nativeLanguageCode == "es") "de" else "es",
                avatarEmoji = "🤝",
                bio = "Usuario emparejado en VozLink."
            )

        if (matched.id == _currentUser.value.id) {
            _safetyNotification.value = "No puedes emparejarte contigo mismo. Elige otro usuario."
            return
        }

        if (SafetyManager.isUserBlocked(matched.id)) {
            _safetyNotification.value = "Este usuario se encuentra bloqueado por motivos de seguridad."
            return
        }

        _pairedPartner.value = matched
        _pairingStatus.value = PairingStatus.PAIRED
        syncLanguagesWithPairing()
        _safetyNotification.value = "¡Emparejado con éxito con ${matched.alias} (ID #${matched.id})!"
    }

    fun unpair() {
        _pairedPartner.value = null
        _pairingStatus.value = PairingStatus.UNPAIRED
        _speaker1Transcription.value = "Emparejamiento finalizado. Conéctate con un usuario para traducir."
        _speaker1Translation.value = ""
        _speaker2Transcription.value = "Sin conexión activa."
        _speaker2Translation.value = ""
        ttsManager.stop()
        sttManager.stopListening()
    }

    fun blockAndReportCurrentPartner() {
        val partner = _pairedPartner.value
        if (partner != null) {
            SafetyManager.blockUser(partner.id)
            unpair()
            _safetyNotification.value = "Usuario bloqueado y reporte de seguridad generado."
        }
    }

    fun switchUser(user: UserProfile) {
        _currentUser.value = user
        if (_pairedPartner.value?.id == user.id) {
            _pairedPartner.value = UserProfile.PRESET_USERS.firstOrNull { it.id != user.id }
        }
        syncLanguagesWithPairing()
    }

    fun login(alias: String, pass: String) {
        val found = UserProfile.PRESET_USERS.firstOrNull { it.alias.equals(alias.trim(), ignoreCase = true) }
            ?: UserProfile(
                id = (10..99).random().toString(),
                alias = alias.trim(),
                email = "${alias.trim().lowercase()}@vozlink.app",
                nativeLanguageCode = "es",
                avatarEmoji = "👤",
                bio = "Usuario de VozLink."
            )
        _currentUser.value = found
        _isLoggedIn.value = true
        syncLanguagesWithPairing()
    }

    fun register(alias: String, email: String, langCode: String, bio: String) {
        val newUser = UserProfile(
            id = (20..99).random().toString(),
            alias = alias.trim(),
            email = email.trim(),
            nativeLanguageCode = langCode,
            avatarEmoji = "✨",
            bio = bio.ifBlank { "Nuevo usuario en VozLink." }
        )
        _currentUser.value = newUser
        _isLoggedIn.value = true
        syncLanguagesWithPairing()
    }

    fun logout() {
        _isLoggedIn.value = false
        unpair()
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            translationDao.deleteById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            translationDao.clearAll()
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
        sttManager.destroy()
    }
}
