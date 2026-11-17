package de.tanzfit.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.tanzfit.Nav
import de.tanzfit.data.Exercise
import de.tanzfit.data.Mode
import de.tanzfit.data.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseEditScreen(id: String?, nav: Nav) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val original = remember(id) { Repository.exercise(id) }

    var name by remember { mutableStateOf(original?.name ?: "") }
    var description by remember { mutableStateOf(original?.description ?: "") }
    var videoFile by remember { mutableStateOf(original?.videoFile) }
    var mode by remember { mutableStateOf(original?.mode ?: Mode.REPS) }
    var amountText by remember { mutableStateOf((original?.amount ?: 10).toString()) }
    var importing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    // Videos, die in dieser Sitzung neu importiert wurden (zum Aufräumen beim Abbrechen).
    val newlyImported = remember { mutableListOf<String>() }

    fun cancel() {
        newlyImported.forEach { Repository.deleteVideo(it) }
        nav.pop()
    }

    fun save() {
        val amount = amountText.toIntOrNull() ?: 0
        if (name.isBlank()) {
            Toast.makeText(context, "Bitte gib einen Namen ein", Toast.LENGTH_SHORT).show(); return
        }
        if (amount <= 0) {
            Toast.makeText(context, "Bitte gib eine Zeit bzw. Anzahl > 0 ein", Toast.LENGTH_SHORT).show(); return
        }
        // Nicht mehr benutzte Videos löschen
        (newlyImported + listOfNotNull(original?.videoFile))
            .filter { it != videoFile }
            .forEach { Repository.deleteVideo(it) }
        Repository.saveExercise(
            Exercise(
                id = original?.id ?: Repository.newId(),
                name = name.trim(),
                description = description.trim(),
                videoFile = videoFile,
                mode = mode,
                amount = amount,
            )
        )
        nav.pop()
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        importing = true
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) { Repository.importVideo(context, uri) }
                newlyImported += file
                videoFile = file
                if (name.isBlank()) name = "Neue Übung"
            } catch (e: Exception) {
                Toast.makeText(context, "Video konnte nicht importiert werden", Toast.LENGTH_LONG).show()
            } finally {
                importing = false
            }
        }
    }

    BackHandler { cancel() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (original == null) "Neue Übung" else "Übung bearbeiten") },
                navigationIcon = {
                    IconButton(onClick = { cancel() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") }
                },
                actions = {
                    if (original != null) {
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "Löschen") }
                    }
                    IconButton(onClick = { save() }, enabled = !importing) { Icon(Icons.Default.Check, "Speichern") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ---- Video ----
            val vf = videoFile
            when {
                importing -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Video wird importiert …")
                    }
                }
                vf != null -> LoopingVideo(vf, Modifier.fillMaxWidth().height(320.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }, enabled = !importing) {
                    Icon(Icons.Default.VideoLibrary, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (videoFile == null) "Video hinzufügen" else "Video ersetzen")
                }
                if (videoFile != null && !importing) {
                    OutlinedButton(onClick = { videoFile = null }) { Text("Video entfernen") }
                }
            }
            if (videoFile == null && !importing) {
                Text(
                    "Optional: Wähle eine Bildschirmaufnahme (z. B. von Instagram) aus deiner Galerie. " +
                        "Ohne Video funktioniert die Übung genauso.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ---- Daten ----
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Name") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text("Beschreibung / Tipps (optional)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Standard-Vorgabe", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(selected = mode == Mode.REPS, onClick = { mode = Mode.REPS }, label = { Text("Wiederholungen") })
                FilterChip(selected = mode == Mode.TIME, onClick = { mode = Mode.TIME }, label = { Text("Zeit") })
            }
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter(Char::isDigit).take(4) },
                label = { Text(if (mode == Mode.TIME) "Sekunden" else "Wiederholungen") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(200.dp),
            )
            Text(
                "Im Workout kannst du die Vorgabe pro Übung noch anpassen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = { save() }, enabled = !importing, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Speichern")
            }
        }
    }

    if (confirmDelete && original != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Übung löschen?") },
            text = { Text("„${original.name}“ wird auch aus allen Workouts entfernt.") },
            confirmButton = {
                TextButton(onClick = {
                    newlyImported.forEach { Repository.deleteVideo(it) }
                    Repository.deleteExercise(original.id)
                    nav.pop()
                }) { Text("Löschen") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Abbrechen") } },
        )
    }
}
