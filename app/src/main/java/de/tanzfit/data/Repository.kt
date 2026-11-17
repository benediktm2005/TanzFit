package de.tanzfit.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.UUID

/**
 * Speichert alles lokal in einer JSON-Datei. Videos werden in den
 * privaten App-Ordner kopiert, damit sie auch dann noch funktionieren,
 * wenn du die Bildschirmaufnahme aus der Galerie löschst.
 */
object Repository {
    private lateinit var dataFile: File
    private lateinit var videoDir: File
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private val _data = MutableStateFlow(AppData())
    val data: StateFlow<AppData> = _data

    fun init(context: Context) {
        if (::dataFile.isInitialized) return
        dataFile = File(context.filesDir, "data.json")
        videoDir = File(context.filesDir, "videos").apply { mkdirs() }
        _data.value = load() ?: seed()
        save()
    }

    fun newId(): String = UUID.randomUUID().toString()

    fun videoPath(fileName: String): String = File(videoDir, fileName).absolutePath

    // ---------- Übungen ----------

    fun exercise(id: String?): Exercise? = _data.value.exercises.firstOrNull { it.id == id }

    fun saveExercise(exercise: Exercise) = update { d ->
        val exists = d.exercises.any { it.id == exercise.id }
        d.copy(
            exercises = if (exists) d.exercises.map { if (it.id == exercise.id) exercise else it }
            else d.exercises + exercise
        )
    }

    fun deleteExercise(id: String) {
        exercise(id)?.videoFile?.let { deleteVideo(it) }
        update { d ->
            d.copy(
                exercises = d.exercises.filterNot { it.id == id },
                workouts = d.workouts.map { w -> w.copy(items = w.items.filterNot { it.exerciseId == id }) }
            )
        }
    }

    // ---------- Workouts ----------

    fun workout(id: String?): Workout? = _data.value.workouts.firstOrNull { it.id == id }

    fun saveWorkout(workout: Workout) = update { d ->
        val exists = d.workouts.any { it.id == workout.id }
        d.copy(
            workouts = if (exists) d.workouts.map { if (it.id == workout.id) workout else it }
            else d.workouts + workout
        )
    }

    fun deleteWorkout(id: String) = update { d -> d.copy(workouts = d.workouts.filterNot { it.id == id }) }

    // ---------- Verlauf ----------

    fun addHistory(entry: HistoryEntry) = update { d -> d.copy(history = listOf(entry) + d.history) }

    fun deleteHistory(id: String) = update { d -> d.copy(history = d.history.filterNot { it.id == id }) }

    // ---------- Videos ----------

    /** Kopiert ein gewähltes Video in den App-Ordner. Im Hintergrund-Thread aufrufen. */
    fun importVideo(context: Context, uri: Uri): String {
        val name = "${newId()}.mp4"
        val target = File(videoDir, name)
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Video konnte nicht geöffnet werden" }
            target.outputStream().use { output -> input.copyTo(output) }
        }
        return name
    }

    fun deleteVideo(fileName: String) {
        File(videoDir, fileName).delete()
    }

    // ---------- intern ----------

    private fun update(transform: (AppData) -> AppData) {
        _data.value = transform(_data.value)
        save()
    }

    @Suppress("SENSELESS_COMPARISON", "USELESS_ELVIS")
    private fun load(): AppData? {
        if (!dataFile.exists()) return null
        return try {
            val raw = gson.fromJson(dataFile.readText(), AppData::class.java) ?: return null
            // Gson kann bei fehlenden Feldern null setzen – hier absichern.
            AppData(
                exercises = (raw.exercises ?: emptyList()).filter { it.id != null && it.name != null }
                    .map { it.copy(description = it.description ?: "", mode = it.mode ?: Mode.REPS) },
                workouts = (raw.workouts ?: emptyList()).filter { it.id != null }
                    .map { it.copy(name = it.name ?: "Workout", items = it.items ?: emptyList()) },
                history = raw.history ?: emptyList(),
            )
        } catch (e: Exception) {
            // Kaputte Datei nicht überschreiben, sondern sichern.
            dataFile.copyTo(File(dataFile.parentFile, "data-defekt-${System.currentTimeMillis()}.json"), overwrite = true)
            null
        }
    }

    private fun save() {
        val tmp = File(dataFile.parentFile, "data.json.tmp")
        tmp.writeText(gson.toJson(_data.value))
        if (!tmp.renameTo(dataFile)) {
            dataFile.writeText(tmp.readText())
            tmp.delete()
        }
    }

    private fun seed(): AppData {
        val push = Exercise(newId(), "Liegestütze", "Körper gerade halten, Brust bis kurz über den Boden.", null, Mode.REPS, 10)
        val squat = Exercise(newId(), "Kniebeugen", "Füße schulterbreit, Knie über den Zehen.", null, Mode.REPS, 15)
        val jacks = Exercise(newId(), "Hampelmänner", "Locker aufwärmen.", null, Mode.TIME, 30)
        val plank = Exercise(newId(), "Unterarmstütz", "Bauch fest, Po nicht hochstrecken.", null, Mode.TIME, 30)
        val all = listOf(jacks, squat, push, plank)
        val warmup = Workout(
            id = newId(),
            name = "Aufwärmen",
            items = all.map { WorkoutItem(it.id, it.mode, it.amount) },
            restSeconds = 15,
        )
        return AppData(exercises = all, workouts = listOf(warmup))
    }
}
