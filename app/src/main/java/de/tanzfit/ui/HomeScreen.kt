package de.tanzfit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.tanzfit.Nav
import de.tanzfit.Screen
import de.tanzfit.data.AppData
import de.tanzfit.data.Repository
import de.tanzfit.data.estimateSeconds
import de.tanzfit.data.formatAmount
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: Nav, tab: Int, onTab: (Int) -> Unit) {
    val data by Repository.data.collectAsState()
    val titles = listOf("Workouts", "Übungen", "Verlauf")

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(
                    if (tab == 0) "TanzFit 💃" else titles[tab],
                    fontWeight = FontWeight.Bold,
                )
            })
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0, onClick = { onTab(0) },
                    icon = { Icon(Icons.Default.ViewList, null) }, label = { Text("Workouts") },
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { onTab(1) },
                    icon = { Icon(Icons.Default.FitnessCenter, null) }, label = { Text("Übungen") },
                )
                NavigationBarItem(
                    selected = tab == 2, onClick = { onTab(2) },
                    icon = { Icon(Icons.Default.History, null) }, label = { Text("Verlauf") },
                )
            }
        },
        floatingActionButton = {
            when (tab) {
                0 -> ExtendedFloatingActionButton(
                    onClick = { nav.push(Screen.EditWorkout(null)) },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Workout") },
                )
                1 -> ExtendedFloatingActionButton(
                    onClick = { nav.push(Screen.EditExercise(null)) },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Übung") },
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> WorkoutsTab(data, nav)
                1 -> ExercisesTab(data, nav)
                else -> HistoryTab(data)
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WorkoutsTab(data: AppData, nav: Nav) {
    if (data.workouts.isEmpty()) {
        EmptyHint("Noch kein Workout.\nTippe auf „+ Workout“ und stell dir deine Übungen zusammen.")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(data.workouts, key = { it.id }) { w ->
            val firstVideo = w.items.firstNotNullOfOrNull { item ->
                data.exercises.firstOrNull { it.id == item.exerciseId }?.videoFile
            }
            Card(
                modifier = Modifier.fillMaxWidth().clickable { nav.push(Screen.WorkoutDetail(w.id)) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    ExerciseThumb(firstVideo)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(w.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        val min = (estimateSeconds(w) + 59) / 60
                        Text(
                            "${w.items.size} Übungen · ca. $min Min",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FilledIconButton(
                        onClick = { nav.push(Screen.Player(w.id)) },
                        enabled = w.items.isNotEmpty(),
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Starten")
                    }
                }
            }
        }
    }
}

@Composable
private fun ExercisesTab(data: AppData, nav: Nav) {
    if (data.exercises.isEmpty()) {
        EmptyHint("Noch keine Übungen.\nTippe auf „+ Übung“ – mit Video aus deiner Galerie oder ganz ohne.")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(data.exercises.sortedBy { it.name.lowercase() }, key = { it.id }) { e ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { nav.push(Screen.EditExercise(e.id)) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    ExerciseThumb(e.videoFile)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.name, style = MaterialTheme.typography.titleMedium)
                        if (e.description.isNotBlank()) {
                            Text(
                                e.description, maxLines = 1,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        formatAmount(e.mode, e.amount),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryTab(data: AppData) {
    if (data.history.isEmpty()) {
        EmptyHint("Hier erscheinen deine erledigten Workouts. 🏆")
        return
    }
    val weekStart = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val thisWeek = data.history.count { it.timestamp >= weekStart }
    val totalMin = data.history.sumOf { it.durationSeconds } / 60
    val fmt = SimpleDateFormat("EEE, dd.MM.yyyy · HH:mm", Locale.GERMANY)

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Diese Woche", "$thisWeek", Modifier.weight(1f))
                StatCard("Gesamt", "${data.history.size}", Modifier.weight(1f))
                StatCard("Minuten", "$totalMin", Modifier.weight(1f))
            }
            Spacer(Modifier.padding(4.dp))
        }
        items(data.history, key = { it.id }) { h ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(h.workoutName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${fmt.format(Date(h.timestamp))} · ${h.done}/${h.total} ✓ · ${(h.durationSeconds + 30) / 60} Min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { Repository.deleteHistory(h.id) }) {
                        Icon(Icons.Default.Delete, "Löschen", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
