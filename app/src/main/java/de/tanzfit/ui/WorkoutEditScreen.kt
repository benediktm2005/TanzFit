package de.tanzfit.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.tanzfit.Nav
import de.tanzfit.Screen
import de.tanzfit.data.Mode
import de.tanzfit.data.Repository
import de.tanzfit.data.Workout
import de.tanzfit.data.WorkoutItem

/**
 * Entwurf eines Workouts. Lebt im Navigations-Stapel, damit nichts verloren geht,
 * wenn man zwischendurch eine neue Übung anlegt.
 */
class WorkoutDraft(id: String?) {
    val original: Workout? = Repository.workout(id)
    val name = mutableStateOf(original?.name ?: "")
    val restText = mutableStateOf((original?.restSeconds ?: 20).toString())
    val items = mutableStateListOf<WorkoutItem>().apply { addAll(original?.items ?: emptyList()) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutEditScreen(draft: WorkoutDraft, nav: Nav) {
    val context = LocalContext.current
    val data by Repository.data.collectAsState()
    val original = draft.original
    var name by draft.name
    var restText by draft.restText
    val items = draft.items
    var showPicker by remember { mutableStateOf(false) }

    val exerciseById = data.exercises.associateBy { it.id }

    fun save() {
        if (name.isBlank()) {
            Toast.makeText(context, "Bitte gib dem Workout einen Namen", Toast.LENGTH_SHORT).show(); return
        }
        val cleaned = items.filter { it.amount > 0 && exerciseById.containsKey(it.exerciseId) }
        val w = Workout(
            id = original?.id ?: Repository.newId(),
            name = name.trim(),
            items = cleaned,
            restSeconds = restText.toIntOrNull() ?: 0,
        )
        Repository.saveWorkout(w)
        if (original == null) nav.replace(Screen.WorkoutDetail(w.id)) else nav.pop()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (original == null) "Neues Workout" else "Workout bearbeiten") },
                navigationIcon = {
                    IconButton(onClick = { nav.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") }
                },
                actions = {
                    IconButton(onClick = { save() }) { Icon(Icons.Default.Check, "Speichern") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name des Workouts") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = restText,
                    onValueChange = { restText = it.filter(Char::isDigit).take(3) },
                    label = { Text("Pause zwischen Übungen (Sekunden)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text(
                    "Übungen (${items.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            itemsIndexed(items) { index, item ->
                val ex = exerciseById[item.exerciseId]
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ExerciseThumb(ex?.videoFile, size = 44.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "${index + 1}. ${ex?.name ?: "(gelöscht)"}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = {
                                if (index > 0) { items.add(index - 1, items.removeAt(index)) }
                            }, enabled = index > 0) { Icon(Icons.Default.KeyboardArrowUp, "Nach oben") }
                            IconButton(onClick = {
                                if (index < items.lastIndex) { items.add(index + 1, items.removeAt(index)) }
                            }, enabled = index < items.lastIndex) { Icon(Icons.Default.KeyboardArrowDown, "Nach unten") }
                            IconButton(onClick = { items.removeAt(index) }) { Icon(Icons.Default.Close, "Entfernen") }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = item.mode == Mode.REPS,
                                onClick = { items[index] = item.copy(mode = Mode.REPS) },
                                label = { Text("Wdh.") },
                            )
                            FilterChip(
                                selected = item.mode == Mode.TIME,
                                onClick = { items[index] = item.copy(mode = Mode.TIME) },
                                label = { Text("Zeit") },
                            )
                            OutlinedTextField(
                                value = if (item.amount == 0) "" else item.amount.toString(),
                                onValueChange = { t ->
                                    val v = t.filter(Char::isDigit).take(4).toIntOrNull() ?: 0
                                    items[index] = item.copy(amount = v)
                                },
                                singleLine = true,
                                suffix = { Text(if (item.mode == Mode.TIME) "s" else "x") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(110.dp),
                            )
                        }
                    }
                }
            }
            item {
                OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Übungen hinzufügen")
                }
            }
        }
    }

    if (showPicker) {
        ExercisePickerDialog(
            onDismiss = { showPicker = false },
            onAdd = { ids ->
                ids.forEach { exId ->
                    exerciseById[exId]?.let { items.add(WorkoutItem(it.id, it.mode, it.amount)) }
                }
                showPicker = false
            },
            onCreateNew = {
                showPicker = false
                nav.push(Screen.EditExercise(null))
            },
        )
    }
}

/** Mehrfachauswahl: mehrere Übungen auf einmal ins Workout übernehmen. */
@Composable
private fun ExercisePickerDialog(
    onDismiss: () -> Unit,
    onAdd: (List<String>) -> Unit,
    onCreateNew: () -> Unit,
) {
    val data by Repository.data.collectAsState()
    val selected = remember { mutableStateListOf<String>() }
    val list = data.exercises.sortedBy { it.name.lowercase() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Übungen auswählen") },
        text = {
            if (list.isEmpty()) {
                Text("Du hast noch keine Übungen angelegt.")
            } else {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(list, key = { it.id }) { e ->
                        val checked = e.id in selected
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { if (checked) selected.remove(e.id) else selected.add(e.id) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            ExerciseThumb(e.videoFile, size = 40.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(e.name, Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(selected.toList()) }, enabled = selected.isNotEmpty()) {
                Text("Hinzufügen (${selected.size})")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onCreateNew) { Text("Neue Übung") }
                TextButton(onClick = onDismiss) { Text("Abbrechen") }
            }
        },
    )
}
