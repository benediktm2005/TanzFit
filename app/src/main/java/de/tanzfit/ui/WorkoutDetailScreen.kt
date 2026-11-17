package de.tanzfit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import de.tanzfit.Nav
import de.tanzfit.Screen
import de.tanzfit.data.HistoryEntry
import de.tanzfit.data.Motivation
import de.tanzfit.data.Repository
import de.tanzfit.data.estimateSeconds
import de.tanzfit.data.formatAmount

/**
 * Übersicht eines Workouts. Man kann geführt starten (Timer, Videos, Pausen)
 * oder die Übungen frei im eigenen Tempo abhaken.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(id: String, nav: Nav) {
    val data by Repository.data.collectAsState()
    val workout = data.workouts.firstOrNull { it.id == id }
    if (workout == null) {
        LaunchedEffect(Unit) { nav.pop() }
        return
    }
    val exerciseById = data.exercises.associateBy { it.id }

    val checked = remember(workout.items.size) {
        mutableStateListOf<Boolean>().apply { repeat(workout.items.size) { add(false) } }
    }
    var startedAt by remember { mutableLongStateOf(0L) }
    var popup by remember { mutableStateOf<String?>(null) }
    var finishedPopup by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val doneCount = checked.count { it }

    fun toggle(index: Int) {
        if (startedAt == 0L) startedAt = System.currentTimeMillis()
        val nowChecked = !checked[index]
        checked[index] = nowChecked
        if (nowChecked) {
            if (checked.all { it }) {
                Repository.addHistory(
                    HistoryEntry(
                        id = Repository.newId(),
                        workoutName = workout.name,
                        timestamp = System.currentTimeMillis(),
                        done = checked.size,
                        total = checked.size,
                        durationSeconds = ((System.currentTimeMillis() - startedAt) / 1000).toInt(),
                    )
                )
                finishedPopup = Motivation.finished() + "\n\nIm Verlauf gespeichert."
            } else {
                popup = Motivation.afterExercise()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workout.name) },
                navigationIcon = {
                    IconButton(onClick = { nav.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") }
                },
                actions = {
                    IconButton(onClick = { nav.push(Screen.EditWorkout(workout.id)) }) { Icon(Icons.Default.Edit, "Bearbeiten") }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "Löschen") }
                },
            )
        },
        bottomBar = {
            Button(
                onClick = { nav.push(Screen.Player(workout.id)) },
                enabled = workout.items.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("START", fontWeight = FontWeight.Bold)
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                val min = (estimateSeconds(workout) + 59) / 60
                Text(
                    "${workout.items.size} Übungen · ca. $min Min · ${workout.restSeconds} s Pause",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                if (workout.items.isEmpty()) {
                    Text("Noch keine Übungen – tippe oben auf ✏️, um welche hinzuzufügen.")
                } else {
                    Text("Abgehakt: $doneCount / ${workout.items.size}", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { doneCount.toFloat() / workout.items.size },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tipp: Mit START wirst du mit Timer & Video durchs Training geführt. " +
                            "Oder hake die Übungen hier frei ab.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            itemsIndexed(workout.items) { index, item ->
                val ex = exerciseById[item.exerciseId]
                val isDone = checked.getOrElse(index) { false }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { toggle(index) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDone) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ExerciseThumb(ex?.videoFile)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                ex?.name ?: "(gelöscht)",
                                style = MaterialTheme.typography.titleMedium,
                                textDecoration = if (isDone) TextDecoration.LineThrough else null,
                            )
                            Text(
                                formatAmount(item.mode, item.amount),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Checkbox(checked = isDone, onCheckedChange = { toggle(index) }, modifier = Modifier.size(40.dp))
                    }
                }
            }
        }
    }

    popup?.let { MotivationPopup(it, onDismiss = { popup = null }) }

    finishedPopup?.let { text ->
        MotivationPopup(text, autoCloseMs = 0) {
            finishedPopup = null
            for (i in checked.indices) checked[i] = false
            startedAt = 0L
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Workout löschen?") },
            text = { Text("Die Übungen selbst bleiben erhalten.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    Repository.deleteWorkout(workout.id)
                    nav.pop()
                }) { Text("Löschen") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Abbrechen") } },
        )
    }
}
