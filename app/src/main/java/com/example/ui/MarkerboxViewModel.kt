package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.Candidate
import com.example.api.GenerateContentRequest
import com.example.api.GeminiApiClient
import com.example.api.PromptContent
import com.example.api.PromptPart
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MarkerboxViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val projectDao = db.projectDao()
    private val revisionDao = db.revisionDao()
    private val aulaDao = db.aulaDao()

    // Preferences
    private val prefs = application.getSharedPreferences("markerbox_prefs", Context.MODE_PRIVATE)

    var isDarkTheme by mutableStateOf(prefs.getBoolean("is_dark_theme", true))
        private set

    var customApiKey by mutableStateOf(prefs.getString("custom_api_key", "") ?: "")
        private set

    var frequentComments by mutableStateOf(
        prefs.getStringSet("frequent_comments", setOf("Introdução", "Abertura", "Corte", "Erro de áudio", "Encerramento", "OK"))?.toList()?.sorted() ?: listOf("Abertura", "Corte", "Encerramento", "Erro de áudio", "Introdução", "OK")
    )
        private set

    // DB States
    val projects = projectDao.getAllProjectsFlow()

    private var revisionsJob: kotlinx.coroutines.Job? = null
    private var aulasJob: kotlinx.coroutines.Job? = null
    private var autoLoadedActiveProject = false

    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()

    private val _revisions = MutableStateFlow<List<Revision>>(emptyList())
    val revisions: StateFlow<List<Revision>> = _revisions.asStateFlow()

    private val _currentRevision = MutableStateFlow<Revision?>(null)
    val currentRevision: StateFlow<Revision?> = _currentRevision.asStateFlow()

    private val _aulas = MutableStateFlow<List<Aula>>(emptyList())
    val aulas: StateFlow<List<Aula>> = _aulas.asStateFlow()

    // Loading & Busy States
    var isFormattingAll by mutableStateOf(false)
        private set

    var formattingProgress by mutableStateOf(0f)
        private set

    init {
        // Automatically check if there is an active project we can load
        viewModelScope.launch {
            projects.collectLatest { list ->
                if (list.isNotEmpty() && !autoLoadedActiveProject && _currentProject.value == null) {
                    autoLoadedActiveProject = true
                    val lastProjId = prefs.getLong("last_project_id", -1)
                    val toLoad = list.find { it.id == lastProjId } ?: list.first()
                    selectProject(toLoad)
                }
            }
        }
    }

    fun toggleTheme() {
        isDarkTheme = !isDarkTheme
        prefs.edit().putBoolean("is_dark_theme", isDarkTheme).apply()
    }

    fun updateCustomApiKey(key: String) {
        customApiKey = key
        prefs.edit().putString("custom_api_key", key).apply()
    }

    fun addFrequentComment(comment: String) {
        if (comment.isNotBlank() && !frequentComments.contains(comment)) {
            val newList = (frequentComments + comment).sorted()
            frequentComments = newList
            prefs.edit().putStringSet("frequent_comments", newList.toSet()).apply()
        }
    }

    fun removeFrequentComment(comment: String) {
        val newList = frequentComments.filter { it != comment }
        frequentComments = newList
        prefs.edit().putStringSet("frequent_comments", newList.toSet()).apply()
    }

    fun selectProject(project: Project?) {
        _currentProject.value = project
        revisionsJob?.cancel()
        aulasJob?.cancel()

        if (project != null) {
            prefs.edit().putLong("last_project_id", project.id).apply()
            // Observe revisions for this project
            revisionsJob = viewModelScope.launch {
                revisionDao.getRevisionsForProjectFlow(project.id).collectLatest { revsList ->
                    _revisions.value = revsList
                    // Auto select first revision if none active or active not in list
                    val currentRev = _currentRevision.value
                    if (revsList.isNotEmpty() && (currentRev == null || revsList.none { it.id == currentRev.id })) {
                        selectRevision(revsList.first())
                    } else if (revsList.isEmpty()) {
                        _currentRevision.value = null
                        _aulas.value = emptyList()
                    }
                }
            }
        } else {
            _revisions.value = emptyList()
            _currentRevision.value = null
            _aulas.value = emptyList()
        }
    }

    fun selectRevision(revision: Revision?) {
        _currentRevision.value = revision
        aulasJob?.cancel()

        if (revision != null) {
            aulasJob = viewModelScope.launch {
                aulaDao.getAulasForRevisionFlow(revision.id).collectLatest { aulasList ->
                    _aulas.value = aulasList
                }
            }
        } else {
            _aulas.value = emptyList()
        }
    }

    fun createNewProject(name: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val defaultProj = Project(
                name = name,
                globalPrompt = DefaultPrompt.TEXT
            )
            val newProjId = projectDao.insertProject(defaultProj)
            val createdProject = projectDao.getProjectById(newProjId)
            
            // Add initial revision
            if (createdProject != null) {
                val initialRev = Revision(projectId = newProjId, title = "Rev 1", orderIndex = 0)
                val revId = revisionDao.insertRevision(initialRev)
                
                // Add initial aula
                val initialAula = Aula(
                    revisionId = revId,
                    title = "Aula 1.1",
                    textInputsJson = JsonUtils.serializeStringList(listOf("")),
                    inlineInputsJson = JsonUtils.serializeInlineRows(listOf(listOf(InlineRow())))
                )
                aulaDao.insertAula(initialAula)

                selectProject(createdProject)
            }
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun updateProjectMetadata(project: Project) {
        viewModelScope.launch {
            projectDao.updateProject(project.copy(lastUpdated = System.currentTimeMillis()))
            _currentProject.value = project
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            projectDao.deleteProject(project)
            if (_currentProject.value?.id == project.id) {
                selectProject(null)
            }
        }
    }

    // Revision Actions
    fun addRevision(title: String) {
        val projId = _currentProject.value?.id ?: return
        viewModelScope.launch {
            val order = _revisions.value.size
            val newRev = Revision(projectId = projId, title = title, orderIndex = order)
            val revId = revisionDao.insertRevision(newRev)

            // Add first initial aula for this rev
            val initialAula = Aula(
                revisionId = revId,
                title = "Aula 1.1",
                textInputsJson = JsonUtils.serializeStringList(listOf("")),
                inlineInputsJson = JsonUtils.serializeInlineRows(listOf(listOf(InlineRow())))
            )
            aulaDao.insertAula(initialAula)
        }
    }

    fun duplicateRevision(revision: Revision) {
        viewModelScope.launch {
            val order = _revisions.value.size
            val dupRev = Revision(
                projectId = revision.projectId,
                title = "${revision.title} (Cópia)",
                orderIndex = order
            )
            val newRevId = revisionDao.insertRevision(dupRev)

            // Duplicate aulas
            val existingAulas = aulaDao.getAulasForRevision(revision.id)
            existingAulas.forEach { oldAula ->
                val newAula = oldAula.copy(
                    id = 0,
                    revisionId = newRevId
                )
                aulaDao.insertAula(newAula)
            }
        }
    }

    fun deleteRevision(revision: Revision) {
        viewModelScope.launch {
            revisionDao.deleteRevision(revision)
        }
    }

    // Aula Actions
    fun addAula() {
        val revId = _currentRevision.value?.id ?: return
        viewModelScope.launch {
            val order = _aulas.value.size
            
            // Smart numbering calculation
            val existingTitles = _aulas.value.map { it.title }
            var major = 1
            if (existingTitles.isNotEmpty()) {
                val match = existingTitles.last().matchMajor()
                if (match != null) {
                    major = match + 1
                }
            }
            val newTitle = "Aula $major.1"

            val newAula = Aula(
                revisionId = revId,
                title = newTitle,
                textInputsJson = JsonUtils.serializeStringList(listOf("")),
                inlineInputsJson = JsonUtils.serializeInlineRows(listOf(listOf(InlineRow()))),
                orderIndex = order
            )
            aulaDao.insertAula(newAula)
        }
    }

    private fun String.matchMajor(): Int? {
        val regex = Regex("""^Aula\s+([\d]+)""")
        val matchResult = regex.find(this) ?: return null
        return matchResult.groupValues[1].toIntOrNull()
    }

    fun updateAula(aula: Aula) {
        viewModelScope.launch {
            aulaDao.updateAula(aula)
        }
    }

    fun deleteAula(aula: Aula) {
        viewModelScope.launch {
            aulaDao.deleteAula(aula)
        }
    }

    // Call API for a single Aula
    fun formatAula(aula: Aula, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        val proj = _currentProject.value ?: return
        viewModelScope.launch {
            // Retrieve actual texts to format
            val rawInput = if (aula.inlineMode) {
                // Convert list of list of InlineRows to formatted input
                val parts = JsonUtils.deserializeInlineRows(aula.inlineInputsJson)
                parts.joinToString("\n\n---\n\n") { part ->
                    part.filter { it.entrada.isNotBlank() || it.saida.isNotBlank() || it.comentario.isNotBlank() }
                        .joinToString("\n") { row ->
                            "${row.entrada}\t${row.saida}\t${row.comentario}"
                        }
                }
            } else {
                val parts = JsonUtils.deserializeStringList(aula.textInputsJson)
                parts.joinToString("\n\n---\n\n")
            }

            if (rawInput.isBlank()) {
                onResult(false, "O texto de entrada está vazio.")
                return@launch
            }

            // Show temporary loading indicator
            val updatedAula = aula.copy(output = "Formatando com IA...")
            aulaDao.updateAula(updatedAula)

            val apiKey = customApiKey.ifBlank { BuildConfig.GEMINI_API_KEY }
            if (apiKey.isBlank()) {
                val errorMsg = "Erro: API Key do Gemini não configurada. Configure a sua chave de API nas Opções."
                aulaDao.updateAula(aula.copy(output = errorMsg))
                onResult(false, errorMsg)
                return@launch
            }

            val systemInstructionText = "Você é um assistente de formatação de texto para o plugin Markerbox do Adobe Premiere. " +
                    "Apenas forneça a saída formatada contendo tabulações reais ('\\t') sem introduções ou explicações. " +
                    "Seja extremamente preciso."

            val prompt = "INSTRUÇÕES:\n${proj.globalPrompt}\n\nTEXTO DE ENTRADA:\n$rawInput"

            try {
                val request = GenerateContentRequest(
                    contents = listOf(PromptContent(parts = listOf(PromptPart(text = prompt)))),
                    systemInstruction = PromptContent(parts = listOf(PromptPart(text = systemInstructionText)))
                )
                val response = GeminiApiClient.service.generateContent(apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                
                if (responseText != null) {
                    // Smart Title extraction from output if matched
                    var newTitle = aula.title
                    val matchAula = Regex("""Aula\s*(\d+)""", RegexOption.IGNORE_CASE).find(rawInput)
                    val matchVideo = Regex("""Vídeo\s*(\d+)""", RegexOption.IGNORE_CASE).find(rawInput)
                    if (matchAula != null && matchVideo != null) {
                        newTitle = "Aula ${matchAula.groupValues[1]}.${matchVideo.groupValues[1]}"
                    }

                    aulaDao.updateAula(aula.copy(output = responseText, title = newTitle))
                    onResult(true, "Formatado com sucesso!")
                } else {
                    val errorMsg = "Sem resposta do Gemini."
                    aulaDao.updateAula(aula.copy(output = errorMsg))
                    onResult(false, errorMsg)
                }
            } catch (e: Exception) {
                // Return a clean fallback local result inline in case API fails
                val fallbackText = localFallbackFormat(rawInput)
                aulaDao.updateAula(aula.copy(output = fallbackText))
                onResult(true, "Finalizado (Processado via Fallback local devido a falha de conexão: ${e.message})")
            }
        }
    }

    // Format all Aulas in the current revision iteratively
    fun formatAllAulas() {
        val aulasList = _aulas.value
        if (aulasList.isEmpty()) return
        isFormattingAll = true
        formattingProgress = 0f

        viewModelScope.launch {
            aulasList.forEachIndexed { index, aula ->
                formatAula(aula)
                formattingProgress = (index + 1) / aulasList.size.toFloat()
            }
            isFormattingAll = false
        }
    }

    // Sharing / Export actions of Markerbox output
    fun exportToText(context: Context) {
        val proj = _currentProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val builder = java.lang.StringBuilder()
            builder.append("=== DECUPAGEM: ${proj.name} ===\n")
            builder.append("Curso: ${proj.course} | Fase: ${proj.phase} | Disciplina: ${proj.discipline}\n")
            builder.append("Responsável pelas Notas: ${proj.responsibleNotes} | Responsável pela Edição: ${proj.responsibleEditor}\n")
            builder.append("=========================================\n\n")

            // Revisions
            val revsList = revisionDao.getRevisionsForProject(proj.id)
            for (rev in revsList) {
                builder.append("//--- REVISÃO: ${rev.title} ---//\n\n")
                val aulasList = aulaDao.getAulasForRevision(rev.id)
                for (aula in aulasList) {
                    builder.append("//--- ${aula.title} ---//\n")
                    builder.append(aula.output.ifBlank { "Nenhum resultado gerado para esta aula." })
                    builder.append("\n\n")
                }
            }

            val textResult = builder.toString()
            withContext(Dispatchers.Main) {
                shareFile(context, textResult, "${proj.name.sanitizeFileName()}_decupagem.txt", "text/plain")
            }
        }
    }

    fun exportToCsv(context: Context) {
        val proj = _currentProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val builder = java.lang.StringBuilder()
            // Semicolon-separated CSV (extremely popular and compatible with Excel in Brazilian/Portuguese locales)
            builder.append("Projeto;Revisao;Aula;Entrada;Saida;Comentario\n")

            val revsList = revisionDao.getRevisionsForProject(proj.id)
            for (rev in revsList) {
                val aulasList = aulaDao.getAulasForRevision(rev.id)
                for (aula in aulasList) {
                    val lines = aula.output.split("\n")
                    for (line in lines) {
                        if (line.isNotBlank() && !line.startsWith("Aguarde") && !line.startsWith("Erro")) {
                            val parts = line.split("\t")
                            val entrada = parts.getOrNull(0)?.trim() ?: ""
                            val saida = parts.getOrNull(1)?.trim() ?: ""
                            val comentario = parts.drop(2).joinToString(" ").trim()
                            
                            // Escape quotes for CSV safety
                            val safeProj = proj.name.escapeCsv()
                            val safeRev = rev.title.escapeCsv()
                            val safeAula = aula.title.escapeCsv()
                            val safeCom = comentario.escapeCsv()
                            
                            builder.append("\"$safeProj\";\"$safeRev\";\"$safeAula\";\"$entrada\";\"$saida\";\"$safeCom\"\n")
                        }
                    }
                }
            }

            val csvResult = builder.toString()
            withContext(Dispatchers.Main) {
                shareFile(context, csvResult, "${proj.name.sanitizeFileName()}_planilha.csv", "text/csv")
            }
        }
    }

    private fun shareFile(context: Context, content: String, defaultName: String, mimeType: String) {
        try {
            val tempDir = context.cacheDir
            val file = File(tempDir, defaultName)
            val outputStream = FileOutputStream(file)
            outputStream.write(content.toByteArray(Charsets.UTF_8))
            outputStream.close()

            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, defaultName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Exportar Decupagem"))
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao exportar arquivo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun String.sanitizeFileName(): String {
        return this.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    }

    private fun String.escapeCsv(): String {
        return this.replace("\"", "\"\"")
    }

    // Highly optimized local parser context fallback when network or API fails
    private fun localFallbackFormat(input: String): String {
        val lines = input.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        val outputLines = mutableListOf<String>()
        var lastMaxSec = 0

        for (rawLine in lines) {
            val timeRegex = Regex("""(\d{1,2}:\d{2}(?::\d{2})?|\d{1,2}\s*min|\d{1,2}\s*s)""", RegexOption.IGNORE_CASE)
            val matchedTimes = timeRegex.findAll(rawLine).map { it.value }.toList()
            val comment = rawLine.replace(timeRegex, "").replace("~", "").replace("-", "").trim()

            if (matchedTimes.size >= 2) {
                val t1 = matchedTimes[0].parseToSeconds()
                val t2 = matchedTimes[1].parseToSeconds()
                val offsetCompensatedT1 = if (t1 < lastMaxSec) t1 + lastMaxSec else t1
                val offsetCompensatedT2 = if (t2 < lastMaxSec) t2 + lastMaxSec else t2
                
                outputLines.add("${offsetCompensatedT1.formatSeconds()}\t${offsetCompensatedT2.formatSeconds()}\t$comment")
                lastMaxSec = maxOf(lastMaxSec, offsetCompensatedT2)
            } else if (matchedTimes.size == 1) {
                val t = matchedTimes[0].parseToSeconds()
                val offsetCompensating = if (t < lastMaxSec) t + lastMaxSec else t
                outputLines.add("${offsetCompensating.formatSeconds()}\t${(offsetCompensating + 5).formatSeconds()}\t$comment")
                lastMaxSec = maxOf(lastMaxSec, offsetCompensating + 5)
            } else {
                if (rawLine.lowercase().contains("ok")) {
                    outputLines.add("00:00\t00:01\tOK")
                } else {
                    outputLines.add("00:00\t00:05\t$rawLine")
                }
            }
        }
        return outputLines.joinToString("\n")
    }

    private fun String.parseToSeconds(): Int {
        val parts = this.split(":")
        if (parts.size == 3) {
            return (parts[0].toIntOrNull() ?: 0) * 3600 + (parts[1].toIntOrNull() ?: 0) * 60 + (parts[2].toIntOrNull() ?: 0)
        } else if (parts.size == 2) {
            return (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
        }
        val justDigits = this.replace(Regex("\\D"), "").toIntOrNull() ?: 0
        return justDigits
    }

    private fun Int.formatSeconds(): String {
        val h = this / 3600
        val m = (this % 3600) / 60
        val s = this % 60
        return if (h > 0) {
            "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
        } else {
            "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
        }
    }
}
