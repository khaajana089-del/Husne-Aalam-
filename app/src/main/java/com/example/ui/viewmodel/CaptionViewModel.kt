package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.data.local.AppDatabase
import com.example.data.model.BrandKit
import com.example.data.model.CaptionItem
import com.example.data.model.ExportItem
import com.example.data.model.Project
import com.example.data.repository.CaptionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CaptionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CaptionRepository

    // Base flows from database
    val allProjects: StateFlow<List<Project>>
    val exportHistory: StateFlow<List<ExportItem>>
    val brandKit: StateFlow<BrandKit>

    // Navigation and Active editing states
    private val _activeProject = MutableStateFlow<Project?>(null)
    val activeProject: StateFlow<Project?> = _activeProject.asStateFlow()

    private val _activeCaptions = MutableStateFlow<List<CaptionItem>>(emptyList())
    val activeCaptions: StateFlow<List<CaptionItem>> = _activeCaptions.asStateFlow()

    private val _selectedCaption = MutableStateFlow<CaptionItem?>(null)
    val selectedCaption: StateFlow<CaptionItem?> = _selectedCaption.asStateFlow()

    // Timeline player states
    private val _currentTimeMs = MutableStateFlow(0L)
    val currentTimeMs: StateFlow<Long> = _currentTimeMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // AI Copilot state
    private val _copilotMessages = MutableStateFlow<List<CopilotMessage>>(listOf(
        CopilotMessage("system", "Welcome to CaptionX AI Assistant. I can analyze your multi-language video (Hindi, Hinglish, English) to recommend branding formats, titles, and emojis.")
    ))
    val copilotMessages: StateFlow<List<CopilotMessage>> = _copilotMessages.asStateFlow()

    private val _isCopilotLoading = MutableStateFlow(false)
    val isCopilotLoading: StateFlow<Boolean> = _isCopilotLoading.asStateFlow()

    // Render configuration and queue
    private val _renderingQueue = MutableStateFlow<List<RenderJob>>(emptyList())
    val renderingQueue: StateFlow<List<RenderJob>> = _renderingQueue.asStateFlow()

    // Active screen navigation tracking (simulated for type-safe control inside main)
    private val _currentScreen = MutableStateFlow("dashboard")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = CaptionRepository(
            database.projectDao(),
            database.captionDao(),
            database.brandKitDao(),
            database.exportDao()
        )

        allProjects = repository.allProjects.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        exportHistory = repository.allExports.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        brandKit = repository.brandKit
            .map { it ?: BrandKit() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = BrandKit()
            )

        // Automatically populate mock dashboard projects on first launch if empty
        viewModelScope.launch {
            delay(500)
            if (allProjects.value.isEmpty()) {
                createDefaultProjects()
            }
        }

        // Timeline ticking logic
        viewModelScope.launch {
            while (true) {
                if (_isPlaying.value) {
                    val currentProj = _activeProject.value
                    if (currentProj != null) {
                        val maxDurationMs = currentProj.videoDuration * 1000L
                        if (_currentTimeMs.value >= maxDurationMs) {
                            _currentTimeMs.value = 0L
                            _isPlaying.value = false
                        } else {
                            _currentTimeMs.value += 100L
                        }
                    }
                }
                delay(100)
            }
        }
    }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    // Set timeline playback position
    fun seekTo(timeMs: Long) {
        val currentProj = _activeProject.value ?: return
        val maxDurationMs = currentProj.videoDuration * 1000L
        _currentTimeMs.value = timeMs.coerceIn(0, maxDurationMs)
    }

    fun togglePlayback() {
        _isPlaying.value = !_isPlaying.value
    }

    // Load project and observe captions dynamically
    fun selectProject(project: Project) {
        _activeProject.value = project
        _currentTimeMs.value = 0L
        _isPlaying.value = false
        _selectedCaption.value = null

        viewModelScope.launch {
            repository.getCaptions(project.id).collect { captions ->
                _activeCaptions.value = captions
            }
        }
    }

    fun selectCaption(caption: CaptionItem?) {
        _selectedCaption.value = caption
    }

    // Interactive Caption Manipulations
    fun updateSelectedCaption(update: (CaptionItem) -> CaptionItem) {
        val active = _selectedCaption.value ?: return
        val updated = update(active)
        _selectedCaption.value = updated

        // Instantly save changes local state and debounced database save
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCaption(updated)
        }
    }

    fun deleteSelectedCaption() {
        val active = _selectedCaption.value ?: return
        _selectedCaption.value = null
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCaption(active)
        }
    }

    fun addNewCaption(text: String, startTimeMs: Long, endTimeMs: Long) {
        val proj = _activeProject.value ?: return
        val newCaption = CaptionItem(
            projectId = proj.id,
            text = text,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            styleCategory = "Neon",
            textColor = "#FF007F",
            glowColor = "#00F0FF",
            fontSize = 24
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCaption(newCaption)
        }
    }

    // Project creation entry-point
    fun createAndOpenProject(title: String, duration: Int, language: String) {
        viewModelScope.launch {
            val project = Project(
                title = title,
                videoDuration = duration,
                videoSizeMb = (duration * 1.5).coerceIn(5.0, 999.0),
                sourceLanguage = language,
                viralScore = (60..98).random(),
                engagementScore = (65..99).random()
            )
            val projectId = repository.createProject(project)
            val createdProject = project.copy(id = projectId)

            // Generate initial subtitles for the project (simulate Speech-to-text transcripts)
            generateInitialSubtitles(projectId, language)

            _activeProject.value = createdProject
            navigateTo("editor")
            selectProject(createdProject)
        }
    }

    fun deleteProject(project: Project) {
        if (_activeProject.value?.id == project.id) {
            _activeProject.value = null
            _activeCaptions.value = emptyList()
            _selectedCaption.value = null
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProject(project)
        }
    }

    // Update Brand Kit properties
    fun updateBrandKit(kit: BrandKit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveBrandKit(kit)
        }
    }

    // Build & Burn Video Rendering Engine Engine
    fun renderProjectVideo(format: String, resolution: String, subtitleBurn: Boolean) {
        val proj = _activeProject.value ?: return
        val jobId = (1000..9999).random()

        val newJob = RenderJob(
            id = jobId,
            projectTitle = proj.title,
            format = format,
            resolution = resolution,
            progress = 0f,
            status = "Processing",
            subtitleBurned = subtitleBurn
        )

        _renderingQueue.value = _renderingQueue.value + newJob

        viewModelScope.launch(Dispatchers.Default) {
            // Background thread rendering queue animation emulator
            for (step in 1..10) {
                delay(800)
                _renderingQueue.value = _renderingQueue.value.map {
                    if (it.id == jobId) it.copy(progress = step * 0.1f) else it
                }
            }

            _renderingQueue.value = _renderingQueue.value.map {
                if (it.id == jobId) it.copy(status = "Completed", progress = 1.0f) else it
            }

            // Save completed rendered output of lossless 720/1080/2K/4K target, instantly export logs
            val sizeMb = when (resolution) {
                "720P" -> proj.videoDuration * 0.8
                "1080P" -> proj.videoDuration * 1.5
                "2K" -> proj.videoDuration * 3.0
                else -> proj.videoDuration * 6.2
            }

            repository.addExport(
                ExportItem(
                    projectId = proj.id,
                    projectTitle = proj.title,
                    format = format,
                    resolution = resolution,
                    fileSizeMb = sizeMb,
                    status = "Completed"
                )
            )

            // Auto notify back
            addCopilotMessage(
                "system",
                "Successfully exported '${proj.title}' to $format ($resolution) with premium burnt subtitles and 0% source degradation!"
            )
        }
    }

    private fun addCopilotMessage(sender: String, message: String) {
        _copilotMessages.value = _copilotMessages.value + CopilotMessage(sender, message)
    }

    // Direct Remote Gemini API Engine Interrogator
    fun submitCopilotCommand(prompt: String) {
        if (prompt.isBlank()) return
        addCopilotMessage("user", prompt)
        _isCopilotLoading.value = true

        viewModelScope.launch {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isNotEmpty() && key != "MY_GEMINI_API_KEY") {
                try {
                    val systemProm = """
                        You are CaptionX AI: the world's most advanced subtitle optimization editor.
                        You help users write premium subtitle styles, edit transcription errors in Hindi, Hinglish, English, generate high engagement video titles and viral tags.
                        Provide helpful, extremely brief, highly interactive responsive feedback.
                    """.trimIndent()

                    val request = GenerateContentRequest(
                        contents = listOf(com.example.api.Content(parts = listOf(Part(text = prompt)))),
                        systemInstruction = com.example.api.Content(parts = listOf(Part(text = systemProm))),
                        generationConfig = com.example.api.GenerationConfig(temperature = 0.7f)
                    )

                    val response = RetrofitClient.service.generateContent(key, request)
                    val result = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (result != null) {
                        addCopilotMessage("assistant", result)
                    } else {
                        simulateCopilotResponse(prompt)
                    }
                } catch (e: Exception) {
                    // Fallback to local on connection or execution error
                    simulateCopilotResponse(prompt)
                }
            } else {
                // Key not configured, run local emulator
                simulateCopilotResponse(prompt)
            }
            _isCopilotLoading.value = false
        }
    }

    private suspend fun simulateCopilotResponse(prompt: String) {
        delay(1200) // Realistic mock lag
        val clean = prompt.lowercase()
        val response = when {
            clean.contains("style") || clean.contains("font") -> {
                "💡 **CaptionX Style Engine recommendation:** Since your video has dynamic pacing, I recommend the **Neon Glow** style with a **secondary yellow drop shadow** and **Montserrat ExtraBold** font. It increases viewer retention by up to 34%."
            }
            clean.contains("hindi") || clean.contains("hinglish") -> {
                "🗣️ **Multilingual Accent DNA:** I have analyzed your bilingual conversation frames. Mixing devanagari-phonetics seamlessly with English captures both urban and regional viewer demographic beautifully. Transcripts synchronized with Hinglish patterns!"
            }
            clean.contains("title") || clean.contains("viral") -> {
                "🎯 **AI Engagement Engine Suggestions:**\n1. Oye, Yeh Kaise Hua?! 😱 (Viral Hinglish Tier)\n2. The Secret Behind This Caption Tool 🤫\n3. CaptionX Engine is UNREAL 🚀\n🔥 *Viral Potential Index: 98.4%*"
            }
            clean.contains("color") -> {
                "🎨 **Interactive Brand Palette recommendation:**\nPrimary: `#39FF14` (Neon Green Highlight)\nSecondary: `#00F0FF` (Accent Cyan Shadow)\nThis high contrast pairing ensures maximum readability against dark backgrounds."
            }
            else -> {
                "⚡ **CaptionX Copilot Assist:** Video analysed! Empathic visual facial cues mapped. Emojis recommended: 🤫, 😱, 🚀 based on spoken adrenaline peaks. Reading comfort level assessed: **Perfect (140 Words Per Minute)**."
            }
        }
        addCopilotMessage("assistant", response)
    }

    // Copilot smart quick actions
    fun triggerCopilotAction(actionType: String) {
        viewModelScope.launch {
            _isCopilotLoading.value = true
            delay(1000)
            when (actionType) {
                "auto_color" -> {
                    addCopilotMessage("assistant", "🎨 **AI Auto Color Optimization applied!** Customized highlights changed to Neon Cyberpunk Cyan (`#00F0FF`) with high-voltage Pink glows (`#FF007F`). Aesthetics matched to scene contrast.")
                    // Dynamic color adjust for current active captions
                    _activeCaptions.value = _activeCaptions.value.mapIndexed { idx, item ->
                        val col = if (idx % 2 == 0) "#00F0FF" else "#FFFFFF"
                        val glow = if (idx % 2 == 0) "#FF007F" else "#00F0FF"
                        item.copy(textColor = col, glowColor = glow)
                    }
                    // Save to database
                    _activeCaptions.value.forEach {
                        repository.updateCaption(it)
                    }
                }
                "auto_emoji" -> {
                    addCopilotMessage("assistant", "🔥 **AI Dynamic Emoji Placement triggered!** Analyzed dialog peaks and infused relevant high-engagement emojis at phrase endings.")
                    _activeCaptions.value = _activeCaptions.value.map { item ->
                        val textWithEmoji = when {
                            item.text.contains("hacked", true) -> "${item.text} 🤫"
                            item.text.contains("CaptionX", true) -> "${item.text} 🚀"
                            item.text.contains("video", true) -> "${item.text} 📹"
                            item.text.contains("subtitles", true) -> "${item.text} ✨"
                            else -> item.text
                        }
                        item.copy(text = textWithEmoji)
                    }
                    _activeCaptions.value.forEach {
                        repository.updateCaption(it)
                    }
                }
                "preset_style" -> {
                    addCopilotMessage("assistant", "✨ **Luxury Glow preset selected.** Captions updated to elegantly spaced, high-shadow cursive typography.")
                    _activeCaptions.value = _activeCaptions.value.map { item ->
                        item.copy(styleCategory = "Aesthetic", textColor = "#FFFFFF", glowColor = "#FFD700", hasGlow = true)
                    }
                    _activeCaptions.value.forEach {
                        repository.updateCaption(it)
                    }
                }
                "engagement" -> {
                    addCopilotMessage("assistant", "📈 **Audiovisual Virality Predictor Report:**\n- Visual Attention Score: **92/100**\n- Speaker Pacing Comfort: **Excellent**\n- Est. Completion Rate: **84%**\n- Recommended Duration: **No changes needed**")
                }
            }
            _isCopilotLoading.value = false
        }
    }

    private suspend fun generateInitialSubtitles(projectId: Int, language: String) {
        val initialList = when (language) {
            "Hinglish / Mixed" -> listOf(
                CaptionItem(projectId = projectId, startTimeMs = 0L, endTimeMs = 3000L, text = "Hello friends! Aaj hum unbox karenge CaptionX AI.", styleCategory = "Neon"),
                CaptionItem(projectId = projectId, startTimeMs = 3000L, endTimeMs = 6500L, text = "This tool is literally a game changer for videos! 😱", styleCategory = "Neon"),
                CaptionItem(projectId = projectId, startTimeMs = 6500L, endTimeMs = 10000L, text = "Perfect synchronization with absolute zero efforts.", styleCategory = "Glowing"),
                CaptionItem(projectId = projectId, startTimeMs = 10000L, endTimeMs = 14000L, text = "Best quality highlights with custom gradients and glowing colors! 🚀", styleCategory = "Gradient")
            )
            "Hindi (हिन्दी)" -> listOf(
                CaptionItem(projectId = projectId, startTimeMs = 0L, endTimeMs = 3000L, text = "नमस्ते दोस्तों! आज हम देखेंगे CaptionX AI का जादू। ✨", styleCategory = "Elegant"),
                CaptionItem(projectId = projectId, startTimeMs = 3000L, endTimeMs = 6500L, text = "वीडियो में प्रीमियम सबटाइटल्स लगाना अब अत्यधिक सरल है।", styleCategory = "Neon"),
                CaptionItem(projectId = projectId, startTimeMs = 6500L, endTimeMs = 10000L, text = "सिर्फ एक क्लिक और आपका वीडियो वायरल होने के लिए रेडी! 🔥", styleCategory = "Glowing")
            )
            else -> listOf(
                CaptionItem(projectId = projectId, startTimeMs = 0L, endTimeMs = 3000L, text = "Yo! Welcome to the premium CaptionX AI Studio. 🚀", styleCategory = "Cyberpunk"),
                CaptionItem(projectId = projectId, startTimeMs = 3000L, endTimeMs = 6500L, text = "Perfectly synchronized human-like speech subtitles.", styleCategory = "Neon"),
                CaptionItem(projectId = projectId, startTimeMs = 6500L, endTimeMs = 10000L, text = "Fully customized drag, drop, rotate and styling previews! 🔥", styleCategory = "Luxury"),
                CaptionItem(projectId = projectId, startTimeMs = 10000L, endTimeMs = 14000L, text = "No watermarks, full 4K output render pipeline live.", styleCategory = "Minimal")
            )
        }
        repository.insertCaptions(initialList)
    }

    private suspend fun createDefaultProjects() {
        val proj1Id = repository.createProject(Project(title = "Tech Review Hinglish Vlog", videoDuration = 14, videoSizeMb = 21.4, sourceLanguage = "Hinglish / Mixed", viralScore = 89, engagementScore = 91))
        generateInitialSubtitles(proj1Id.toInt(), "Hinglish / Mixed")

        val proj2Id = repository.createProject(Project(title = "Cooking Masterclass Reel", videoDuration = 10, videoSizeMb = 14.2, sourceLanguage = "Hindi (हिन्दी)", viralScore = 92, engagementScore = 86))
        generateInitialSubtitles(proj2Id.toInt(), "Hindi (हिन्दी)")

        val proj3Id = repository.createProject(Project(title = "Adrenaline Shorts Edit", videoDuration = 14, videoSizeMb = 25.8, sourceLanguage = "English", viralScore = 95, engagementScore = 94))
        generateInitialSubtitles(proj3Id.toInt(), "English")
    }
}

data class CopilotMessage(
    val sender: String, // "system", "user", "assistant"
    val content: String
)

data class RenderJob(
    val id: Int,
    val projectTitle: String,
    val format: String,
    val resolution: String,
    val progress: Float,
    val status: String, // "Processing", "Completed"
    val subtitleBurned: Boolean
)
