package de.tanzfit.ui

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.tanzfit.Nav
import de.tanzfit.data.Exercise
import de.tanzfit.data.HistoryEntry
import de.tanzfit.data.Mode
import de.tanzfit.data.Motivation
import de.tanzfit.data.Repository
import de.tanzfit.data.WorkoutItem
import de.tanzfit.data.formatAmount
import kotlinx.coroutines.delay

private enum class Phase { READY, EXERCISE, REST, DONE }

private data class Step(val exercise: Exercise, val item: WorkoutItem)

private const val READY_SECONDS = 10

private fun clock(seconds: Int) = "%d:%02d".format(seconds / 60, seconds % 60)

/** Geführtes Training: Vorbereitung → Übung → Pause → … → Geschafft. */
@Composable
fun PlayerScreen(id: String, nav: Nav) {
    // Einmal beim Start festhalten, damit Änderungen das laufende Training nicht stören.
    val workout = remember { Repository.workout(id) }
    val steps = remember {
        workout?.items?.mapNotNull { item ->
            Repository.exercise(item.exerciseId)?.let { Step(it, item) }
        } ?: emptyList()
    }
    if (workout == null || steps.isEmpty()) {
        LaunchedEffect(Unit) { nav.pop() }
        return
    }
    val restSeconds = workout.restSeconds

    var phase by remember { mutableStateOf(Phase.READY) }
    var index by remember { mutableIntStateOf(0) }
    var secondsLeft by remember { mutableIntStateOf(READY_SECONDS) }
    var phaseTotal by remember { mutableIntStateOf(READY_SECONDS) }
    var paused by remember { mutableStateOf(false) }
    var popup by remember { mutableStateOf<String?>(null) }
    var banner by remember { mutableStateOf<String?>(null) }
    var confirmQuit by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    val done = remember { mutableStateListOf<Boolean>().apply { repeat(steps.size) { add(false) } } }
    val startedAt = remember { System.currentTimeMillis() }

    // Bildschirm während des Trainings anlassen
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // Signaltöne
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 80) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { tone?.release() } }
    fun beep(long: Boolean = false) {
        runCatching {
            tone?.startTone(
                if (long) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_BEEP,
                if (long) 350 else 150,
            )
        }
    }

    fun saveHistory() {
        if (saved) return
        saved = true
        Repository.addHistory(
            HistoryEntry(
                id = Repository.newId(),
                workoutName = workout.name,
                timestamp = System.currentTimeMillis(),
                done = done.count { it },
                total = steps.size,
                durationSeconds = ((System.currentTimeMillis() - startedAt) / 1000).toInt(),
            )
        )
    }

    fun startExercise(i: Int) {
        index = i
        phase = Phase.EXERCISE
        paused = false
        val item = steps[i].item
        if (item.mode == Mode.TIME) {
            secondsLeft = item.amount
            phaseTotal = item.amount
        }
        beep(long = true)
    }

    fun finish() {
        phase = Phase.DONE
        saveHistory()
    }

    fun completeCurrent(success: Boolean) {
        done[index] = success
        if (success) popup = Motivation.afterExercise()
        when {
            index >= steps.lastIndex -> finish()
            restSeconds > 0 -> {
                phase = Phase.REST
                secondsLeft = restSeconds
                phaseTotal = restSeconds
                paused = false
            }
            else -> startExercise(index + 1)
        }
    }

    // ---------- Timer ----------
    val timed = phase == Phase.READY || phase == Phase.REST ||
        (phase == Phase.EXERCISE && steps[index].item.mode == Mode.TIME)
    val running = timed && !paused && popup == null && !confirmQuit

    LaunchedEffect(running, phase, index) {
        if (!running) return@LaunchedEffect
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
            if (secondsLeft in 1..3) beep()
            if (phase == Phase.EXERCISE && phaseTotal >= 20 && secondsLeft == phaseTotal / 2) {
                banner = Motivation.halfway()
            }
        }
        when (phase) {
            Phase.READY -> startExercise(0)
            Phase.EXERCISE -> completeCurrent(true)
            Phase.REST -> startExercise(index + 1)
            Phase.DONE -> Unit
        }
    }

    LaunchedEffect(banner) {
        if (banner != null) {
            delay(2200)
            banner = null
        }
    }

    BackHandler(enabled = phase != Phase.DONE) { confirmQuit = true }

    // ---------- Oberfläche ----------
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            if (phase != Phase.DONE) {
                TopBar(
                    current = if (phase == Phase.REST) index + 1 else index,
                    total = steps.size,
                    done = done,
                    onClose = { confirmQuit = true },
                )
                Spacer(Modifier.height(12.dp))
            }
            when (phase) {
                Phase.READY -> ReadyView(
                    step = steps[0],
                    secondsLeft = secondsLeft,
                    total = phaseTotal,
                    onGo = { startExercise(0) },
                )
                Phase.EXERCISE -> ExerciseView(
                    step = steps[index],
                    secondsLeft = secondsLeft,
                    total = phaseTotal,
                    paused = paused,
                    canGoBack = index > 0,
                    onTogglePause = { paused = !paused },
                    onDone = { completeCurrent(true) },
                    onSkip = { completeCurrent(false) },
                    onPrevious = { if (index > 0) startExercise(index - 1) },
                )
                Phase.REST -> RestView(
                    next = steps[index + 1],
                    nextNumber = index + 2,
                    total = steps.size,
                    secondsLeft = secondsLeft,
                    totalSeconds = phaseTotal,
                    onAdd = { secondsLeft += 20; phaseTotal += 20 },
                    onSkip = { startExercise(index + 1) },
                )
                Phase.DONE -> DoneView(
                    steps = steps,
                    done = done,
                    durationSeconds = ((System.currentTimeMillis() - startedAt) / 1000).toInt(),
                    onClose = { nav.pop() },
                )
            }
        }

        // Kleines Motivations-Banner (z. B. "Halbzeit!")
        AnimatedVisibility(
            visible = banner != null,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 72.dp),
        ) {
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondary) {
                Text(
                    banner ?: "",
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }
    }

    popup?.let { MotivationPopup(it, onDismiss = { popup = null }) }

    if (confirmQuit) {
        AlertDialog(
            onDismissRequest = { confirmQuit = false },
            title = { Text("Training beenden?") },
            text = { Text("Bisher erledigte Übungen werden im Verlauf gespeichert. Gib nicht auf – du bist schon so weit gekommen! 💪") },
            confirmButton = {
                TextButton(onClick = {
                    confirmQuit = false
                    if (done.any { it }) saveHistory()
                    nav.pop()
                }) { Text("Beenden") }
            },
            dismissButton = {
                Button(onClick = { confirmQuit = false }) { Text("Weitermachen") }
            },
        )
    }
}

