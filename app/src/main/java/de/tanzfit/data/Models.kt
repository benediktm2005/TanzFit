package de.tanzfit.data

/** Wie eine Übung gezählt wird: nach Zeit (Sekunden) oder nach Wiederholungen. */
enum class Mode { TIME, REPS }

data class Exercise(
    val id: String,
    val name: String,
    val description: String = "",
    /** Dateiname im App-Ordner "videos" oder null, wenn es kein Video gibt. */
    val videoFile: String? = null,
    val mode: Mode = Mode.REPS,
    val amount: Int = 10,
)

data class WorkoutItem(
    val exerciseId: String,
    val mode: Mode,
    val amount: Int,
)

data class Workout(
    val id: String,
    val name: String,
    val items: List<WorkoutItem> = emptyList(),
    val restSeconds: Int = 20,
)

data class HistoryEntry(
    val id: String,
    val workoutName: String,
    val timestamp: Long,
    val done: Int,
    val total: Int,
    val durationSeconds: Int,
)

data class AppData(
    val exercises: List<Exercise> = emptyList(),
    val workouts: List<Workout> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
)

fun formatAmount(mode: Mode, amount: Int): String = when (mode) {
    Mode.TIME -> formatSeconds(amount)
    Mode.REPS -> "x $amount"
}

fun formatSeconds(seconds: Int): String =
    if (seconds >= 60) "%d:%02d".format(seconds / 60, seconds % 60) else "$seconds s"

/** Grobe Dauer eines Workouts in Sekunden (Wiederholung ≈ 3 s). */
fun estimateSeconds(workout: Workout): Int {
    if (workout.items.isEmpty()) return 0
    val work = workout.items.sumOf { if (it.mode == Mode.TIME) it.amount else it.amount * 3 }
    return work + workout.restSeconds * (workout.items.size - 1)
}
