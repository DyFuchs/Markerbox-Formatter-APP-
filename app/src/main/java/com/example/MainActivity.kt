package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.MarkerboxViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MarkerboxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDark = viewModel.isDarkTheme
            MyApplicationTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MarkerboxApp(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MarkerboxApp(viewModel: MarkerboxViewModel) {
    val context = LocalContext.current
    val keyboardController = LocalContext.current
    val scope = rememberCoroutineScope()

    // Observables
    val projects by viewModel.projects.collectAsStateWithLifecycle(initialValue = emptyList())
    val currentProject by viewModel.currentProject.collectAsStateWithLifecycle()
    val revisions by viewModel.revisions.collectAsStateWithLifecycle()
    val currentRevision by viewModel.currentRevision.collectAsStateWithLifecycle()
    val aulas by viewModel.aulas.collectAsStateWithLifecycle()

    // Dialog state
    var showCreateProjectDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    // Screen selection
    if (currentProject == null) {
        // Welcome Dashboard
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Markerbox Formatter",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                "Organize suas de-cupagens e transcrições com IA",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.testTag("top_settings_button")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Configurações")
                        }
                        IconButton(
                            onClick = { showHelpDialog = true },
                            modifier = Modifier.testTag("top_help_button")
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Ajuda")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateProjectDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("create_project_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Novo Projeto")
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (projects.isEmpty()) {
                    // Empty State
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Nenhum projeto de decupagem",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Crie um novo projeto usando o botão abaixo para começar a formatar e ajustar minutagens.",
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedGrid(12.dp)
                    ) {
                        item {
                            Text(
                                "Seus Projetos de Decupagem",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(projects) { project ->
                            Card(
                                onClick = { viewModel.selectProject(project) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("project_item_${project.id}"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = project.name,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { viewModel.deleteProject(project) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Excluir",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (project.discipline.isNotBlank() || project.course.isNotBlank()) {
                                        Text(
                                            text = "Ref: ${project.course.ifBlank { "N/A" }} - ${project.discipline.ifBlank { "N/A" }}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Atualizado em: " + java.text.SimpleDateFormat(
                                            "dd/MM/yyyy HH:mm",
                                            java.util.Locale.getDefault()
                                        ).format(project.lastUpdated),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        val proj = currentProject!!
        // Project Workspace Screen
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                proj.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Editando decupagem",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.selectProject(null) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Configurações")
                        }
                        IconButton(onClick = { showHelpDialog = true }) {
                            Icon(Icons.Default.Info, contentDescription = "Instruções")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Workspace Content is divided into collapsible sections
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        // Collapsible Metadata Card
                        MetadataSection(
                            proj = proj,
                            onUpdate = { viewModel.updateProjectMetadata(it) }
                        )
                    }

                    item {
                        // Prompt Geral Collapsible Card
                        PromptSection(
                            proj = proj,
                            onUpdate = { viewModel.updateProjectMetadata(it) }
                        )
                    }

                    item {
                        // Export and General Actions Rows
                        ActionsSection(
                            viewModel = viewModel,
                            onAddAula = { viewModel.addAula() }
                        )
                    }

                    item {
                        // Revisions dynamic tab bar
                        RevisionsTabsSection(
                            revisions = revisions,
                            currentRevision = currentRevision,
                            onSelect = { viewModel.selectRevision(it) },
                            onNewRev = { name -> viewModel.addRevision(name) },
                            onDuplicate = { viewModel.duplicateRevision(it) },
                            onDelete = { viewModel.deleteRevision(it) }
                        )
                    }

                    // Render list of active Aulas
                    if (currentRevision == null) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Crie uma Revisão para gerenciar as aulas",
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else if (aulas.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Nenhuma aula criada nesta revisão",
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.addAula() },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Adicionar Primeira Aula")
                                }
                            }
                        }
                    } else {
                        itemsIndexed(aulas, key = { _, aula -> aula.id }) { index, aula ->
                            AulaCard(
                                index = index,
                                aula = aula,
                                viewModel = viewModel
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(30.dp))
                        }
                    }
                }
            }
        }
    }

    // --- Create Project Dialog ---
    if (showCreateProjectDialog) {
        var newProjName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateProjectDialog = false },
            title = { Text("Nova Decupagem", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newProjName,
                    onValueChange = { newProjName = it },
                    label = { Text("Nome da Decupagem") },
                    placeholder = { Text("Ex: Aula 1 - Visão de Futuro") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_name_field"),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProjName.isNotBlank()) {
                            viewModel.createNewProject(newProjName)
                            showCreateProjectDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_project")
                ) {
                    Text("Criar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateProjectDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // --- Options Settings Dialog ---
    if (showSettingsDialog) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettingsDialog = false }
        )
    }

    // --- Helper / Instructions Dialog ---
    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
    }
}