@Composable
private fun TopBar(current: Int, total: Int, done: List<Boolean>, onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Beenden") }
        Text(
            "Übung ${(current + 1).coerceAtMost(total)} / $total",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 0 until total) {
            val color = when {
                done.getOrElse(i) { false } -> MaterialTheme.colorScheme.tertiary
                i < current -> MaterialTheme.colorScheme.onSurfaceVariant
                i == current -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceContainerHighest
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun ColumnScope.Media(step: Step, playing: Boolean) {
    val modifier = Modifier.weight(1f)
    val vf = step.exercise.videoFile
    if (vf != null) {
        LoopingVideo(vf, modifier.fillMaxWidth(), playing = playing)
    } else {
        Box(
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.FitnessCenter, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(step.exercise.name, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun CountdownCircle(secondsLeft: Int, total: Int, size: Int = 150) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size.dp)) {
        CircularProgressIndicator(
            progress = { if (total > 0) secondsLeft.toFloat() / total else 0f },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 10.dp,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
        Text(clock(secondsLeft), fontSize = 40.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ColumnScope.ReadyView(step: Step, secondsLeft: Int, total: Int, onGo: () -> Unit) {
    Text(
        "Mach dich bereit!",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.align(Alignment.CenterHorizontally),
    )
    Spacer(Modifier.height(12.dp))
    Media(step, playing = true)
    Spacer(Modifier.height(12.dp))
    Text(
        "Als Erstes: ${step.exercise.name}  ${formatAmount(step.item.mode, step.item.amount)}",
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CountdownCircle(secondsLeft, total, size = 120)
    }
    Spacer(Modifier.height(16.dp))
    Button(onClick = onGo, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Text("LOS GEHT'S", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ColumnScope.ExerciseView(
    step: Step,
    secondsLeft: Int,
    total: Int,
    paused: Boolean,
    canGoBack: Boolean,
    onTogglePause: () -> Unit,
    onDone: () -> Unit,
    onSkip: () -> Unit,
    onPrevious: () -> Unit,
) {
    Media(step, playing = !paused)
    Spacer(Modifier.height(12.dp))
    Text(
        step.exercise.name,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    if (step.exercise.description.isNotBlank()) {
        Text(
            step.exercise.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Spacer(Modifier.height(12.dp))

    if (step.item.mode == Mode.TIME) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CountdownCircle(secondsLeft, total)
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious, enabled = canGoBack) { Icon(Icons.Default.SkipPrevious, "Zurück") }
            FilledIconButton(
                onClick = onTogglePause,
                modifier = Modifier.size(72.dp),
                colors = IconButtonDefaults.filledIconButtonColors(),
            ) {
                Icon(
                    if (paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (paused) "Weiter" else "Pause",
                    modifier = Modifier.size(40.dp),
                )
            }
            IconButton(onClick = onSkip) { Icon(Icons.Default.SkipNext, "Überspringen") }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onDone, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Schon fertig ✓")
        }
    } else {
        Text(
            "x ${step.item.amount}",
            fontSize = 64.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious, enabled = canGoBack) { Icon(Icons.Default.SkipPrevious, "Zurück") }
            Button(onClick = onDone, modifier = Modifier.weight(1f).height(64.dp)) {
                Icon(Icons.Default.Check, null, Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text("ERLEDIGT", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            IconButton(onClick = onSkip) { Icon(Icons.Default.SkipNext, "Überspringen") }
        }
    }
}

@Composable
private fun ColumnScope.RestView(
    next: Step,
    nextNumber: Int,
    total: Int,
    secondsLeft: Int,
    totalSeconds: Int,
    onAdd: () -> Unit,
    onSkip: () -> Unit,
) {
    Text(
        "Pause",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.align(Alignment.CenterHorizontally),
    )
    Spacer(Modifier.height(12.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CountdownCircle(secondsLeft, totalSeconds, size = 140)
    }
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilledTonalButton(onClick = onAdd, modifier = Modifier.weight(1f).height(52.dp)) { Text("+20 s") }
        Button(onClick = onSkip, modifier = Modifier.weight(1f).height(52.dp)) { Text("Überspringen") }
    }
    Spacer(Modifier.height(20.dp))
    Text(
        "Als Nächstes ($nextNumber/$total)",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        "${next.exercise.name}  ${formatAmount(next.item.mode, next.item.amount)}",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(8.dp))
    Media(next, playing = true)
}

@Composable
private fun ColumnScope.DoneView(
    steps: List<Step>,
    done: List<Boolean>,
    durationSeconds: Int,
    onClose: () -> Unit,
) {
    val doneCount = done.count { it }
    Spacer(Modifier.height(24.dp))
    Text("🏆", fontSize = 72.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    Text(
        if (doneCount == steps.size) "Geschafft!" else "Gut gemacht!",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.align(Alignment.CenterHorizontally),
    )
    Text(
        "$doneCount von ${steps.size} Übungen · ${clock(durationSeconds)} Min",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.align(Alignment.CenterHorizontally),
    )
    Spacer(Modifier.height(16.dp))
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        itemsIndexed(steps) { i, s ->
            val ok = done.getOrElse(i) { false }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (ok) Icons.Default.Check else Icons.Default.Close, null,
                    tint = if (ok) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(12.dp))
                Text(s.exercise.name, Modifier.weight(1f))
                Text(formatAmount(s.item.mode, s.item.amount), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Text("FERTIG", fontWeight = FontWeight.Bold)
    }
}
