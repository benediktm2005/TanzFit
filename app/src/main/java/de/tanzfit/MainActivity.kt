package de.tanzfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.tanzfit.data.Motivation
import de.tanzfit.data.Repository
import de.tanzfit.ui.ExerciseEditScreen
import de.tanzfit.ui.HomeScreen
import de.tanzfit.ui.MotivationPopup
import de.tanzfit.ui.PlayerScreen
import de.tanzfit.ui.TanzFitTheme
import de.tanzfit.ui.WorkoutDetailScreen
import de.tanzfit.ui.WorkoutDraft
import de.tanzfit.ui.WorkoutEditScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repository.init(applicationContext)
        setContent {
            TanzFitTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    App()
                }
            }
        }
    }
}

sealed interface Screen {
    data object Home : Screen
    data class EditExercise(val id: String?) : Screen
    data class EditWorkout(val id: String?) : Screen {
        val draft = WorkoutDraft(id)
    }
    data class WorkoutDetail(val id: String) : Screen
    data class Player(val id: String) : Screen
}

class Nav(private val stack: MutableList<Screen>) {
    fun push(screen: Screen) { stack.add(screen) }
    fun pop() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    fun replace(screen: Screen) { stack[stack.lastIndex] = screen }
}

@Composable
fun App() {
    val stack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val nav = remember { Nav(stack) }
    var tab by remember { mutableIntStateOf(0) }
    var dailyQuote by remember { mutableStateOf<String?>(Motivation.daily()) }

    BackHandler(enabled = stack.size > 1) { nav.pop() }

    when (val screen = stack.last()) {
        Screen.Home -> HomeScreen(nav = nav, tab = tab, onTab = { tab = it })
        is Screen.EditExercise -> ExerciseEditScreen(screen.id, nav)
        is Screen.EditWorkout -> WorkoutEditScreen(screen.draft, nav)
        is Screen.WorkoutDetail -> WorkoutDetailScreen(screen.id, nav)
        is Screen.Player -> PlayerScreen(screen.id, nav)
    }

    dailyQuote?.let { quote ->
        MotivationPopup(text = quote, autoCloseMs = 3500, onDismiss = { dailyQuote = null })
    }
}