@Composable
fun MetadataSection(
    proj: Project,
    onUpdate: (Project) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // Memory-backed states for instant editing and focus persistence
    var localCourse by remember(proj.id) { mutableStateOf(proj.course) }
    var localPhase by remember(proj.id) { mutableStateOf(proj.phase) }
    var localDiscipline by remember(proj.id) { mutableStateOf(proj.discipline) }
    var localResponsibleNotes by remember(proj.id) { mutableStateOf(proj.responsibleNotes) }
    var localResponsibleEditor by remember(proj.id) { mutableStateOf(proj.responsibleEditor) }

    LaunchedEffect(localCourse, localPhase, localDiscipline, localResponsibleNotes, localResponsibleEditor) {
        kotlinx.coroutines.delay(700)
        onUpdate(
            proj.copy(
                course = localCourse,
                phase = localPhase,
                discipline = localDiscipline,
                responsibleNotes = localResponsibleNotes,
                responsibleEditor = localResponsibleEditor
            )
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                     imageVector = if (expanded) Icons.Default.ArrowBack else Icons.Default.PlayArrow,
                     contentDescription = null,
                     tint = MaterialTheme.colorScheme.primary,
                     modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Metadados do Projeto",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    if (expanded) "Ocultar" else "Editar",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = localCourse,
                        onValueChange = { localCourse = it },
                        label = { Text("Curso", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = localPhase,
                        onValueChange = { localPhase = it },
                        label = { Text("Fase", fontSize = 11.sp) },
                        modifier = Modifier.weight(0.6f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = localDiscipline,
                    onValueChange = { localDiscipline = it },
                    label = { Text("Disciplina") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = localResponsibleNotes,
                    onValueChange = { localResponsibleNotes = it },
                    label = { Text("Resp. pelas Notas") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = localResponsibleEditor,
                    onValueChange = { localResponsibleEditor = it },
                    label = { Text("Resp. pela Edição") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
fun PromptSection(
    proj: Project,
    onUpdate: (Project) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // Memory-backed state for instant editing and focus persistence of global prompt
    var localGlobalPrompt by remember(proj.id) { mutableStateOf(proj.globalPrompt) }

    LaunchedEffect(localGlobalPrompt) {
        kotlinx.coroutines.delay(700)
        onUpdate(proj.copy(globalPrompt = localGlobalPrompt))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowBack else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Prompt Geral",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    if (expanded) "Ocultar" else "Expandir",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = localGlobalPrompt,
                    onValueChange = { localGlobalPrompt = it },
                    label = { Text("Instruções que a IA usará para formatar") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    maxLines = 15
                )
            }
        }
    }
}

@Composable
fun ActionsSection(
    viewModel: MarkerboxViewModel,
    onAddAula: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onAddAula,
            modifier = Modifier
                .weight(1f)
                .testTag("add_aula_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Nova Aula", fontSize = 12.sp)
        }

        Button(
            onClick = { viewModel.formatAllAulas() },
            modifier = Modifier
                .weight(1f)
                .testTag("format_all_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Formatar Tudo", fontSize = 12.sp)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = { viewModel.exportToText(context) },
            modifier = Modifier
                .weight(1f)
                .testTag("export_text_button"),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Exportar TXT", fontSize = 12.sp)
        }

        OutlinedButton(
            onClick = { viewModel.exportToCsv(context) },
            modifier = Modifier
                .weight(1f)
                .testTag("export_csv_button"),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Exportar CSV", fontSize = 12.sp)
        }
    }
}

@Composable
fun RevisionsTabsSection(
    revisions: List<Revision>,
    currentRevision: Revision?,
    onSelect: (Revision) -> Unit,
    onNewRev: (String) -> Unit,
    onDuplicate: (Revision) -> Unit,
    onDelete: (Revision) -> Unit
) {
    var showAddRevDialog by remember { mutableStateOf(false) }
    var actionTargetRev by remember { mutableStateOf<Revision?>(null) }
    var showActionMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        revisions.forEach { rev ->
            val isSelected = rev.id == currentRevision?.id
            Card(
                onClick = { onSelect(rev) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .clickable { onSelect(rev) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rev.title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Menu",
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(14.dp)
                            .clickable {
                                actionTargetRev = rev
                                showActionMenu = true
                            }
                    )
                }
            }
        }

        // '+' Button to add Revision
        OutlinedButton(
            onClick = { showAddRevDialog = true },
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nova Revisão", modifier = Modifier.size(16.dp))
        }
    }

    if (showAddRevDialog) {
        var revName by remember { mutableStateOf("Rev ${revisions.size + 1}") }
        AlertDialog(
            onDismissRequest = { showAddRevDialog = false },
            title = { Text("Nova Revisão", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = revName,
                    onValueChange = { revName = it },
                    label = { Text("Nome da Revisão") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (revName.isNotBlank()) {
                            onNewRev(revName)
                            showAddRevDialog = false
                        }
                    }
                ) {
                    Text("Adicionar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRevDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showActionMenu && actionTargetRev != null) {
        AlertDialog(
            onDismissRequest = { showActionMenu = false },
            title = { Text("Revisão: ${actionTargetRev!!.title}", fontWeight = FontWeight.Bold) },
            text = { Text("Escolha uma ação para esta Revisão.") },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            onDuplicate(actionTargetRev!!)
                            showActionMenu = false
                        }
                    ) {
                        Text("Duplicar")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            onDelete(actionTargetRev!!)
                            showActionMenu = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Excluir")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showActionMenu = false }) {
                    Text("Fechar")
                }
            }
        )
    }
}

// Full Collapsible Lesson (Aula) UI Card
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AulaCard(
    index: Int,
    aula: Aula,
    viewModel: MarkerboxViewModel
) {
    var expanded by remember { mutableStateOf(true) }
    var showFreqSuggestionsRowId by remember { mutableStateOf<Pair<Int, Int>?>(null) } // PartIndex, RowIndex

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Local Compose memory-backed states to prevent Room database write-and-read invalidation loops during active typing
    var localTitle by remember(aula.id) { mutableStateOf(aula.title) }
    var localInlineRows by remember(aula.id) { mutableStateOf(JsonUtils.deserializeInlineRows(aula.inlineInputsJson)) }
    var localTextInputs by remember(aula.id) { mutableStateOf(JsonUtils.deserializeStringList(aula.textInputsJson)) }
    var localInlineMode by remember(aula.id) { mutableStateOf(aula.inlineMode) }
    var localMultipart by remember(aula.id) { mutableStateOf(aula.multipart) }
    var localLessThanOneHour by remember(aula.id) { mutableStateOf(aula.lessThanOneHour) }

    // Debounced background database persistence to avoid main-thread performance dips and input connection disruptions
    LaunchedEffect(localTitle, localInlineRows, localTextInputs, localInlineMode, localMultipart, localLessThanOneHour) {
        kotlinx.coroutines.delay(700)
        val updatedAula = aula.copy(
            title = localTitle,
            inlineInputsJson = JsonUtils.serializeInlineRows(localInlineRows),
            textInputsJson = JsonUtils.serializeStringList(localTextInputs),
            inlineMode = localInlineMode,
            multipart = localMultipart,
            lessThanOneHour = localLessThanOneHour
        )
        viewModel.updateAula(updatedAula)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("aula_card_${aula.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Card Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowBack else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = localTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { viewModel.deleteAula(aula) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Excluir Aula",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (expanded) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Title Editor Inline
                    OutlinedTextField(
                        value = localTitle,
                        onValueChange = { localTitle = it },
                        label = { Text("Nome da Aula", fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        singleLine = true
                    )

                    // Options Checkboxes
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = localInlineMode,
                                onCheckedChange = { localInlineMode = it }
                            )
                            Text("Modo em Linha", fontSize = 13.sp)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = localMultipart,
                                onCheckedChange = { localMultipart = it }
                            )
                            Text("Vídeo em várias partes", fontSize = 13.sp)
                        }

                        if (localInlineMode) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = localLessThanOneHour,
                                    onCheckedChange = { localLessThanOneHour = it }
                                )
                                Text("Vídeo menor que 1h", fontSize = 13.sp)
                            }
                        }
                    }

                    // --- INPUT LAYER ---
                    if (localInlineMode) {
                        // INLINE EDIT MODE (Row list)
                        val updatedParts = if (localInlineRows.isEmpty()) listOf(emptyList()) else localInlineRows

                        updatedParts.forEachIndexed { partIndex, rowList ->
                            if (localMultipart) {
                                Text(
                                    "Parte ${partIndex + 1}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            // Headers
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Entrada", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.28f))
                                Text("Saída", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.28f))
                                Text("Comentários", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.44f))
                            }

                            rowList.forEachIndexed { rowIndex, row ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Entrada
                                    OutlinedTextField(
                                        value = row.entrada,
                                        onValueChange = { newVal ->
                                            val sanitized = newVal.filter { it.isDigit() || it == ':' }
                                            val newRowList = rowList.toMutableList()
                                            newRowList[rowIndex] = row.copy(entrada = sanitized)
                                            val newParts = updatedParts.toMutableList()
                                            newParts[partIndex] = newRowList
                                            localInlineRows = newParts
                                        },
                                        placeholder = { Text(if (localLessThanOneHour) "MM:SS" else "HH:MM") },
                                        singleLine = true,
                                        modifier = Modifier.weight(0.28f),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                    )

                                    // Saida
                                    OutlinedTextField(
                                        value = row.saida,
                                        onValueChange = { newVal ->
                                            val sanitized = newVal.filter { it.isDigit() || it == ':' }
                                            val newRowList = rowList.toMutableList()
                                            newRowList[rowIndex] = row.copy(saida = sanitized)
                                            val newParts = updatedParts.toMutableList()
                                            newParts[partIndex] = newRowList
                                            localInlineRows = newParts
                                        },
                                        placeholder = { Text(if (localLessThanOneHour) "MM:SS" else "HH:MM") },
                                        singleLine = true,
                                        modifier = Modifier.weight(0.28f),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                    )

                                    // Comentario & Sugestoes icon
                                    Box(modifier = Modifier.weight(0.44f)) {
                                        OutlinedTextField(
                                            value = row.comentario,
                                            onValueChange = { newVal ->
                                                val newRowList = rowList.toMutableList()
                                                newRowList[rowIndex] = row.copy(comentario = newVal)
                                                val newParts = updatedParts.toMutableList()
                                                newParts[partIndex] = newRowList
                                                localInlineRows = newParts
                                            },
                                            placeholder = { Text("Nota", fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                            trailingIcon = {
                                                IconButton(
                                                    onClick = {
                                                        showFreqSuggestionsRowId = if (showFreqSuggestionsRowId == partIndex to rowIndex) null else partIndex to rowIndex
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.PlayArrow,
                                                        contentDescription = "Sugestão",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        )

                                        // Auto Suggestion Menu for Fast Comment input
                                        if (showFreqSuggestionsRowId == partIndex to rowIndex) {
                                            DropdownSuggestions(
                                                suggestions = viewModel.frequentComments,
                                                onSelected = { selected ->
                                                    val newRowList = rowList.toMutableList()
                                                    newRowList[rowIndex] = row.copy(comentario = selected)
                                                    val newParts = updatedParts.toMutableList()
                                                    newParts[partIndex] = newRowList
                                                    localInlineRows = newParts
                                                    showFreqSuggestionsRowId = null
                                                },
                                                onSaveAsFreq = {
                                                    if (row.comentario.isNotBlank()) {
                                                        viewModel.addFrequentComment(row.comentario)
                                                        Toast.makeText(context, "Salvo nos favoritos", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                onDismiss = { showFreqSuggestionsRowId = null }
                                            )
                                        }
                                    }

                                    // Inline Trash to delete row
                                    IconButton(
                                        onClick = {
                                            if (rowList.size > 1) {
                                                val newRowList = rowList.toMutableList()
                                                newRowList.removeAt(rowIndex)
                                                val newParts = updatedParts.toMutableList()
                                                newParts[partIndex] = newRowList
                                                localInlineRows = newParts
                                            }
                                        },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Excluir Linha",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Quick buttons to add markup inline inside the part
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = {
                                        val newRowList = rowList.toMutableList()
                                        newRowList.add(InlineRow())
                                        val newParts = updatedParts.toMutableList()
                                        newParts[partIndex] = newRowList
                                        localInlineRows = newParts
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Adicionar Linha", fontSize = 11.sp)
                                }
                            }
                        }

                        // Add new Part if multi-part is enabled
                        if (localMultipart) {
                            ElevatedButton(
                                onClick = {
                                    val newParts = updatedParts.toMutableList()
                                    newParts.add(listOf(InlineRow()))
                                    localInlineRows = newParts
                                },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Adicionar Nova Parte de Vídeo", fontSize = 11.sp)
                            }
                        }

                    } else {
                        // TRADITIONAL CUSTOM OUTLINED FIELD MODE
                        val updatedTexts = if (localTextInputs.isEmpty()) listOf("") else localTextInputs

                        updatedTexts.forEachIndexed { index, text ->
                            OutlinedTextField(
                                value = text,
                                onValueChange = { newVal ->
                                    val newList = updatedTexts.toMutableList()
                                    newList[index] = newVal
                                    localTextInputs = newList
                                },
                                label = { Text(if (localMultipart) "Parte de Vídeo ${index + 1}" else "Input (Texto Original)") },
                                placeholder = { Text("Cole timestamps e anotações aqui...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .padding(bottom = 6.dp),
                                maxLines = 10
                            )

                            if (localMultipart && updatedTexts.size > 1) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            val newList = updatedTexts.toMutableList()
                                            newList.removeAt(index)
                                            localTextInputs = newList
                                        }
                                    ) {
                                        Text("Remover Parte", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        if (localMultipart) {
                            TextButton(
                                onClick = {
                                    val newList = updatedTexts.toMutableList()
                                    newList.add("")
                                    localTextInputs = newList
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mais uma parte", fontSize = 12.sp)
                            }
                        }
                    }

                    // --- ACTION BUTTONS & OUTPUT ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                // Consolidate the absolute latest values from local memory-states into active aula object
                                val finalAula = aula.copy(
                                    title = localTitle,
                                    inlineInputsJson = JsonUtils.serializeInlineRows(localInlineRows),
                                    textInputsJson = JsonUtils.serializeStringList(localTextInputs),
                                    inlineMode = localInlineMode,
                                    multipart = localMultipart,
                                    lessThanOneHour = localLessThanOneHour
                                )
                                // Save immediately in current ViewModel context and trigger external formatting pipeline
                                viewModel.updateAula(finalAula)
                                viewModel.formatAula(finalAula)
                            },
                            modifier = Modifier
                                .weight(1.5f)
                                .testTag("format_individual_${aula.id}"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Formatar com IA", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                // Reset outputs & inputs locally in memory first
                                if (localInlineMode) {
                                    localInlineRows = listOf(listOf(InlineRow()))
                                } else {
                                    localTextInputs = listOf("")
                                }
                                // Also write cleared states into persistent Room Database instantly
                                viewModel.updateAula(
                                    aula.copy(
                                        output = "",
                                        inlineInputsJson = JsonUtils.serializeInlineRows(listOf(listOf(InlineRow()))),
                                        textInputsJson = JsonUtils.serializeStringList(listOf(""))
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Limpar", fontSize = 13.sp)
                        }
                    }

                    // OUTPUT AREA
                    Text(
                        "Resultado Formatado (Compatível Premiere)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        if (aula.output.isNotBlank()) {
                            Column {
                                SelectionContainer {
                                    Text(
                                        text = aula.output,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    // Quick Copy
                                    TextButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(aula.output))
                                            Toast.makeText(context, "Copiado para o Clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text("Copiar Resultado", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Text(
                                "O resultado formatado para Markerbox aparecerá aqui...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DropdownSuggestions(
    suggestions: List<String>,
    onSelected: (String) -> Unit,
    onSaveAsFreq: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(
                "Favoritos / Frequentes",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            suggestions.forEach { comment ->
                Text(
                    text = comment,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelected(comment) }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Text(
                "Salvar Nota escrita",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSaveAsFreq() }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
            Text(
                "Fechar",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDismiss() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

// Dialog for App settings
@Composable
fun SettingsDialog(
    viewModel: MarkerboxViewModel,
    onDismiss: () -> Unit
) {
    var apiKeyText by remember { mutableStateOf(viewModel.customApiKey) }
    var newCommentText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "Opções & Configurações",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Theme selection
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tema Escuro Cosmic", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Alterna entre tema claro e escuro", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Switch(
                            checked = viewModel.isDarkTheme,
                            onCheckedChange = { viewModel.toggleTheme() }
                        )
                    }
                }

                // API config override
                item {
                    Column {
                        Text("Custom Gemini API Key", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Deixe em branco para usar a chave padrão do sistema.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = apiKeyText,
                            onValueChange = {
                                apiKeyText = it
                                viewModel.updateCustomApiKey(it)
                            },
                            placeholder = { Text("AIzaSy...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                // Autocomplete Comment Suggestions Editor
                item {
                    Column {
                        Text("Notas Frequentes (Favoritas)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Gerencie notas para preenchimento rápido em uma linha.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newCommentText,
                                onValueChange = { newCommentText = it },
                                placeholder = { Text("Nova nota...") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Button(
                                onClick = {
                                    if (newCommentText.isNotBlank()) {
                                        viewModel.addFrequentComment(newCommentText)
                                        newCommentText = ""
                                    }
                                }
                            ) {
                                Text("Acre.")
                            }
                        }
                    }
                }

                // Render current comments
                items(viewModel.frequentComments) { comment ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(comment, modifier = Modifier.weight(1f), fontSize = 13.sp)
                        IconButton(
                            onClick = { viewModel.removeFrequentComment(comment) },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                item {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Salvar e Fechar")
                    }
                }
            }
        }
    }
}

// Dialog for quick instructions
@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillModifier()
                    .padding(18.dp)
            ) {
                Text(
                    "Instruções Markerbox",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    "Esta utilidade foi desenhada para agilizar decupagens de vídeos de revisores para o plugin Markerbox do Adobe Premiere.\n\n" +
                            "💡 Coisas Importantes:\n" +
                            "• O Markerbox espera tabulações no formato: Entrada [Tab] Saída [Tab] Comentários.\n" +
                            "• No 'Modo em Linha', digite a minutagem aproximada nos campos Entrada/Saída (MM:SS ou HH:MM:SS) e a IA ajustará as pontes e formatará com tabulações corretas.\n" +
                            "• 'Vídeo em várias partes' calcula e compensa os acumuladores de tempo para vídeos longos divididos em arquivos menores automaticamente.\n" +
                            "• Use o ícone de seta nas caixas de notas na tabela para preenchimento ágil baseado nos seus favoritos.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
                )

                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Entendido")
                }
            }
        }
    }
}

fun Modifier.fillModifier(): Modifier = this.fillMaxWidth()

// Clean Selection container support if not on Android framework
@Composable
fun SelectionContainer(content: @Composable () -> Unit) {
    androidx.compose.foundation.text.selection.SelectionContainer {
        content()
    }
}

// Fix Arrangement.spacedGrid missing in core
fun Arrangement.spacedGrid(size: androidx.compose.ui.unit.Dp): Arrangement.Vertical {
    return Arrangement.spacedBy(size)
}
